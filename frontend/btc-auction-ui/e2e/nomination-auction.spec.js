import { expect, request as playwrightRequest, test } from "@playwright/test";

const BACKEND_URL = "http://127.0.0.1:8081";
const captains = [
    "E2E Captain One",
    "E2E Captain Two",
    "E2E Captain Three",
    "E2E Captain Four",
];
const players = [
    { name: "E2E Player Alpha", seed: "A", basePrice: 300 },
    { name: "E2E Player Bravo", seed: "B", basePrice: 250 },
];
let captainPresenceContexts = [];
let captainHeartbeatInterval = null;

async function assertSuccessful(response) {
    const body = await response.text();
    expect(response.ok(), body).toBeTruthy();
    return body;
}

async function login(browser, username, password) {
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto("/");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: /enter auction room/i }).click();
    await expect(page.getByRole("navigation", { name: "Auction navigation" })).toBeVisible();
    return { context, page };
}

async function closeContexts(...contexts) {
    await Promise.all(contexts.map(context => {
        const close = context.dispose ? context.dispose() : context.close();
        return close.catch(() => undefined);
    }));
}

async function loginCaptainRequest(captainName) {
    const context = await playwrightRequest.newContext();
    await assertSuccessful(await context.post(`${BACKEND_URL}/api/login`, {
        data: { username: captainName, password: "e2e-password" },
    }));
    return context;
}

async function checkInAllCaptains() {
    captainPresenceContexts = await Promise.all(captains.map(loginCaptainRequest));
    await Promise.all(captainPresenceContexts.map(async context => {
        await assertSuccessful(await context.post(`${BACKEND_URL}/api/captain-presence/heartbeat`));
    }));
    captainHeartbeatInterval = setInterval(() => {
        captainPresenceContexts.forEach(context => {
            void context.post(`${BACKEND_URL}/api/captain-presence/heartbeat`).catch(() => undefined);
        });
    }, 5000);
}

async function clearCaptainCheckIns() {
    if (captainHeartbeatInterval !== null) {
        clearInterval(captainHeartbeatInterval);
        captainHeartbeatInterval = null;
    }
    const contexts = captainPresenceContexts;
    captainPresenceContexts = [];
    await Promise.all(contexts.map(context => context.post(`${BACKEND_URL}/api/logout`).catch(() => undefined)));
    await closeContexts(...contexts);
}

