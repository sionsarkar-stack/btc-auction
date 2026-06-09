# 🏏 BTC Auction Manager – Season 9

A web-based auction management platform built for BTC Season 9.

## Features

* Player Nomination
* Live Auction Tracking
* Sell Player Workflow
* Team Purse Management
* Squad Tracking
* Max Bid Calculation
* Recent Sales Log
* Undo Last Sale
* Live Dashboard

## Tech Stack

### Frontend

* React
* Vite
* CSS

### Backend

* Java 21
* Spring Boot
* Maven

## Auction Rules

### Squad Size

* 10 Players per Team (including captain)

### Captains & Purse

| Captain  | Purse |
| -------- | ----: |
| Dinda    |  5000 |
| Boni     |  6300 |
| Swapneel |  6200 |
| Swaswata |  6200 |

### Max Bid Formula

#### Z Seed

Max Bid = Remaining Purse - (200 × (Players Left - 1))

#### A/B/C Seed

Max Bid = Remaining Purse - (100 × (Players Left - 1))

## Running Locally

### Backend

```bash
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8080
```

### Frontend

```bash
npm install
npm run dev
```

Runs on:

```text
http://localhost:5173
```

## Current Status

Season 9 MVP Complete

### Completed

* Dashboard
* Nominate Player
* Sell Player
* Recent Sales
* Undo Sale
* Squad Tracking
* Max Bid Calculation

### Planned

* H2 Database
* AWS Deployment
* Export Results
* Backup / Restore
* Auction Configuration Screen
