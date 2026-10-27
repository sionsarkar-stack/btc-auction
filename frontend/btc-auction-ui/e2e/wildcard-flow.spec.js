import { expect, request as playwrightRequest, test } from "@playwright/test";

const BACKEND_URL = "http://127.0.0.1:8081";
const captains = [
    "E2E Captain One",
    "E2E Captain Two",
    "E2E Captain Three",
    "E2E Captain Four",
    "E2E Captain Five",
];
const players = [
    { name: "E2E Player Alpha", seed: "A", basePrice: 300 },
    { name: "E2E Player Bravo", seed: "B", basePrice: 250 },
];

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
    await Promise.all(contexts.map(context => context.close().catch(() => undefined)));
}

async function submitOpeningBidAsCaptain(playerName, captainName, bidAmount) {
    const captainRequest = await playwrightRequest.newContext();
    try {
        await assertSuccessful(await captainRequest.post(`${BACKEND_URL}/api/login`, {
            data: { username: captainName, password: "e2e-password" },
        }));
        const response = await captainRequest.post(`${BACKEND_URL}/api/auction/blind-opening-bid/submit`, {
            data: { playerName, captainName, bidAmount },
        });
        expect(await assertSuccessful(response)).toBe("Blind opening bid submitted.");
    } finally {
        await captainRequest.dispose();
    }
}

async function spinWheel(page) {
    await page.getByRole("button", { name: "BTC Control Room" }).click();
    const spinButton = page.getByTestId("spin-wheel");
    const playerInput = page.getByTestId("current-nominated-player");
    await expect(spinButton).toBeEnabled();
    await spinButton.click();
    await expect(playerInput).toHaveValue("Wheel spinning...");
    await expect(page.locator(".player-walkout")).toHaveClass(/is-walking-out/);
    await expect(spinButton).toBeDisabled();
    await expect(playerInput).not.toHaveValue("Wheel spinning...");
    await expect(playerInput).not.toHaveValue("No player nominated");
    const selectedPlayer = await playerInput.inputValue();
    await expect(page.locator(".walkout-player-name")).toHaveText(selectedPlayer);
    return selectedPlayer;
}

test.describe.configure({ mode: "serial" });

