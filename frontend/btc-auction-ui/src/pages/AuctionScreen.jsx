import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import axios from "axios";

import { API_URL } from "../config";
import CasinoReel from "../components/CasinoReel";
import EventOverlay from "../components/EventOverlay";

const lastScreenEventStorageKey = "btc-auction:last-screen-event-id";
const lastSilentWinnerStorageKey = "btc-auction:last-silent-winner-event-id";
const lastValueBetWinnerStorageKey = "btc-auction:last-value-bet-winner-event-id";

function AuctionScreen() {

    const [auction, setAuction] =
        useState(null);

    const [events, setEvents] =
        useState([]);

    const [silentBidActive, setSilentBidActive] =
        useState(false);

    const [silentBids, setSilentBids] =
        useState([]);

    const [latestEvent, setLatestEvent] =
        useState(null);

    const [showSilentWinner, setShowSilentWinner] =
        useState(false);

    const [silentWinner, setSilentWinner] =
        useState(null);

    const [valueBetWinner, setValueBetWinner] =
        useState(null);

    const lastProcessedEventId =
        useRef(window.localStorage.getItem(lastSilentWinnerStorageKey));

    const lastScreenEventId =
        useRef(window.localStorage.getItem(lastScreenEventStorageKey));

    const lastValueBetWinnerEventId =
        useRef(window.localStorage.getItem(lastValueBetWinnerStorageKey));

    const previousAuctionPhase = useRef(null);

    const valueBetWinnerTimer =
        useRef(null);

    const [auctionStatus, setAuctionStatus] =
        useState(null);

    const [isConnected, setIsConnected] =
        useState(false);

    const [eventPulse, setEventPulse] =
        useState(false);

    const [valueBetPrediction, setValueBetPrediction] =
        useState("");

    const [team, setTeam] =
        useState(null);

    const [availablePlayers, setAvailablePlayers] =
        useState([]);

    const role =
        localStorage.getItem("role");

    const username =
        localStorage.getItem("username");



    useEffect(() => {

        const loadData = () => {

            axios
                .get(
                    `${API_URL}/api/auction/status`
                )
                .then(response => {

                    setAuctionStatus(
                        response.data
                    );

                });

            axios
                .get(
                    `${API_URL}/api/auction/current`
                )
                .then(response => {

                    setAuction(
                        response.data
                    );

                });

            axios
                .get(`${API_URL}/api/teams`)
                .then(response => {
                    setTeam(response.data.find(item => item.captainName === username) || null);
                });

            axios
                .get(`${API_URL}/api/players/available`)
                .then(response => setAvailablePlayers(response.data));

            axios
                .get(
                    `${API_URL}/api/events`
                )
                .then(response => {

                    const data =
                        response.data;

                    setEvents(
                        data
                            .slice()
                            .reverse()
                            .slice(0, 10)
                    );

                    if (
                        data.length > 0
                    ) {

                        const latest =
                            data[data.length - 1];

                        setLatestEvent(latest);

                        if (String(latest.id) !== String(lastScreenEventId.current)) {
                            lastScreenEventId.current = String(latest.id);
                            window.localStorage.setItem(lastScreenEventStorageKey, String(latest.id));
                            setEventPulse(true);
                            window.setTimeout(() => setEventPulse(false), 700);

                            if (["SOLD", "PLAYER_SOLD", "LAST_STRIKE"].includes(latest.eventType)) {
                                new Audio("/sounds/last-strike.mp3").play().catch(() => { });
                            }

                            if (latest.eventType === "PLAYER_SOLD") {
                                window.scrollTo({ top: 0, behavior: "smooth" });
                            }
                        }

                        if (
                            latest &&
                            latest.eventType === "SILENT_BID_SOLD" &&
                            String(latest.id) !== String(lastProcessedEventId.current)
                        ) {

                            lastProcessedEventId.current =
                                String(latest.id);
                            window.localStorage.setItem(lastSilentWinnerStorageKey, String(latest.id));

                            setSilentWinner(latest);

                            setShowSilentWinner(true);

                            setTimeout(() => {

                                setShowSilentWinner(false);

                                setSilentWinner(null);

                            }, 5000);

                        }

                        if (
                            latest &&
                            latest.eventType === "VALUE_BET_REWARD" &&
                            String(latest.id) !== String(lastValueBetWinnerEventId.current)
                        ) {

                            lastValueBetWinnerEventId.current =
                                String(latest.id);
                            window.localStorage.setItem(lastValueBetWinnerStorageKey, String(latest.id));

                            setValueBetWinner(latest);

                            window.clearTimeout(valueBetWinnerTimer.current);

                            valueBetWinnerTimer.current = window.setTimeout(() => {

                                setValueBetWinner(null);

                            }, 5000);

                        }
                    }

                });

            axios
                .get(
                    `${API_URL}/api/silent-bid/active`
                )
                .then(response => {
                    const active = response.data;
                    setSilentBidActive(active);
                    if (active) {
                        axios.get(`${API_URL}/api/silent-bid/all`)
                            .then(res => setSilentBids(res.data || []))
                            .catch(() => setSilentBids([]));
                    } else {
                        setSilentBids([]);
                    }
                })
                .catch(() => {
                    setSilentBidActive(false);
                    setSilentBids([]);
                });

        };

        loadData();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000
        });

        client.onConnect = () => {

            setIsConnected(true);

            client.subscribe(
                "/topic/auction",
                () => {

                    loadData();

                }
            );

        };

        client.onDisconnect = () => setIsConnected(false);
        client.onWebSocketClose = () => setIsConnected(false);

        client.activate();

        return () => {

            window.clearTimeout(valueBetWinnerTimer.current);
            client.deactivate();

        };

    }, [username]);

    useEffect(() => {
        const enteredSoldPhase = auctionStatus?.auctionPhase === "SOLD"
            && previousAuctionPhase.current !== "SOLD";

        if (enteredSoldPhase) {
            window.scrollTo({ top: 0, behavior: "smooth" });
        }

        previousAuctionPhase.current = auctionStatus?.auctionPhase;
    }, [auctionStatus?.auctionPhase]);

    const useWildPick = async () => {
        if (!window.confirm("Activate Wildcard? This cancels every submitted blind opening bid and returns the player to the nomination lot.")) {
            return;
        }

        const response = await axios.post(`${API_URL}/api/auction/wild-pick`, null, {
            withCredentials: true
        });
        window.dispatchEvent(new CustomEvent("auction-toast", {
            detail: { type: "success", message: response.data }
        }));
    };

    const submitValueBet = async () => {
        if (!valueBetPrediction || Number(valueBetPrediction) <= 0) {
            return;
        }

        const response = await axios.post(`${API_URL}/api/value-bet/submit`, {
            playerName: auction?.currentPlayer,
            captainName: username,
            predictedPrice: Number(valueBetPrediction)
        });
        window.dispatchEvent(new CustomEvent("auction-toast", {
            detail: { type: "success", message: response.data }
        }));
        setValueBetPrediction("");
    };

    if (!auction) {

        return (

            <div className="app-container">

                Loading...

            </div>

        );

    }

    const waitingForLastStrike =
        auctionStatus &&
        auctionStatus.auctionPhase === "SOLD";

    const isWheelSpinning =
        auctionStatus?.auctionPhase === "SPINNING";

    const valueBetActive =
        role === "CAPTAIN"
        && ["OPENING_BID", "BLIND_OPENING_BID"].includes(auctionStatus?.auctionPhase)
        && auctionStatus?.valueBetPlayer?.toLowerCase() === auction.currentPlayer?.toLowerCase()
        && auction.currentPlayer;

    const reelPlayers = availablePlayers.filter(player => !player.sold);

    return (
        <>
            <EventOverlay event={valueBetWinner} />
            {showSilentWinner &&
                silentWinner && (

                    <div className="event-overlay silent">

                        <div className="event-card">

                            <div
                                style={{
                                    fontSize: "100px"
                                }}
                            >

                                🏆

                            </div>

                            <div
                                className="event-title"
                            >

                                SILENT BID COMPLETE

                            </div>

                            <div
                                style={{
                                    fontSize: "54px",
                                    marginTop: "25px",
                                    fontWeight: "bold"
                                }}
                            >

                                {silentWinner.playerName}

                            </div>

                            <div
                                style={{
                                    fontSize: "34px",
                                    marginTop: "20px"
                                }}
                            >

                                SOLD TO

                            </div>

                            <div
                                className="event-captain"
                            >

                                {silentWinner.captainName}

                            </div>

                            <div
                                style={{
                                    fontSize: "48px",
                                    marginTop: "25px"
                                }}
                            >

                                ₹{silentWinner.amount}

                            </div>

                        </div>

                    </div>

                )}
            <div className="app-container">

                <div className="page-header">

                    <h1 className="page-title">

                        {silentBidActive

                            ? "🔒 SILENT BID ROUND"

                            : "🏏 LIVE AUCTION"}

                    </h1>

                    <div className={isConnected ? "connection-status connected" : "connection-status"}>
                        <span className="connection-dot" />
                        {isConnected ? "Live scoreboard connected" : "Reconnecting to scoreboard..."}
                    </div>

                </div>

                <div className="auction-phase-bar">
                    <span className="auction-phase-label">{auctionStatus?.auctionPhase || "WAITING"}</span>
                    <span>{isWheelSpinning ? "Reel spinning" : silentBidActive ? "Confidential bids in progress" : waitingForLastStrike ? "RTM / last-strike decision window" : "Bidding is open"}</span>
                </div>

                {!silentBidActive &&
                    !waitingForLastStrike &&
                    latestEvent && (

                        <div
                            className="form-card"
                            style={{
                                marginBottom: "20px"
                            }}
                        >

                            <h2>

                                {latestEvent.eventType ===
                                    "TARGET_ACHIEVED" &&
                                    "🎯 TARGET ACHIEVED"}

                                {latestEvent.eventType ===
                                    "ALL_TARGETS_ACHIEVED" &&
                                    "🥇 ALL TARGETS ACHIEVED"}
                                {latestEvent.eventType === "SOLD" &&
                                    "🔨 SOLD"}



                            </h2>

                            <p>
                                {latestEvent.playerName}
                            </p>

                            <p>

                                {(latestEvent.eventType === "SOLD" || latestEvent.eventType === "PLAYER_SOLD") && (
                                    <>
                                        Final price: ₹{latestEvent.amount}
                                        <br />
                                    </>
                                )}

                                {latestEvent.details}

                            </p>

                        </div>

                    )}

                <div className={`current-auction-card ${eventPulse ? "event-pulse" : ""}`}>

                    <div className="auction-metrics" aria-label="Current auction metrics">
                        <div>
                            <span>Base price</span>
                            <strong>₹{auction.basePrice}</strong>
                        </div>
                        <div>
                            <span>Current bid</span>
                            <strong>₹{auction.currentBid}</strong>
                        </div>
                        <div>
                            <span>Leader</span>
                            <strong>{auction.leader || "No bids"}</strong>
                        </div>
                    </div>

                    <div
                        className="current-player-name"
                        style={{
                            fontSize: waitingForLastStrike
                                ? "72px"
                                : undefined,
                            fontWeight: waitingForLastStrike
                                ? "900"
                                : undefined,
                            marginBottom: waitingForLastStrike
                                ? "15px"
                                : undefined
                        }}
                    >

                        {isWheelSpinning
                            ? "REEL SPINNING..."
                            : auction.currentPlayer ||
                            "No Player Nominated"}

                    </div>

                    <div className="current-player-seed">

                        {isWheelSpinning ? "-" : auction.seed || "-"}

                    </div>

                    <CasinoReel
                        players={reelPlayers}
                        selectedPlayer={auction.currentPlayer}
                        spinStartedAt={auctionStatus?.wheelSpinStartedAt}
                        spinEndsAt={auctionStatus?.wheelSpinEndsAt}
                        className="viewer-casino-reel"
                        seasonName={auctionStatus?.seasonName}
                    />

                    {role === "CAPTAIN" &&
                        auctionStatus?.auctionPhase === "OPENING_BID" &&
                        auction.currentPlayer && (
                            <div style={{ marginTop: "20px", display: "grid", gap: "10px", justifyItems: "center" }}>
                                <button
                                    className="button-secondary"
                                    type="button"
                                    onClick={useWildPick}
                                    disabled={team?.wildPickUsed}
                                >
                                    {team?.wildPickUsed ? "Wild Pick Used" : "Return Player with Wild Pick"}
                                </button>
                            </div>
                        )}

                    {valueBetActive && (
                        <div style={{ marginTop: "20px", display: "flex", gap: "8px", maxWidth: "320px", width: "100%" }}>
                            <input
                                className="input"
                                type="number"
                                min="1"
                                step={Number(valueBetPrediction) > 1000 ? 100 : 50}
                                placeholder="Predict final price"
                                value={valueBetPrediction}
                                onChange={event => setValueBetPrediction(event.target.value)}
                            />
                            <button className="button" type="button" onClick={submitValueBet}>
                                Submit Value Bet
                            </button>
                        </div>
                    )}

                    {!silentBidActive && (

                        <>

                            {waitingForLastStrike ? (

                                <div
                                    className="message-success"
                                    style={{
                                        marginTop: "35px",
                                        textAlign: "center",
                                        padding: "30px"
                                    }}
                                >

                                    <div
                                        style={{
                                            fontSize: "90px",
                                            marginBottom: "20px",
                                            animation: "pulse 1s infinite"
                                        }}
                                    >

                                        🔨

                                    </div>

                                    <div
                                        style={{
                                            fontSize: "72px",
                                            fontWeight: "900",
                                            color: "#FFD700",
                                            letterSpacing: "6px"
                                        }}
                                    >

                                        SOLD

                                    </div>

                                    <div
                                        style={{
                                            marginTop: "35px",
                                            fontSize: "64px",
                                            fontWeight: "bold"
                                        }}
                                    >

                                        ₹{auction.currentBid}

                                    </div>

                                    <div
                                        style={{
                                            marginTop: "25px",
                                            fontSize: "36px",
                                            fontWeight: "bold"
                                        }}
                                    >

                                        🏆 {auction.leader}

                                    </div>

                                    <div
                                        style={{
                                            marginTop: "40px",
                                            fontSize: "32px",
                                            color: "#FFD700",
                                            animation: "pulse 1.2s infinite"
                                        }}
                                    >

                                        ⚡ Waiting for Last Strike / RTM...

                                    </div>

                                </div>

                            ) : (

                                <>

                                </>

                            )}

                        </>

                    )}

                    {silentBidActive && (

                        <div
                            className="message-success"
                            style={{
                                marginTop: "30px",
                                fontSize: "20px",
                                textAlign: "center",
                                lineHeight: "1.6"
                            }}
                        >

                            <div style={{ fontSize: "28px", fontWeight: "bold", marginBottom: "12px" }}>
                                🔒 SECRET BIDDING IN PROGRESS
                            </div>

                            <p style={{ margin: "8px 0 16px 0", fontSize: "18px" }}>
                                Confidential bids for <strong>{silentBids[0]?.playerName || auction.currentPlayer}</strong>
                            </p>

                            <table className="standings-table" style={{ width: "100%", margin: "0 auto", textAlign: "left" }}>
                                <thead>
                                    <tr>
                                        <th>Player</th>
                                        <th>Captain</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {silentBids.map(bid => (
                                        <tr key={bid.id || bid.captainName}>
                                            <td>{bid.playerName || silentBids[0]?.playerName || auction.currentPlayer}</td>
                                            <td>{bid.captainName}</td>
                                            <td>{bid.submitted ? "✅ Submitted" : "⌛ Waiting"}</td>
                                        </tr>
                                    ))}
                                    {silentBids.length === 0 && (
                                        <tr>
                                            <td colSpan="3" style={{ textAlign: "center", padding: "10px" }}>
                                                Waiting for captains to submit bids...
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>

                        </div>

                    )}

                </div>

                {!silentBidActive &&
                    !waitingForLastStrike && (

                        <div
                            className="form-card"
                            style={{
                                marginTop: "20px"
                            }}
                        >

                            <h2>

                                Bid & Event History

                            </h2>

                            {events.length === 0 && (

                                <p>

                                    No events yet

                                </p>

                            )}

                            {events.map(event => (

                                <div
                                    key={event.id}
                                    style={{
                                        padding: "10px",
                                        borderBottom:
                                            "1px solid #ddd"
                                    }}
                                >

                                    <strong>

                                        {event.eventType === "BOUNTY" && "🎁 "}
                                        {event.eventType === "GOLDEN_BOUNTY" && "🏆 "}
                                        {event.eventType === "TARGET_ACHIEVED" && "🎯 "}
                                        {event.eventType === "ALL_TARGETS_ACHIEVED" && "🥇 "}
                                        {event.eventType === "SOLD" && "🔨 "}

                                        {
                                            event.eventType === "SOLD"
                                                ? "SOLD"
                                                : event.eventType
                                        }

                                    </strong>

                                    {(
                                        <>
                                            <div>
                                                {event.playerName}
                                                {" → "}
                                                {event.captainName}
                                            </div>

                                            {(event.eventType === "SOLD" || event.eventType === "PLAYER_SOLD") && (
                                                <div>Final price: ₹{event.amount}</div>
                                            )}

                                            <div>{event.details}</div>
                                        </>
                                    )}

                                </div>

                            ))}

                        </div>

                    )}

            </div>
        </>
    );

}

export default AuctionScreen;