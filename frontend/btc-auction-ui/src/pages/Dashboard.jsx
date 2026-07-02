import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import LiveActivity from "../components/LiveActivity";

function Dashboard() {

    const [dashboard, setDashboard] =
        useState(null);

    const [events, setEvents] =
        useState([]);

    const [rtm, setRtm] =

        useState(null);

    const [rtmBid, setRtmBid] =
        useState("");

    const role =
        localStorage.getItem("role");

    const username =
        localStorage.getItem("username");

    const isAdmin =
        role === "ADMIN";

    const isCaptain =
        role === "CAPTAIN";


    const isRtmCaptain =
        rtm &&
        rtm.captainName === username;

    const isOriginalCaptain =
        rtm &&
        rtm.originalCaptain === username;

    const waitingForDecision =
        rtm &&
        rtm.status === "BID_SUBMITTED";

    const currentTeam =
        dashboard?.teams?.find(
            team => team.captainName === username
        );

    const rtmAlreadyUsed =
        currentTeam ? !currentTeam.rtmAvailable : false;

    const [auctionStatus, setAuctionStatus] =
        useState(null);







    useEffect(() => {

        loadDashboard();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
        });

        client.onConnect = () => {

            client.subscribe(
                "/topic/auction",
                () => {

                    loadDashboard();

                }
            );

        };

        client.activate();

        return () => {

            client.deactivate();

        };

    }, []);


    const loadDashboard = () => {

        fetch(`${API_URL}/api/dashboard`)
            .then(r => r.json())
            .then(setDashboard);

        fetch(`${API_URL}/api/events`)
            .then(r => r.json())
            .then(data =>
                setEvents(
                    data.slice().reverse().slice(0, 20)
                ));

        fetch(`${API_URL}/api/auction/status`)
            .then(r => r.json())
            .then(setAuctionStatus);

        fetch(`${API_URL}/api/rtm/current`)
            .then(async r => {

                if (!r.ok) {

                    setRtm(null);

                    return;
                }

                setRtm(await r.json());

            })
            .catch(() => setRtm(null));

    };

    const claimRtm = async () => {

        if (navigator.vibrate) {

            navigator.vibrate(300);

        }

        const audio = new Audio("/sounds/veto.mp3");

        audio.play();

        const response = await fetch(
            `${API_URL}/api/rtm/claim`,
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body: JSON.stringify({

                    captainName:
                        username

                })
            }
        );

        const text = await response.text();

        alert(text);

        loadDashboard();


    };

    const submitRtmBid = async () => {

        const response = await fetch(

            `${API_URL}/api/rtm/bid`,

            {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    bidAmount: Number(rtmBid)

                })

            }

        );

        alert(await response.text());

        loadDashboard();

    };

    const acceptRtm = async () => {

        const response = await fetch(

            `${API_URL}/api/rtm/accept`,

            {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    captainName: username

                })

            }

        );

        alert(await response.text());

        loadDashboard();

    };

    const declineRtm = async () => {

        const response = await fetch(

            `${API_URL}/api/rtm/decline`,

            {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    captainName: username

                })

            }

        );

        alert(await response.text());

        loadDashboard();

    };

    if (!dashboard) {

        return (
            <div>
                Loading...
            </div>
        );
    }

    const specialEvents =
        events.filter(event =>
            [
                "BOUNTY",
                "GOLDEN_BOUNTY",
                "REVERSE_TARGET_TRIGGERED",
                "CAPTAIN_TRIBUNAL",
                "RTM_TRIGGERED",
                "RTM_BID_SUBMITTED",
                "RTM_ACCEPTED",
                "RTM_DECLINED"
            ].includes(event.eventType)
        );

    return (

        <div>

            <div className="section-card current-auction">

                <h2>
                    🎤 Current Auction
                </h2>

                <p>
                    <strong>Player:</strong>{" "}
                    {dashboard.currentAuction.currentPlayer || "None"}
                </p>

                <p>
                    <strong>Seed:</strong>{" "}
                    {dashboard.currentAuction.seed || "-"}
                </p>

                <p>
                    <strong>Current Bid:</strong>{" "}
                    ₹{dashboard.currentAuction.currentBid}
                </p>

                <p>
                    <strong>Leading Captain:</strong>{" "}
                    {dashboard.currentAuction.leader}
                </p>

            </div>

            <div className="section-card">

                <h2>
                    🏆 Team Standings
                </h2>

                <div className="team-grid">

                    {dashboard.teams?.map(team => (

                        <div
                            key={team.captainName}
                            className="team-card"
                        >

                            <h2>
                                {team.captainName}
                            </h2>

                            <p>
                                💰 Purse: ₹{team.purse}
                            </p>
                            <p>
                                🔨 Max Bid: ₹{team.maxBid}
                            </p>
                            <p>
                                🔄 RTM:
                                {team.rtmAvailable
                                    ? " 🟢 Available"
                                    : " 🔴 Used"}
                            </p>
                            <p>
                                👥 Players Bought: {team.playersBought}
                            </p>





                            <div className="squad-title">
                                Squad ({team.squad?.length || 0})
                            </div>

                            {team.squad &&
                                team.squad.length > 0 ? (

                                <ul>

                                    {team.squad.map(player => (

                                        <li key={player}>
                                            {player}
                                        </li>

                                    ))}

                                </ul>

                            ) : (

                                <div className="empty-squad">
                                    No players bought yet
                                </div>

                            )}

                        </div>

                    ))}

                </div>

            </div>
            {isCaptain &&
                (auctionStatus?.auctionPhase === "SOLD" || rtm) && (

                    <div className="section-card">

                        <h2>
                            🔄 Right To Match
                        </h2>

                        <p>

                            {

                                auctionStatus?.auctionPhase === "SOLD"

                                    ? "🟢 RTM Window Open"

                                    : "🔒 Waiting for Auctioneer to call SOLD"

                            }

                        </p>

                        {!rtm && (

                            <button
                                className={
                                    auctionStatus?.auctionPhase === "SOLD"
                                        ? "button"
                                        : "button-secondary"
                                }
                                disabled={
                                    auctionStatus?.auctionPhase !== "SOLD" ||
                                    rtmAlreadyUsed
                                }
                                onClick={claimRtm}
                            >

                                {rtmAlreadyUsed ? "RTM ALREADY USED" : "CLAIM RTM"}

                            </button>

                        )}

                        {isRtmCaptain &&
                            rtm?.status === "CLAIMED" && (

                                <div
                                    style={{ marginTop: 20 }}
                                >

                                    <h3>

                                        Enter RTM Bid

                                    </h3>

                                    <input
                                        className="input"
                                        type="number"
                                        min={dashboard.currentAuction.currentBid + 100}
                                        max={
                                            dashboard.teams.find(
                                                t => t.captainName === username
                                            )?.maxBid
                                        }
                                        className="input"
                                        type="number"
                                        value={rtmBid}
                                        onChange={(e) =>
                                            setRtmBid(
                                                e.target.value
                                            )
                                        }
                                    />

                                    <button
                                        className="button"
                                        disabled={
                                            !rtmBid ||
                                            Number(rtmBid) <= dashboard.currentAuction.currentBid ||
                                            Number(rtmBid) >
                                            dashboard.teams.find(
                                                t => t.captainName === username
                                            )?.maxBid
                                        }
                                        className="button"
                                        style={{
                                            marginTop: 15
                                        }}
                                        onClick={submitRtmBid}
                                    >

                                        Submit RTM Bid

                                    </button>

                                </div>

                            )}

                        {isOriginalCaptain &&
                            rtm?.status === "BID_SUBMITTED" && (

                                <div style={{ marginTop: 20 }}>

                                    <h3>
                                        🔄 RTM Decision
                                    </h3>

                                    <p>

                                        Original Bid:
                                        <strong> ₹{dashboard.currentAuction.currentBid}</strong>

                                    </p>

                                    <p>

                                        RTM Bid:
                                        <strong> ₹{rtm.bidAmount}</strong>

                                    </p>

                                    <button
                                        className="button"
                                        style={{ marginRight: 10 }}
                                        onClick={acceptRtm}
                                    >
                                        ✅ Accept
                                    </button>

                                    <button
                                        className="button-secondary"
                                        onClick={declineRtm}
                                    >
                                        ❌ Decline
                                    </button>

                                </div>

                            )}

                        {waitingForDecision &&
                            !isOriginalCaptain &&
                            !isRtmCaptain && (

                                <div
                                    className="message-success"
                                    style={{ marginTop: 20 }}
                                >

                                    ⏳ Waiting for
                                    {" "}
                                    <strong>{rtm.originalCaptain}</strong>
                                    {" "}
                                    to accept or decline the RTM bid.

                                </div>

                            )}

                    </div>

                )}

            <div className="section-card">

                <h2>
                    📢 Special Events
                </h2>

                {specialEvents.length === 0 && (

                    <p>
                        No special events yet
                    </p>

                )}

                {specialEvents.map(event => (

                    <div
                        key={event.id}
                        className="activity-item"
                    >

                        <strong>

                            {event.eventType ===
                                "BOUNTY" &&
                                "🎁 BOUNTY"}

                            {event.eventType ===
                                "GOLDEN_BOUNTY" &&
                                "🏆 GOLDEN BOUNTY"}

                            {event.eventType ===
                                "REVERSE_TARGET_TRIGGERED" &&
                                "🎯 Reverse Target"}

                            {event.eventType ===
                                "CAPTAIN_TRIBUNAL" &&
                                "⚖️ CAPTAIN TRIBUNAL"}

                            {event.eventType === "RTM_TRIGGERED" &&
                                "🔄 RTM ACTIVATED"}

                            {event.eventType === "RTM_BID_SUBMITTED" &&
                                "📝 RTM BID SUBMITTED"}

                            {event.eventType === "RTM_ACCEPTED" &&
                                "✅ RTM ACCEPTED"}

                            {event.eventType === "RTM_DECLINED" &&
                                "❌ RTM DECLINED"}

                        </strong>

                        <div>

                            {event.eventType === "REVERSE_TARGET_TRIGGERED"
                                ? event.playerName
                                : (
                                    <>
                                        {event.playerName}
                                        {event.captainName &&
                                            ` → ${event.captainName}`}
                                    </>
                                )
                            }

                        </div>

                        <div>
                            {event.details}
                        </div>

                        {event.amount > 0 && (

                            <div>
                                ₹{event.amount}
                            </div>

                        )}

                    </div>

                ))}

            </div>

            <LiveActivity />

        </div>

    );
}

export default Dashboard;