test.beforeAll(async ({ request }) => {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/login`, {
        data: { username: "auctioneer", password: "Sarkar" },
    }));
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));

    for (const captainName of captains) {
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/admin/captains`, {
            data: {
                currentName: "",
                captainName,
                password: "e2e-password",
                totalPoints: 5_000,
            },
        }));
    }

    for (const player of players) {
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/players`, {
            data: {
                ...player,
                category: "E2E",
            },
        }));
    }

});

test.beforeEach(async ({ request }) => {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/login`, {
        data: { username: "auctioneer", password: "Sarkar" },
    }));
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    const availablePlayersResponse = await request.get(`${BACKEND_URL}/api/players/available`);
    await assertSuccessful(availablePlayersResponse);
    const availablePlayerNames = new Set((await availablePlayersResponse.json()).map(player => player.name));
    for (const player of players) {
        if (!availablePlayerNames.has(player.name)) {
            await assertSuccessful(await request.post(`${BACKEND_URL}/api/players`, {
                data: {
                    ...player,
                    category: "E2E",
                },
            }));
        }
    }
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/curse-players/auto-select`));
    const startResponse = await request.post(`${BACKEND_URL}/api/auction/start`);
    expect(await startResponse.text()).toBe("Auction Started Successfully.");
});

test("requires a captain session and ignores a supplied captain name", async ({ request }) => {
    const anonymousContext = await playwrightRequest.newContext();
    try {
        const anonymousAttempt = await anonymousContext.post(`${BACKEND_URL}/api/auction/wild-pick`, {
            data: { captainName: captains[0] },
        });
        expect(anonymousAttempt.status()).toBe(401);

        const adminAttempt = await request.post(`${BACKEND_URL}/api/auction/wild-pick`, {
            data: { captainName: captains[0] },
        });
        expect(adminAttempt.status()).toBe(403);
    } finally {
        await anonymousContext.dispose();
    }
});

test("reset clears auction state and requires new curse picks", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));

        const statusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(statusResponse);
        expect((await statusResponse.json()).curseSelectionEnabled).toBeFalsy();

        await admin.page.reload();
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();
        await expect(admin.page.getByRole("button", { name: "🚀 Start Auction" })).toBeDisabled();
    } finally {
        await closeContexts(admin.context);
    }
});

test("admin navigation menus collapse each other", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const settingsMenu = admin.page.locator(".app-nav-settings-menu");
    const manageMenu = admin.page.locator(".app-nav-manage-menu");

    try {
        await admin.page.locator(".app-nav-settings-menu > summary").click();
        await expect(settingsMenu).toHaveJSProperty("open", true);

        await admin.page.locator(".app-nav-manage-menu > summary").click();
        await expect(manageMenu).toHaveJSProperty("open", true);
        await expect(settingsMenu).toHaveJSProperty("open", false);

        await admin.page.locator(".app-nav-settings-menu > summary").click();
        await expect(settingsMenu).toHaveJSProperty("open", true);
        await expect(manageMenu).toHaveJSProperty("open", false);
    } finally {
        await closeContexts(admin.context);
    }
});

test("bypass curse explains when no captain is configured", async ({ browser, request }) => {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await admin.page.route("**/api/teams", route => route.fulfill({ json: [] }));
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();

        const bypassButton = admin.page.getByRole("button", { name: "Bypass: Auto-select Curses" });
        await expect(bypassButton).toBeDisabled();
        await expect(admin.page.getByText("Add at least one captain before bypassing curse selection.")).toBeVisible();
    } finally {
        await closeContexts(admin.context);
    }
});

test("bypass curse stays disabled when no players are imported", async ({ browser, request }) => {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await admin.page.route("**/api/players/available", route => route.fulfill({ json: [] }));
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();

        const bypassButton = admin.page.getByRole("button", { name: "Bypass: Auto-select Curses" });
        await expect(bypassButton).toBeDisabled();
        await expect(admin.page.getByRole("alert")).toContainText("Import at least two available players");
        await expect(admin.page.getByRole("button", { name: "🚀 Start Auction" })).toBeDisabled();
    } finally {
        await closeContexts(admin.context);
    }
});

test("manager keeps curse actions disabled when player data fails to load", async ({ browser, request }) => {
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await admin.page.route("**/api/players/available", route => route.fulfill({
            status: 503,
            contentType: "application/json",
            body: JSON.stringify({ message: "Player database is unavailable." }),
        }));
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();

        await expect(admin.page.getByRole("alert")).toContainText("Player database is unavailable.");
        await expect(admin.page.getByRole("button", { name: "Bypass: Auto-select Curses" })).toBeDisabled();
        await expect(admin.page.getByRole("button", { name: "🚀 Start Auction" })).toBeDisabled();
    } finally {
        await closeContexts(admin.context);
    }
});

test("walks every screen through the same nomination reveal", async ({ browser }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[0], "e2e-password");
    const viewer = await login(browser, "viewer", "viewer");

    try {
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();
        await Promise.all([admin.page, captain.page, viewer.page].map(page =>
            page.locator(".bottle-draw-webgl[data-render-state=ready]").waitFor()
        ));
        const spinButton = admin.page.getByTestId("spin-wheel");
        await spinButton.click();

        await expect(admin.page.getByTestId("current-nominated-player")).toHaveValue("Wheel spinning...");
        await expect(admin.page.locator(".player-walkout")).toHaveClass(/is-walking-out/);
        await expect(captain.page.locator(".player-walkout")).toHaveClass(/is-walking-out/);
        await expect(viewer.page.locator(".player-walkout")).toHaveClass(/is-walking-out/);

        await expect(admin.page.getByTestId("current-nominated-player")).not.toHaveValue("Wheel spinning...", { timeout: 20_000 });
        const selectedPlayer = await admin.page.getByTestId("current-nominated-player").inputValue();

        await expect(admin.page.locator(".walkout-player-name")).toHaveText(selectedPlayer);
        await expect(captain.page.locator(".walkout-player-name")).toHaveText(selectedPlayer);
        await expect(viewer.page.locator(".walkout-player-name")).toHaveText(selectedPlayer);
    } finally {
        await closeContexts(viewer.context, captain.context, admin.context);
    }
});

test("captain Wildcard returns the player and lets the admin spin again", async ({ browser }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[0], "e2e-password");

    try {
        await spinWheel(admin.page);
        const wildcardButton = captain.page.getByTestId("activate-wildcard");
        await expect(wildcardButton).toBeVisible();

        const dialogPromise = captain.page.waitForEvent("dialog");
        await Promise.all([
            wildcardButton.click(),
            dialogPromise.then(async dialog => {
                expect(dialog.message()).toContain("cancels every submitted blind opening bid");
                await dialog.accept();
            }),
        ]);

        await expect(admin.page.getByTestId("current-nominated-player")).toHaveValue("No player nominated");
        await expect(admin.page.getByTestId("spin-wheel")).toBeEnabled();

        await spinWheel(admin.page);
    } finally {
        await closeContexts(captain.context, admin.context);
    }
});

test("marks a nominee unsold, restarts the auction, and cancels re-auction", async ({ browser, request }) => {
    test.setTimeout(60_000);
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        const deferredPlayer = await spinWheel(admin.page);

        const markUnsoldDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: /mark unsold/i }).click(),
            markUnsoldDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByTestId("current-nominated-player")).toHaveValue("No player nominated");

        const heldPool = await request.get(`${BACKEND_URL}/api/players/available`);
        await assertSuccessful(heldPool);
        expect((await heldPool.json()).map(player => player.name)).not.toContain(deferredPlayer);

        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/end`));
        await admin.page.reload();
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();

        const restartDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: /restart auction/i }).click(),
            restartDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByText("Re-auction started. Unsold players will return at 50% of their original base price.")).toBeVisible();

        const restartedStatus = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(restartedStatus);
        expect((await restartedStatus.json()).auctionRound).toBe(2);

        const reAuctionPool = await request.get(`${BACKEND_URL}/api/players/available`);
        await assertSuccessful(reAuctionPool);
        expect((await reAuctionPool.json()).map(player => player.name)).toContain(deferredPlayer);

        const teamsBeforeSale = await request.get(`${BACKEND_URL}/api/teams`);
        await assertSuccessful(teamsBeforeSale);
        const originalTeam = (await teamsBeforeSale.json()).find(team => team.captainName === captains[0]);

        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/manual-sale`, {
            data: {
                playerName: deferredPlayer,
                newCaptain: captains[0],
                newPrice: 600,
            },
        }));

        const cancelDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: /cancel re-auction/i }).click(),
            cancelDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByText("Re-auction cancelled. Re-auction sales were rolled back and unsold players returned to the wheel.")).toBeVisible();

        const restoredStatus = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(restoredStatus);
        const restoredConfig = await restoredStatus.json();
        expect(restoredConfig.auctionRound).toBe(1);
        expect(restoredConfig.auctionStarted).toBeTruthy();

        const restoredPlayers = await request.get(`${BACKEND_URL}/api/players`);
        await assertSuccessful(restoredPlayers);
        const restoredPlayer = (await restoredPlayers.json()).find(player => player.name === deferredPlayer);
        expect(restoredPlayer.sold).toBeFalsy();
        expect(restoredPlayer.deferredToReAuction).toBeFalsy();

        const restoredPool = await request.get(`${BACKEND_URL}/api/players/available`);
        await assertSuccessful(restoredPool);
        expect((await restoredPool.json()).map(player => player.name)).toContain(deferredPlayer);

        const restoredTeams = await request.get(`${BACKEND_URL}/api/teams`);
        await assertSuccessful(restoredTeams);
        const restoredTeam = (await restoredTeams.json()).find(team => team.captainName === captains[0]);
        expect(restoredTeam.purse).toBe(originalTeam.purse);
        expect(restoredTeam.playersBought).toBe(originalTeam.playersBought);
        expect(restoredTeam.playersLeft).toBe(originalTeam.playersLeft);
    } finally {
        await closeContexts(admin.context);
    }
});

test("re-auction removes the live bid maximum", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/end`));
        await admin.page.reload();
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();

        const restartDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: /restart auction/i }).click(),
            restartDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByText("Re-auction started. Unsold players will return at 50% of their original base price.")).toBeVisible();

        const selectedPlayer = await spinWheel(admin.page);
        for (const [index, captainName] of captains.entries()) {
            await submitOpeningBidAsCaptain(selectedPlayer, captainName, 10_000 + index * 100);
        }

        const captain = await login(browser, captains[0], "e2e-password");
        try {
            const liveBid = captain.page.getByRole("spinbutton", { name: "Enter bid" });
            await expect(liveBid).toBeVisible();
            expect(await liveBid.getAttribute("max")).toBeNull();

            await liveBid.fill("11000");
            await captain.page.getByRole("button", { name: "Place Bid" }).click();
            await expect(captain.page.locator(".captain-actions").getByRole("status")).toHaveText("Updated");

            const currentAuction = await request.get(`${BACKEND_URL}/api/auction/current`);
            await assertSuccessful(currentAuction);
            expect((await currentAuction.json()).currentBid).toBe(11_000);
        } finally {
            await closeContexts(captain.context);
        }
    } finally {
        await closeContexts(admin.context);
    }
});

