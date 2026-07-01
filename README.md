# 🏏 BTC Auction Manager – Season X

A full-featured web-based cricket auction management platform built for **BTC Reloaded – Season X**.

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

## Joker System

Each captain receives one random secret Joker.

- 🛑 VETO
- ⚡ LAST STRIKE
- 🚫 BID BLOCK (Manual)
- 🎯 STEAL BID (Manual)

## Tournament Rules

- Forbidden Pick
- Trusted Captain
- Tribunal Vote
- Bounty Player
- Golden Bounty
- Squad Tracking
- Remaining Purse
- Maximum Bid Calculation

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

# 🏆 Season X Captains

| Captain | Starting Purse |
|---------|---------------:|
| Joy | 10000 |
| Rimo | 10000 |
| Sujay | 9400 |
| Asgorath Monarch Dragleeo | 9200 |

---

# 📐 Auction Rules

## Squad Size

10 Players per Team (including Captain)

## Max Bid Formula

### ICON

Remaining Purse − (200 × Remaining ICON Slots)

### STAR / CHALLENGER / PRO / FOUNDATION / EMERGING

Remaining Purse − (100 × Remaining Player Slots)

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