async function ensureFixtures(request) {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/login`, {
        data: { username: "auctioneer", password: "Sarkar" },
    }));
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));

    const configuredCaptains = await request.get(`${BACKEND_URL}/api/admin/captains`);
    await assertSuccessful(configuredCaptains);
    const captainNames = new Set((await configuredCaptains.json()).map(captain => captain.captainName));
    for (const captainName of captains) {
        if (!captainNames.has(captainName)) {
            await assertSuccessful(await request.post(`${BACKEND_URL}/api/admin/captains`, {
                data: {
                    currentName: "",
                    captainName,
                    password: "e2e-password",
                    totalPoints: 5_000,
                },
            }));
        }
    }

    const availableResponse = await request.get(`${BACKEND_URL}/api/players/available`);
    await assertSuccessful(availableResponse);
    const availableNames = new Set((await availableResponse.json()).map(player => player.name));
    for (const player of players) {
        if (!availableNames.has(player.name)) {
            await assertSuccessful(await request.post(`${BACKEND_URL}/api/players`, {
                data: { ...player, category: "E2E" },
            }));
        }
    }
}

async function startAuction(page) {
    await checkInAllCaptains();
    const startButton = page.getByRole("button", { name: "Start auction" });
    await expect(startButton).toBeEnabled();
    await startButton.click();
    await expect(page.getByRole("button", { name: "Auction started" })).toBeVisible();
}

async function waitForPlayerSale(request, playerName, expectedCaptain, expectedPrice) {
    await expect.poll(async () => {
        const response = await request.get(`${BACKEND_URL}/api/players`);
        if (!response.ok()) return null;
        const player = (await response.json()).find(item => item.name === playerName);
        return player?.sold ? [player.team, player.soldPrice] : null;
    }, { timeout: 15_000 }).toEqual([expectedCaptain, expectedPrice]);
}

test.describe.configure({ mode: "serial" });

test.beforeAll(async ({ request }) => {
    await ensureFixtures(request);
});

test.beforeEach(async ({ request }) => {
    await ensureFixtures(request);
});

test.afterEach(async () => {
    await clearCaptainCheckIns();
});

test("shows player base prices and statuses across screens", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const viewer = await login(browser, "viewer", "viewer");

    try {
        const adminPlayerItem = admin.page.locator(".player-roster-grid li").filter({ hasText: "E2E Player Alpha" });
        const playersResponse = await request.get(`${BACKEND_URL}/api/players`);
        await assertSuccessful(playersResponse);
        await expect(admin.page.getByRole("heading", { name: "Player list" })).toBeVisible();
        await expect(admin.page.locator(".player-roster-grid li")).toHaveCount((await playersResponse.json()).length);
        await expect(adminPlayerItem).toContainText("₹300");
        await expect(adminPlayerItem).toContainText("Available");
        await expect.poll(() => admin.page.locator(".player-roster-grid").evaluate(list => list.scrollHeight === list.clientHeight)).toBe(true);
        await expect(viewer.page.locator(".player-roster-grid li").filter({ hasText: "E2E Player Alpha" })).toContainText("Available");

        await admin.page.locator(".app-nav-settings-menu > summary").click();
        await admin.page.getByRole("button", { name: "Auction Settings" }).click();
        await expect(admin.page.getByRole("heading", { name: "Player list" })).toBeVisible();

        await admin.page.locator(".app-nav-manage-menu > summary").click();
        await admin.page.getByRole("button", { name: "Manual Sale" }).click();
        await expect(admin.page.getByRole("heading", { name: "Player list" })).toBeVisible();
    } finally {
        await closeContexts(viewer.context, admin.context);
    }
});

test("hides next nomination when no players are available", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await startAuction(admin.page);
        await expect(admin.page.locator(".auction-flow-nomination")).toBeVisible();
        await admin.page.route("**/api/players/available", route => route.fulfill({ json: [] }));
        await admin.page.reload();
        await expect(admin.page.locator(".auction-flow-nomination")).toHaveCount(0);
    } finally {
        await closeContexts(admin.context);
    }
});

test("reset auction restores configured team points and roster slots", async ({ request }) => {
    const captainName = captains[0];
    const configResponse = await request.get(`${BACKEND_URL}/api/config`);
    await assertSuccessful(configResponse);
    const config = await configResponse.json();
    config.auctionsSinceLastBreak = 12;
    config.auctionBreakEndsAt = Date.now() + 180_000;
    config.nextNominationCaptain = captainName;
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/config`, { data: config }));

    const teamsBeforeResponse = await request.get(`${BACKEND_URL}/api/teams`);
    await assertSuccessful(teamsBeforeResponse);
    const startingPurse = (await teamsBeforeResponse.json())
        .find(team => team.captainName === captainName).purse;

    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/manual-sale`, {
        data: {
            playerName: "E2E Player Alpha",
            newCaptain: captainName,
            newPrice: 500,
            reason: "Reset purse test",
        },
    }));

    const spentTeamsResponse = await request.get(`${BACKEND_URL}/api/teams`);
    await assertSuccessful(spentTeamsResponse);
    const spentTeam = (await spentTeamsResponse.json()).find(team => team.captainName === captainName);
    expect(spentTeam.purse).toBe(startingPurse - 500);
    expect(spentTeam.playersBought).toBe(1);
    expect(spentTeam.playersLeft).toBe(8);

    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));

    const resetTeamsResponse = await request.get(`${BACKEND_URL}/api/teams`);
    await assertSuccessful(resetTeamsResponse);
    const resetTeam = (await resetTeamsResponse.json()).find(team => team.captainName === captainName);
    expect(resetTeam.purse).toBe(startingPurse);
    expect(resetTeam.playersBought).toBe(0);
    expect(resetTeam.playersLeft).toBe(9);
    expect(resetTeam.tournamentPoints).toBe(0);
    expect(resetTeam.wildPickUsed).toBe(false);

    const resetConfigResponse = await request.get(`${BACKEND_URL}/api/config`);
    await assertSuccessful(resetConfigResponse);
    const resetConfig = await resetConfigResponse.json();
    expect(resetConfig.auctionsSinceLastBreak).toBe(0);
    expect(resetConfig.auctionBreakEndsAt).toBeNull();
    expect(resetConfig.nextNominationCaptain).toBeNull();
});

test("waits for every captain and pauses bidding during a disconnect", async ({ browser, request }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainOne = await login(browser, captains[0], "e2e-password");
    const captainTwo = await loginCaptainRequest(captains[1]);
    const captainThree = await loginCaptainRequest(captains[2]);
    const captainContexts = [captainTwo, captainThree];

    try {
        const startButton = admin.page.getByRole("button", { name: "Start auction" });
        await expect(admin.page.locator(".captain-checkin-strip")).toContainText("3/4 captains checked in");
        await expect(startButton).toBeDisabled();

        const captainFour = await loginCaptainRequest(captains[3]);
        captainContexts.push(captainFour);
        await expect(startButton).toBeEnabled();
        await startButton.click();
        await expect(admin.page.getByRole("button", { name: "Auction started" })).toBeVisible();

        const nomination = captainOne.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Alpha");
        await captainOne.page.getByRole("button", { name: "Nominate player" }).click();
        await assertSuccessful(await captainTwo.post(`${BACKEND_URL}/api/auction/bid/increase`));

        const activeStatusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(activeStatusResponse);
        const activeStatus = await activeStatusResponse.json();
        expect(activeStatus.bidDeadlineAt).toBeGreaterThan(activeStatus.serverTime);

        await assertSuccessful(await captainFour.post(`${BACKEND_URL}/api/logout`));
        await expect.poll(async () => {
            const response = await request.get(`${BACKEND_URL}/api/auction/status`);
            return response.ok() ? (await response.json()).captainDisconnectPaused : false;
        }).toBe(true);

        const pausedStatusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(pausedStatusResponse);
        const pausedStatus = await pausedStatusResponse.json();
        expect(pausedStatus.missingCaptains).toContain(captains[3]);
        expect(pausedStatus.bidDeadlineAt).toBeNull();
        expect(pausedStatus.pausedBidTimeRemainingMillis).toBeGreaterThan(0);
        await expect(admin.page.locator(".captain-checkin-strip")).toContainText("Bidding paused");

        const blockedBidResponse = await captainThree.post(`${BACKEND_URL}/api/auction/bid/increase`);
        expect(await blockedBidResponse.text()).toContain("Waiting for captains to check in");

        const reconnectedCaptainFour = await loginCaptainRequest(captains[3]);
        captainContexts.push(reconnectedCaptainFour);
        await expect.poll(async () => {
            const response = await request.get(`${BACKEND_URL}/api/auction/status`);
            if (!response.ok()) return null;
            const status = await response.json();
            return status.captainDisconnectPaused ? null : status.bidDeadlineAt;
        }).toBeGreaterThan(Date.now());

        const resumedStatusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(resumedStatusResponse);
        const resumedStatus = await resumedStatusResponse.json();
        expect(resumedStatus.missingCaptains).toEqual([]);
        expect(resumedStatus.pausedBidTimeRemainingMillis).toBeNull();
    } finally {
        await Promise.all(captainContexts.map(context => context.post(`${BACKEND_URL}/api/logout`).catch(() => undefined)));
        await closeContexts(...captainContexts, captainOne.context, admin.context);
    }
});

test("three passes sell to the highest bidder and update the viewer", async ({ browser, request }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainOne = await login(browser, captains[0], "e2e-password");
    const captainTwo = await login(browser, captains[1], "e2e-password");
    const viewer = await login(browser, "viewer", "viewer");
    const passContexts = [];

    try {
        await startAuction(admin.page);
        await expect(captainOne.page.getByRole("heading", { name: captains[0] })).toBeVisible();
        await expect(captainTwo.page.getByText(`Waiting for ${captains[0]} to nominate.`)).toBeVisible();

        const nomination = captainOne.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Alpha");
        await captainOne.page.getByRole("button", { name: "Nominate player" }).click();
        await expect(captainOne.page.getByRole("heading", { name: "E2E Player Alpha" })).toBeVisible();
        await expect(viewer.page.getByRole("heading", { name: "E2E Player Alpha" })).toBeVisible();
        await expect(captainOne.page.locator(".auction-metrics strong").nth(0)).toHaveText("₹300");
        await Promise.all([admin.page, captainOne.page, viewer.page].map(page =>
            page.setViewportSize({ width: 390, height: 844 })
        ));
        await expect.poll(() => admin.page.locator(".auction-flow-admin").evaluate(element => getComputedStyle(element).order)).toBe("3");
        await expect.poll(() => admin.page.locator(".auction-flow-current").evaluate(element => getComputedStyle(element).order)).toBe("1");
        await expect.poll(async () => {
            const currentCard = await admin.page.locator(".auction-flow-current").boundingBox();
            const adminControls = await admin.page.locator(".auction-flow-admin").boundingBox();
            return currentCard && adminControls ? currentCard.y < adminControls.y : false;
        }).toBe(true);
        await expect(captainOne.page.locator(".auction-bid-actions .button")).toHaveCSS("min-height", "52px");
        await expect(viewer.page.locator(".auction-flow-current .auction-countdown")).toBeVisible();

        await captainTwo.page.getByLabel("Custom bid").fill("400");
        await captainTwo.page.getByRole("button", { name: "Place custom bid" }).click();
        await expect(captainOne.page.locator(".auction-metrics strong").nth(1)).toHaveText("₹400");
        await expect(viewer.page.locator(".auction-metrics strong").nth(1)).toHaveText("₹400");
        await captainOne.page.getByRole("button", { name: /increase to/i }).click();
        await expect(viewer.page.locator(".auction-metrics strong").nth(1)).toHaveText("₹450");
        await captainTwo.page.getByRole("button", { name: "Pass" }).click();
        for (const captainName of captains.slice(2)) {
            const captainContext = await loginCaptainRequest(captainName);
            passContexts.push(captainContext);
            await assertSuccessful(await captainContext.post(`${BACKEND_URL}/api/auction/bid/pass`));
        }
        await waitForPlayerSale(request, "E2E Player Alpha", captains[0], 450);
        await expect(viewer.page.locator(".player-sold-banner")).toContainText("E2E Player Alpha");
        await expect(viewer.page.locator(".player-sold-banner")).toContainText(captains[0]);
        await expect(viewer.page.locator(".player-sold-banner")).toContainText("₹450");
        const winnerRow = viewer.page.locator(".auction-team-table tbody tr")
            .filter({ hasText: captains[0] });
        await expect(winnerRow).toContainText("₹4550");
        await expect(winnerRow).toContainText("₹3850");
        await expect(winnerRow).toContainText("E2E Player Alpha");
        const [teamResponse, nextStatusResponse] = await Promise.all([
            request.get(`${BACKEND_URL}/api/teams`),
            request.get(`${BACKEND_URL}/api/auction/status`),
        ]);
        await assertSuccessful(teamResponse);
        await assertSuccessful(nextStatusResponse);
        const captainOrder = (await teamResponse.json()).sort((first, second) => first.id - second.id)
            .map(team => team.captainName);
        const nextCaptain = captainOrder[(captainOrder.indexOf(captains[0]) + 1) % captainOrder.length];
        await expect.poll(async () => {
            const response = await request.get(`${BACKEND_URL}/api/auction/status`);
            if (!response.ok()) return null;
            return (await response.json()).nextNominationCaptain;
        }).toBe(nextCaptain);
    } finally {
        await closeContexts(...passContexts, viewer.context, captainTwo.context, captainOne.context, admin.context);
    }
});

test("three passes with no bids mark the player unsold", async ({ browser, request }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainOne = await login(browser, captains[0], "e2e-password");
    const passContexts = [];

    try {
        await startAuction(admin.page);
        const nomination = captainOne.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Bravo");
        await captainOne.page.getByRole("button", { name: "Nominate player" }).click();
        await expect(captainOne.page.getByRole("heading", { name: "E2E Player Bravo" })).toBeVisible();

        for (const captainName of captains.slice(0, 3)) {
            const captainContext = await loginCaptainRequest(captainName);
            passContexts.push(captainContext);
            await assertSuccessful(await captainContext.post(`${BACKEND_URL}/api/auction/bid/pass`));
        }

        await expect.poll(async () => {
            const response = await request.get(`${BACKEND_URL}/api/auction/current`);
            if (!response.ok()) return "error";
            return (await response.json()).currentPlayer;
        }, { timeout: 15_000 }).toBe("");

        const playersResponse = await request.get(`${BACKEND_URL}/api/players`);
        await assertSuccessful(playersResponse);
        const player = (await playersResponse.json()).find(item => item.name === "E2E Player Bravo");
        expect(player.sold).toBeFalsy();
        const statusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(statusResponse);
        expect((await statusResponse.json()).nextNominationCaptain).toBe(captains[1]);
    } finally {
        await closeContexts(...passContexts, captainOne.context, admin.context);
    }
});

test("a captain can rejoin bidding after passing", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const nominatingCaptain = await login(browser, captains[0], "e2e-password");
    const biddingCaptain = await login(browser, captains[1], "e2e-password");

    try {
        await startAuction(admin.page);
        const nomination = nominatingCaptain.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Alpha");
        await nominatingCaptain.page.getByRole("button", { name: "Nominate player" }).click();

        await biddingCaptain.page.getByRole("button", { name: "Pass" }).click();
        await expect(biddingCaptain.page.getByRole("button", { name: "Rejoin bidding" })).toBeVisible();
        await expect(nominatingCaptain.page.getByText("1 of 4 captains passed")).toBeVisible();

        await biddingCaptain.page.getByRole("button", { name: "Increase to ₹350" }).click();
        await expect(nominatingCaptain.page.locator(".auction-metrics strong").nth(1)).toHaveText("₹350");
        const statusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(statusResponse);
        expect((await statusResponse.json()).passedCaptains).not.toContain(captains[1]);
    } finally {
        await closeContexts(biddingCaptain.context, nominatingCaptain.context, admin.context);
    }
});

test("fixed and custom bid controls enforce the team max bid", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const nominatingCaptain = await login(browser, captains[0], "e2e-password");
    const biddingCaptain = await login(browser, captains[1], "e2e-password");

    try {
        await startAuction(admin.page);
        const nomination = nominatingCaptain.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Alpha");
        await nominatingCaptain.page.getByRole("button", { name: "Nominate player" }).click();
        await expect(biddingCaptain.page.getByRole("heading", { name: "E2E Player Alpha" })).toBeVisible();

        const teamsResponse = await request.get(`${BACKEND_URL}/api/teams`);
        await assertSuccessful(teamsResponse);
        const teams = await teamsResponse.json();
        const nominatingTeam = teams.find(team => team.captainName === captains[0]);
        const biddingTeam = teams.find(team => team.captainName === captains[1]);
        const nominatingMaxBid = nominatingTeam.purse - ((nominatingTeam.playersLeft - 1) * 100);
        const biddingMaxBid = biddingTeam.purse - ((biddingTeam.playersLeft - 1) * 100);

        await biddingCaptain.page.getByLabel("Custom bid").fill(String(biddingMaxBid));
        await biddingCaptain.page.getByRole("button", { name: "Place custom bid" }).click();
        await expect(nominatingCaptain.page.locator(".auction-metrics strong").nth(1)).toHaveText(`₹${biddingMaxBid}`);
        await expect(nominatingCaptain.page.getByRole("button", { name: `Increase to ₹${biddingMaxBid + 50}` })).toBeDisabled();

        const customBid = nominatingCaptain.page.getByLabel("Custom bid");
        await expect(customBid).toHaveAttribute("max", String(nominatingMaxBid));
        const tooHighBid = Math.ceil((Math.max(biddingMaxBid, nominatingMaxBid) + 1) / 50) * 50;
        await customBid.fill(String(tooHighBid));
        await expect(nominatingCaptain.page.getByRole("button", { name: "Place custom bid" })).toBeDisabled();
    } finally {
        await closeContexts(biddingCaptain.context, nominatingCaptain.context, admin.context);
    }
});

test("captain timeout pauses bidding and uses one of three timeouts", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[0], "e2e-password");
    const viewer = await login(browser, "viewer", "viewer");

    try {
        await startAuction(admin.page);
        const nomination = captain.page.getByRole("combobox", { name: "Choose a player to nominate" });
        await nomination.selectOption("E2E Player Alpha");
        await captain.page.getByRole("button", { name: "Nominate player" }).click();
        await expect(captain.page.getByRole("heading", { name: "E2E Player Alpha" })).toBeVisible();

        await captain.page.getByRole("button", { name: "Pause auction · 3 timeouts left" }).click();
        await expect(captain.page.getByRole("button", { name: "Pause auction · 2 timeouts left" })).toBeDisabled();
        await expect(captain.page.getByRole("button", { name: /increase to/i })).toBeDisabled();
        await expect(viewer.page.locator(".auction-timeout-banner")).toContainText(`${captains[0]} called timeout`);
        await expect(viewer.page.locator(".auction-timeout-banner")).toContainText("seconds");

        const statusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(statusResponse);
        const status = await statusResponse.json();
        expect(status.timeoutUsage[captains[0]]).toBe(1);
        expect(status.auctionTimeoutEndsAt).toBeGreaterThan(status.serverTime);
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    } finally {
        await closeContexts(viewer.context, captain.context, admin.context);
    }
});

test("admin can configure the bid countdown", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await admin.page.locator(".app-nav-settings-menu > summary").click();
        await admin.page.getByRole("button", { name: "Auction Settings" }).click();
        const timerInput = admin.page.getByLabel("Bid countdown (seconds)");
        await timerInput.fill("25");
        await admin.page.getByRole("button", { name: "Save Settings" }).click();

        await expect.poll(async () => {
            const response = await request.get(`${BACKEND_URL}/api/config`);
            if (!response.ok()) return null;
            return (await response.json()).bidTimerSeconds;
        }).toBe(25);
    } finally {
        await closeContexts(admin.context);
    }
});
