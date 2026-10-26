# 🏏 BTC Auction Manager – Season 12

A web-based cricket auction management platform built for **Belgharia Turf Cricket – Season 12**.


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
- Nominator must open at the announced player base price
- Bid increments: ₹50 through ₹1,000; ₹100 thereafter
- No auction-wide Max Bid cap; the purse is still deducted by the final payable price.
- Each captain secretly curses two distinct players before the auction. If another captain buys a cursed player, compute `20% × final payable price × number of curses`, then round to the nearest 50 points (halfway rounds up). The buyer loses that many tournament points, floored at zero, and each setter receives the full rounded amount. If any setter buys the player, all curses on them cancel. RTM uses the final acquiring captain. Curses trigger once and expire if still unsold after re-auction; they never affect purse or bidding limits.
- The unique highest valid blind bidder is eligible for +300 tournament points only if they become the final buyer after RTM. If blind bids tie, eligibility goes to the final unique highest bidder after tie-break rounds.
- Blind Bonanza is a random event that upgrades that player's +300 blind-bid bonus to +500. It is awarded only if the eligible bidder becomes the final buyer, and can successfully upgrade at most four player sales per auction.
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


# 🏆 Season 12 Captains

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
- Tournament Points and Curse transfers
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
