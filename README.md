# 🏏 BTC Auction Manager – Season 11

A web-based cricket auction management platform built for **Belgharia Turf Cricket – Season 11**.

---

# ✨ Features

## Auction Management

- Player Nomination
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
- Dynamic max bid: `purse − (vacant slots × ₹100)`
- Four normal bounty players (+₹100) and two golden bounty players (+₹200)
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

---

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

---

# 🏆 Season 11 Captains

| Captain | Starting Purse |
|---------|---------------:|
| Sen | 5000 |
| Gappu | 5300 |
| Anirban | 5300 |
| Joy | 5300 |

---

# 📐 Auction Rules

## Squad Size

10 Players per Team (including Captain); each captain purchases 9 players.

## Max Bid Formula

Remaining Purse − (100 × remaining vacant slots after this purchase)

---

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

---

# 🚀 Current Status

## ✅ Completed

- Live Auction
- Silent Bid
- Tie Break
- Joker System
- Last Strike
- Call SOLD
- Bounty
- Golden Bounty
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

---

# 🔮 Future Enhancements

- Authentication (JWT)
- PostgreSQL Support
- AWS Deployment
- Auction Analytics
- Export Auction Results
- Backup & Restore
- Live TV Display Mode
- Real-time updates using WebSockets
