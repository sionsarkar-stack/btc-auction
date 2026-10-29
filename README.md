# 🏏 BTC Auction Manager – Season 13

A web-based cricket auction management platform built for **Belgharia Turf Cricket – Season 13**.


# ✨ Features

## Auction Management

- Spinning Wheel Player Selection
- Live Auction Dashboard
- Live Bid Tracking
- Call SOLD Workflow
- Last Strike Window
- Sell Player Workflow
- Undo Last Sale
- Manual Sale Correction
- Recent Auction Events

## Silent Bid

- Secret Silent Bid Round
- Live Captain Submission Status
- Automatic Tie Detection
- Unlimited Tie-Break Rounds
- Silent Bid Winner Reveal
- Silent Bid Winner Overlay

## Tournament Rules

- Four captains: Sen (₹5,000), Gappu (₹5,300), Anirban (₹5,300), and Joy (₹5,300)
- 10-player squads including each captain (nine auction purchases per team)
- Seed C players never enter the lottery independently, even without a configured pair; they are revealed and sold with their paired A player.
- Nominator must open at the announced player base price
- Bid increments: ₹50 through ₹1,000; ₹100 thereafter
- Opening, live, silent and RTM bids must be affordable after market adjustments, applicable opening-bid discounts and the full rounded curse penalty. The spendable purse is deducted by the effective player/package price. With ₹150 purse and no curse, the opening-bid cap is ₹450 normally, ₹350 under a ₹100 Market Boom, or ₹650 under eligible Blind Bonanza. Later bids get the discount only for the eligible opening-bid winner; silent bidding does not create opening-bid eligibility. Squad capacity must also cover the entire package. Server checks enforce these limits again before SOLD and final settlement; captain screens show the current maximum. Admin manual sale corrections remain explicit overrides.
- Each captain secretly curses two distinct players before the auction. If another captain buys a cursed player, compute `20% × final payable price × number of distinct curse-setters`, then round to the nearest ₹50 (halfway rounds up). Add the full amount to the buyer's effective price and credit each setter's spendable purse by that amount. No tournament-points balance or zero-floor limits the penalty: a ₹550 sale with a ₹100 curse costs ₹650. An A + C paired sale is one curse target: curses on either player cause at most one deduction, and a captain who cursed both counts once. A buyer's own curse cancels curses on that same player. RTM uses the final acquiring captain. Curses trigger once and expire if still unsold after re-auction.
- The unique highest valid blind bidder earns a ₹300 effective-price discount only if they become the final buyer after RTM. If blind bids tie, eligibility goes to the final unique highest bidder after tie-break rounds. An A + C paired sale awards this discount at most once for the pair.
- Blind Bonanza is a random event that upgrades that player's ₹300 blind-bid discount to ₹500. It is awarded only if the eligible bidder becomes the final buyer, and can successfully upgrade at most four player sales per auction.
- Effective prices subtract a buyer's blind-bid discount and add the full curse penalty, floored at zero. The actual spendable purse equals the starting budget minus effective squad value plus curse-setter rewards, retaining separate purse events/admin adjustments. Recorded sale prices remain unchanged. Existing sales and setter rewards are reconciled on startup using persisted adjustment balances, so restarting never credits a reward twice. Undo, cancellation, manual transfers and re-auction rollback restore these balances with the sale. Legacy tournament-point columns remain only for historical storage and are not used in accounting or exposed in the UI/API.
- Two secret targets per captain: +₹400 for both, +₹50 net for one, −₹200 for neither
- One reverse target per captain: selected rival's purchase deducts ₹200 from that rival's purse
- RTM+ challenge flow after SOLD

## Admin Tools

- Auction Start / End
- Reset Auction
- Manual Sale
- Undo Sale
- Auction Event Log
- Joker Assignment
- Silent Bid Manager


# 🛠 Tech Stack

## Frontend

- React
- Vite
- CSS
- Axios

## Backend

- Java 25
- Spring Boot
- Maven
- Spring Data JPA


# 🏆 Season 13 Captains

| Captain | Starting Purse |
|---------|---------------:|
| Sen | 5000 |
| Gappu | 5300 |
| Anirban | 5300 |
| Joy | 5300 |


# 📐 Auction Rules

## Squad Size

10 Players per Team (including Captain); each captain purchases 9 players.

# ▶ Running Locally

## Backend

```bash
mvn spring-boot:run
```

Runs on:

```
http://localhost:8080
```

## Frontend

```bash
npm install
npm run dev
```

Runs on:

```
http://localhost:5173
```

## Browser E2E Tests

From `frontend/btc-auction-ui`:

```bash
npx playwright install chromium
npm run test:e2e
```

The test command starts the frontend and an isolated in-memory H2 backend profile automatically.


# 🚀 Current Status

## ✅ Completed

- Live Auction
- Silent Bid
- Tie Break
- Joker System
- Last Strike
- Call SOLD
- Forbidden Pick
- Trusted Captain
- Tribunal Vote
- Manual Sale
- Undo Sale
- Reset Auction
- Auction Events
- Live Dashboard
- Team Purse Tracking
- Effective-price discounts, curse penalties and direct purse rewards
- Squad Tracking


# 🔮 Future Enhancements

- Authentication (JWT)
- PostgreSQL Support
- AWS Deployment
- Auction Analytics
- Export Auction Results
- Backup & Restore
- Live TV Display Mode
- Real-time updates using WebSockets
