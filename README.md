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
- Dynamic max bid: `total points − ((players to buy − 2) × ₹100)`
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

- Java 21
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

## Max Bid Formula

Total Points − ((Players to Buy − 2) × ₹100)


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
- Squad Tracking
- Max Bid Calculation


# 🔮 Future Enhancements

- Authentication (JWT)
- PostgreSQL Support
- AWS Deployment
- Auction Analytics
- Export Auction Results
- Backup & Restore
- Live TV Display Mode
- Real-time updates using WebSockets
