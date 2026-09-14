import { expect, test } from "@playwright/test";

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

async function spinWheel(page) {
    await page.getByRole("button", { name: "BTC Control Room" }).click();
    const spinButton = page.getByTestId("spin-wheel");
    const playerInput = page.getByTestId("current-nominated-player");
    await expect(spinButton).toBeEnabled();
    await spinButton.click();
    await expect(playerInput).toHaveValue("Wheel spinning...");
    await expect(page.locator(".casino-reel")).toHaveClass(/spinning/);
    await expect(spinButton).toBeDisabled();
    await expect(playerInput).not.toHaveValue("Wheel spinning...");
    await expect(playerInput).not.toHaveValue("No player nominated");
    const selectedPlayer = await playerInput.inputValue();
    await expect(page.locator(".casino-reel-result .casino-result-name")).toHaveText(selectedPlayer);
    return selectedPlayer;
}

test.describe.configure({ mode: "serial" });

test.beforeAll(async ({ request }) => {
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
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));
    await assertSuccessful(await request.post(`${BACKEND_URL}/api/protection-players/auto-select`));
    const startResponse = await request.post(`${BACKEND_URL}/api/auction/start`);
    expect(await startResponse.text()).toBe("Auction Started Successfully.");
});

test("requires a captain session and ignores a supplied captain name", async ({ request }) => {
    const anonymousAttempt = await request.post(`${BACKEND_URL}/api/auction/wild-pick`, {
        data: { captainName: captains[0] },
    });
    expect(anonymousAttempt.status()).toBe(401);

    await assertSuccessful(await request.post(`${BACKEND_URL}/api/login`, {
        data: { username: "auctioneer", password: "Sarkar" },
    }));
    const adminAttempt = await request.post(`${BACKEND_URL}/api/auction/wild-pick`, {
        data: { captainName: captains[0] },
    });
    expect(adminAttempt.status()).toBe(403);
});

test("reset clears protections and disables Start Auction", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");

    try {
        await assertSuccessful(await request.post(`${BACKEND_URL}/api/auction/reset`));

        const protectionsResponse = await request.get(`${BACKEND_URL}/api/protection-players`);
        await assertSuccessful(protectionsResponse);
        expect(await protectionsResponse.json()).toEqual([]);

        const statusResponse = await request.get(`${BACKEND_URL}/api/auction/status`);
        await assertSuccessful(statusResponse);
        expect((await statusResponse.json()).protectionSelectionEnabled).toBeFalsy();

        await admin.page.reload();
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();
        await expect(admin.page.getByRole("button", { name: /start auction/i })).toBeDisabled();
    } finally {
        await closeContexts(admin.context);
    }
});

test("spins every reel together and reveals one nominee", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[0], "e2e-password");
    const viewer = await login(browser, "viewer", "viewer");

    try {
        await admin.page.getByRole("button", { name: "BTC Control Room" }).click();
        const spinButton = admin.page.getByTestId("spin-wheel");
        await spinButton.click();

        await expect(admin.page.getByTestId("current-nominated-player")).toHaveValue("Wheel spinning...");
        await expect(admin.page.locator(".casino-reel")).toHaveClass(/spinning/);
        await expect(captain.page.locator(".casino-reel")).toHaveClass(/spinning/);
        await expect(viewer.page.locator(".casino-reel")).toHaveClass(/spinning/);

        await expect(admin.page.getByTestId("current-nominated-player")).not.toHaveValue("Wheel spinning...");
        const selectedPlayer = await admin.page.getByTestId("current-nominated-player").inputValue();

        await expect(admin.page.locator(".casino-reel-result .casino-result-name")).toHaveText(selectedPlayer);
        await expect(captain.page.locator(".casino-reel-result .casino-result-name")).toHaveText(selectedPlayer);
        await expect(viewer.page.locator(".casino-reel-result .casino-result-name")).toHaveText(selectedPlayer);
    } finally {
        await closeContexts(viewer.context, captain.context, admin.context);
    }
});

test("captain Wildcard returns the player and lets the admin spin again", async ({ browser }) => {
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

test("settles a normal auction after blind opening and live bids", async ({ browser }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captainSessions = await Promise.all(
        captains.map(captainName => login(browser, captainName, "e2e-password"))
    );

    try {
        const selectedPlayer = await spinWheel(admin.page);

        for (const [index, captain] of captainSessions.entries()) {
            await expect(captain.page.getByTestId("blind-opening-bid")).toBeVisible();
            await captain.page.getByTestId("blind-opening-bid").fill(String(300 + index * 50));
            await captain.page.getByTestId("place-blind-opening-bid").click();
        }

        const liveBidder = captainSessions[0].page;
        await expect(liveBidder.getByRole("button", { name: "Place Bid" })).toBeVisible();
        await liveBidder.getByPlaceholder(/start bid/i).fill("550");
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
    const captainSessions = await Promise.all(
        captains.map(captainName => login(browser, captainName, "e2e-password"))
    );

    try {
        const selectedPlayer = await spinWheel(admin.page);
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
        await expect(admin.page.getByRole("status")).toContainText("Waiting for RTM.");

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

test("removes Wildcard after all five blind opening bids are submitted", async ({ browser, request }) => {
    const admin = await login(browser, "auctioneer", "Sarkar");
    const captain = await login(browser, captains[1], "e2e-password");

    try {
        const selectedPlayer = await spinWheel(admin.page);
        await expect(captain.page.getByTestId("activate-wildcard")).toBeVisible();

        for (const captainName of captains) {
            const response = await request.post(`${BACKEND_URL}/api/auction/blind-opening-bid/submit`, {
                data: {
                    playerName: selectedPlayer,
                    captainName,
                    bidAmount: 300,
                },
            });
            expect(await response.text()).toBe("Blind opening bid submitted.");
        }

        await captain.page.reload();
        await expect(captain.page.getByTestId("activate-wildcard")).toHaveCount(0);
    } finally {
        await closeContexts(captain.context, admin.context);
    }
});