test("settles a normal auction after blind opening and live bids", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainSessions = [];

    try {
        const selectedPlayer = await spinWheel(admin.page);
        for (const captainName of captains) {
            captainSessions.push(await login(browser, captainName, "e2e-password"));
        }

        for (const [index, captain] of captainSessions.entries()) {
            await expect(captain.page.getByTestId("blind-opening-bid")).toBeVisible();
            await captain.page.getByTestId("blind-opening-bid").fill(String(300 + index * 50));
            await captain.page.getByTestId("place-blind-opening-bid").click();
        }

        const liveBidder = captainSessions[0].page;
        await expect(liveBidder.getByRole("button", { name: "Place Bid" })).toBeVisible();
        await liveBidder.getByRole("spinbutton", { name: "Enter bid" }).fill("550");
        await liveBidder.getByRole("button", { name: "Place Bid" }).click();

        await expect(admin.page.getByLabel("Player nominated")).toHaveValue(selectedPlayer);
        await admin.page.getByLabel("Player", { exact: true }).selectOption(selectedPlayer);
        await admin.page.getByLabel("Winning Captain").selectOption(captains[0]);
        await admin.page.getByLabel("Sold Price").fill("550");

        const callSoldDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: "🟢 CALL SOLD" }).click(),
            callSoldDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByText("Waiting for RTM.")).toBeVisible();

        const confirmSaleDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            admin.page.getByRole("button", { name: "✅ CONFIRM SALE" }).click(),
            confirmSaleDialog.then(dialog => dialog.accept()),
        ]);

        await expect(admin.page.getByLabel("Player nominated")).toHaveValue("No player nominated");
    } finally {
        await closeContexts(...captainSessions.map(captain => captain.context), admin.context);
    }
});

