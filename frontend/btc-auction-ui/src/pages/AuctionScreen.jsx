import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import axios from "axios";

import { API_URL } from "../config";

function AuctionScreen() {

    const [auction, setAuction] =
        useState(null);

    const [events, setEvents] =
        useState([]);

    const [silentBidActive, setSilentBidActive] =
        useState(false);

    const [latestEvent, setLatestEvent] =
        useState(null);

    const [showSilentWinner, setShowSilentWinner] =
        useState(false);

    const [silentWinner, setSilentWinner] =
        useState(null);

    const lastProcessedEventId =
        useRef(null);

    const [auctionStatus, setAuctionStatus] =
        useState(null);



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

                        if (
                            latest &&
                            latest.eventType === "SILENT_BID_SOLD" &&
                            latest.id !== lastProcessedEventId.current
                        ) {

                            lastProcessedEventId.current =
                                latest.id;

                            setSilentWinner(latest);

                            setShowSilentWinner(true);

                            setTimeout(() => {

                                setShowSilentWinner(false);

                                setSilentWinner(null);

                            }, 5000);

                        }
                    }

                });

            axios
                .get(
                    `${API_URL}/api/silent-bid/active`
                )
                .then(response => {

                    setSilentBidActive(
                        response.data
                    );

                });

        };

        loadData();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000
        });

        client.onConnect = () => {

            client.subscribe(
                "/topic/auction",
                () => {

                    loadData();

                }
            );

        };

        client.activate();

        return () => {

            client.deactivate();

        };

    }, []);

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

    return (
        <>
            {showSilentWinner &&
                silentWinner && (

                    <div className="joker-overlay silent">

                        <div className="joker-card">

                            <div
                                style={{
                                    fontSize: "100px"
                                }}
                            >

                                🏆

                            </div>

                            <div
                                className="joker-title"
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
                                className="joker-captain"
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
                                    "BOUNTY" &&
                                    "🎁 BOUNTY REVEALED"}

                                {latestEvent.eventType ===
                                    "GOLDEN_BOUNTY" &&
                                    "🏆 GOLDEN BOUNTY"}

                                {latestEvent.eventType ===
                                    "TARGET_ACHIEVED" &&
                                    "🎯 TARGET ACHIEVED"}

                                {latestEvent.eventType ===
                                    "ALL_TARGETS_ACHIEVED" &&
                                    "🥇 ALL TARGETS ACHIEVED"}
                                {latestEvent.eventType === "REVERSE_TARGET_TRIGGERED" &&
                                    "🎯 Reverse Target"}
                                {latestEvent.eventType === "JOKER_USED" &&
                                    "🃏 JOKER ACTIVATED"}

                                {latestEvent.eventType === "PLAYER_VETOED" &&
                                    "❌ NOMINATION VETOED"}

                                {latestEvent.eventType === "LAST_STRIKE" &&
                                    "⚡ LAST STRIKE"}

                                {latestEvent.eventType === "SOLD" &&
                                    "🔨 SOLD"}



                            </h2>

                            <p>
                                {latestEvent.eventType === "REVERSE_TARGET_TRIGGERED"
                                    ? latestEvent.details
                                    : latestEvent.playerName}
                            </p>

                            <p>

                                {latestEvent.eventType === "BOUNTY" &&
                                    latestEvent.amount > 0 && (

                                        <>
                                            Reward : ₹{latestEvent.amount}
                                            <br />
                                        </>

                                    )}

                                {latestEvent.eventType === "GOLDEN_BOUNTY" &&
                                    latestEvent.amount > 0 && (

                                        <>
                                            Reward : ₹{latestEvent.amount}
                                            <br />
                                        </>

                                    )}

                                {latestEvent.eventType === "REVERSE_TARGET_TRIGGERED" && (

                                    <>
                                        {latestEvent.amount < 0
                                            ? `Penalty : ₹${Math.abs(latestEvent.amount)}`
                                            : `Reward : ₹${latestEvent.amount}`}
                                        <br />
                                    </>

                                )}

                                {latestEvent.eventType !== "REVERSE_TARGET_TRIGGERED" &&
                                    latestEvent.details}

                            </p>

                        </div>

                    )}

                <div className="current-auction-card">

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

                        {auction.currentPlayer ||
                            "No Player Nominated"}

                    </div>

                    <div className="current-player-seed">

                        {auction.seed || "-"}

                    </div>

                    <p>

                        Base Price:
                        ₹{auction.basePrice}

                    </p>

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

                                    <p>

                                        Current Bid:
                                        ₹{auction.currentBid}

                                    </p>

                                    <p>

                                        Leader:
                                        {auction.leader}

                                    </p>

                                </>

                            )}

                        </>

                    )}

                    {silentBidActive && (

                        <div
                            className="message-success"
                            style={{
                                marginTop: "30px",
                                fontSize: "30px",
                                fontWeight: "bold",
                                textAlign: "center",
                                lineHeight: "1.8"
                            }}
                        >

                            🔒 SECRET BIDDING IN PROGRESS

                            <br /><br />

                            All Captains are placing

                            <br />

                            their confidential bids.

                            <br /><br />

                            ⏳ Please Wait...

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

                                Recent Events

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
                                        {event.eventType === "REVERSE_TARGET_TRIGGERED" && "🎯 "}
                                        {event.eventType === "JOKER_USED" && "🃏 "}
                                        {event.eventType === "PLAYER_VETOED" && "❌ "}
                                        {event.eventType === "LAST_STRIKE" && "⚡ "}
                                        {event.eventType === "SOLD" && "🔨 "}

                                        {
                                            event.eventType === "JOKER_USED"
                                                ? "JOKER ACTIVATED"
                                                : event.eventType === "PLAYER_VETOED"
                                                    ? "NOMINATION VETOED"
                                                    : event.eventType === "LAST_STRIKE"
                                                        ? "LAST STRIKE"
                                                        : event.eventType === "SOLD"
                                                            ? "SOLD"
                                                            : event.eventType === "REVERSE_TARGET_TRIGGERED"
                                                                ? "Reverse Target"
                                                                : event.eventType
                                        }

                                    </strong>

                                    {event.eventType === "REVERSE_TARGET_TRIGGERED" ? (

                                        <>
                                            <div>{event.details}</div>

                                            <div style={{ color: event.amount < 0 ? "#b91c1c" : "green", fontWeight: "bold" }}>
                                                {event.amount < 0
                                                    ? `Penalty : ₹${Math.abs(event.amount)}`
                                                    : `Reward : ₹${event.amount}`}
                                            </div>
                                        </>

                                    ) : (

                                        <>
                                            <div>
                                                {event.playerName}
                                                {" → "}
                                                {event.captainName}
                                            </div>

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