test("settles a silent auction after secret bids", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainSessions = [];

    try {
        const selectedPlayer = await spinWheel(admin.page);
        for (const captainName of captains) {
            captainSessions.push(await login(browser, captainName, "e2e-password"));
        }
        await admin.page.getByRole("button", {
            name: new RegExp(`Start Silent Bid Round for ${selectedPlayer}`),
        }).click();

        for (const [index, captain] of captainSessions.entries()) {
            const secretBid = captain.page.getByPlaceholder("Secret silent bid");
            await expect(secretBid).toBeVisible();
            await secretBid.fill(String(300 + index * 50));
            await captain.page.getByRole("button", { name: "Submit Silent Bid" }).click();
        }

        const revealWinner = admin.page.getByRole("button", { name: "🏆 Reveal Winner" });
        await revealWinner.click();

        const callSold = admin.page.getByRole("button", { name: "🔨 Call SOLD / Open RTM" });
        await expect(callSold).toBeEnabled();
        const callSoldDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            callSold.click(),
            callSoldDialog.then(dialog => dialog.accept()),
        ]);
        await expect(admin.page.getByRole("status").filter({ hasText: "Waiting for RTM." })).toBeVisible();

        const confirmSale = admin.page.getByRole("button", { name: "💰 Confirm Sale" });
        await expect(confirmSale).toBeEnabled();
        const confirmSaleDialog = admin.page.waitForEvent("dialog");
        await Promise.all([
            confirmSale.click(),
            confirmSaleDialog.then(dialog => dialog.accept()),
        ]);

        await expect(admin.page.getByLabel("Player nominated")).toHaveValue("No player nominated");
    } finally {
        await closeContexts(...captainSessions.map(captain => captain.context), admin.context);
    }
});

test("removes Wildcard after all five blind opening bids are submitted", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[1], "e2e-password");

    try {
        const selectedPlayer = await spinWheel(admin.page);
        await expect(captain.page.getByTestId("activate-wildcard")).toBeVisible();

        for (const captainName of captains) {
            await submitOpeningBidAsCaptain(selectedPlayer, captainName, 300);
        }

        await captain.page.reload();
        await expect(captain.page.getByTestId("activate-wildcard")).toHaveCount(0);
    } finally {
        await closeContexts(captain.context, admin.context);
    }
});