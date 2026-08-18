import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import LiveActivity from "../components/LiveActivity";
import { showToast } from "../services/toast";

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

    const isCurrentHighestBidder =
        dashboard?.currentAuction?.leader?.toLowerCase() ===
        username?.toLowerCase();

    const [auctionStatus, setAuctionStatus] =
        useState(null);

    const [players, setPlayers] =
        useState([]);

    const [captains, setCaptains] =
        useState([]);

    const [teamQuery, setTeamQuery] =
        useState("");

    const [playerQuery, setPlayerQuery] =
        useState("");

    const [playerFilter, setPlayerFilter] =
        useState("ALL");

    const [playerCategory, setPlayerCategory] =
        useState("ALL");

    const [secretTargets, setSecretTargets] =
        useState(null);

    const [reverseTargets, setReverseTargets] =
        useState(null);

    const [newSecretTargets, setNewSecretTargets] =
        useState({ captainName: username, playerOne: "", playerTwo: "" });

    const [newReverseTarget, setNewReverseTarget] =
        useState({ captainName: username, rivalCaptain: "", playerName: "" });







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

        fetch(`${API_URL}/api/players`)
            .then(r => r.json())
            .then(setPlayers);

        fetch(`${API_URL}/api/teams`)
            .then(r => r.json())
            .then(setCaptains);

        if (isCaptain) {
            fetch(`${API_URL}/api/secret-targets/${username}`)
                .then(r => r.ok ? r.json() : null)
                .then(setSecretTargets)
                .catch(() => { });

            fetch(`${API_URL}/api/reverse-target/${username}`)
                .then(r => r.ok ? r.json() : null)
                .then(setReverseTargets)
                .catch(() => { });
        }

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

        showToast(text);

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

        showToast(await response.text());

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

        showToast(await response.text());

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

        showToast(await response.text());

        loadDashboard();

    };

    const submitSecretTargets = async () => {
        const response = await fetch(`${API_URL}/api/secret-targets`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(newSecretTargets)
        });
        const result = await response.text();
        showToast(result);
        loadDashboard();
    };

    const submitReverseTarget = async () => {
        const response = await fetch(`${API_URL}/api/reverse-target`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(newReverseTarget)
        });
        const result = await response.text();
        showToast(result);
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
                "SECRET_TARGET_SETTLED",
                "REVERSE_TARGET_TRIGGERED",
                "CAPTAIN_TRIBUNAL",
                "RTM_TRIGGERED",
                "RTM_BID_SUBMITTED",
                "RTM_ACCEPTED",
                "RTM_DECLINED"
            ].includes(event.eventType)
        );

    const categories = [
        "ALL",
        ...new Set(
            players
                .map(player => player.category || player.seed || "UNCATEGORIZED")
        )
    ];

    const filteredTeams = dashboard.teams?.filter(team =>
        team.captainName.toLowerCase().includes(teamQuery.toLowerCase())
    ) || [];

    const filteredPlayers = players.filter(player => {
        const matchesQuery = player.name.toLowerCase().includes(playerQuery.toLowerCase());
        const matchesStatus = playerFilter === "ALL"
            || (playerFilter === "AVAILABLE" && !player.sold)
            || (playerFilter === "SOLD" && player.sold);
        const category = player.category || player.seed || "UNCATEGORIZED";
        const matchesCategory = playerCategory === "ALL"
            || category === playerCategory;

        return matchesQuery && matchesStatus && matchesCategory;
    });

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
                    <strong>Nominated By:</strong>{" "}
                    {dashboard.currentAuction.nominatedBy || "None"}
                </p>

            </div>

            {isCaptain &&
                (rtm ||
                    (auctionStatus?.auctionPhase === "SOLD" &&
                        !isCurrentHighestBidder)) && (

                    <div className="section-card">

                        <h2>
                            🔄 Right To Match
                        </h2>

                        <p>
                            {auctionStatus?.auctionPhase === "SOLD"
                                ? "🟢 RTM Window Open"
                                : "🔒 Waiting for Auctioneer to call SOLD"}
                        </p>

                        {!rtm && !isCurrentHighestBidder && (

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

                        {isRtmCaptain && rtm?.status === "CLAIMED" && (

                            <div style={{ marginTop: 20 }}>

                                <h3>Enter RTM Bid</h3>

                                <input
                                    className="input"
                                    type="number"
                                    min={dashboard.currentAuction.currentBid + 100}
                                    max={dashboard.teams.find(
                                        team => team.captainName === username
                                    )?.maxBid}
                                    value={rtmBid}
                                    onChange={event => setRtmBid(event.target.value)}
                                />

                                <button
                                    className="button"
                                    disabled={
                                        !rtmBid ||
                                        Number(rtmBid) <= dashboard.currentAuction.currentBid ||
                                        Number(rtmBid) > dashboard.teams.find(
                                            team => team.captainName === username
                                        )?.maxBid
                                    }
                                    style={{ marginTop: 15 }}
                                    onClick={submitRtmBid}
                                >
                                    Submit RTM Bid
                                </button>

                            </div>

                        )}

                        {isOriginalCaptain && rtm?.status === "BID_SUBMITTED" && (

                            <div style={{ marginTop: 20 }}>

                                <h3>🔄 RTM Decision</h3>

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

                                <button className="button-secondary" onClick={declineRtm}>
                                    ❌ Decline
                                </button>

                            </div>

                        )}

                        {waitingForDecision && !isOriginalCaptain && !isRtmCaptain && (

                            <div className="message-success" style={{ marginTop: 20 }}>
                                ⏳ Waiting for <strong>{rtm.originalCaptain}</strong> to accept or decline the RTM bid.
                            </div>

                        )}

                    </div>

                )}

            <div className="section-card">

                <h2>
                    🏆 Team Standings
                </h2>

                <div className="intelligence-toolbar">
                    <input
                        className="input"
                        placeholder="Search teams"
                        value={teamQuery}
                        onChange={event => setTeamQuery(event.target.value)}
                    />
                </div>

                <div className="team-grid">

                    {filteredTeams.map(team => (

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
                            <p>
                                📊 Squad Value: ₹{players
                                    .filter(player => player.team === team.captainName)
                                    .reduce((total, player) => total + (player.finalPrice || player.soldPrice || 0), 0)}
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

                <div className="comparison-table-wrap">
                    <h3>Team Comparison</h3>
                    <table className="standings-table">
                        <thead>
                            <tr>
                                <th>Team</th>
                                <th>Purse</th>
                                <th>Max bid</th>
                                <th>Slots left</th>
                                <th>Squad value</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredTeams.map(team => (
                                <tr key={`comparison-${team.captainName}`}>
                                    <td>{team.captainName}</td>
                                    <td>₹{team.purse}</td>
                                    <td>₹{team.maxBid}</td>
                                    <td>{team.playersLeft}</td>
                                    <td>₹{players
                                        .filter(player => player.team === team.captainName)
                                        .reduce((total, player) => total + (player.finalPrice || player.soldPrice || 0), 0)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

            </div>

            <div className="section-card">
                <h2>🧭 Player Intelligence</h2>
                <div className="intelligence-toolbar player-toolbar">
                    <input
                        className="input"
                        placeholder="Search players"
                        value={playerQuery}
                        onChange={event => setPlayerQuery(event.target.value)}
                    />
                    <select className="select" value={playerFilter} onChange={event => setPlayerFilter(event.target.value)}>
                        <option value="ALL">All statuses</option>
                        <option value="AVAILABLE">Available</option>
                        <option value="SOLD">Sold</option>
                    </select>
                    <select className="select" value={playerCategory} onChange={event => setPlayerCategory(event.target.value)}>
                        {categories.map(category => (
                            <option key={category} value={category}>
                                {category === "ALL" ? "All categories" : category}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="player-directory-wrap">
                    <table className="standings-table player-directory-table">
                        <thead>
                            <tr>
                                <th>Player</th>
                                <th>Category</th>
                                <th>Status</th>
                                <th>Price</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredPlayers.slice(0, 24).map(player => (
                                <tr key={player.id || player.name}>
                                    <td><strong>{player.name}</strong></td>
                                    <td>
                                        <span className="category-badge">
                                            {player.category || player.seed || "Uncategorized"}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={player.sold ? "status-badge sold" : "status-badge available"}>
                                            {player.sold ? "Sold" : "Available"}
                                        </span>
                                    </td>
                                    <td>{player.sold
                                        ? `Final ₹${player.finalPrice || player.soldPrice}`
                                        : `Base ₹${player.basePrice}`}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    {filteredPlayers.length === 0 && <p className="table-empty">No players match these filters.</p>}
                    {filteredPlayers.length > 24 && <p className="table-caption">Showing 24 of {filteredPlayers.length} players</p>}
                </div>
            </div>
            {isCaptain && !auctionStatus?.auctionStarted && (

                <div className="section-card">

                    <h2>🎯 Secret Targets — Double Down</h2>

                    {secretTargets ? (
                        <div>
                            <div className="message-success">
                                ✅ Targets locked: <strong>{secretTargets.playerOne}</strong> and <strong>{secretTargets.playerTwo}</strong>
                            </div>
                            <p style={{ marginTop: "15px", fontSize: "0.95em" }}>
                                ℹ️ Your targets will be settled at the end of the auction.
                                <br />First target bought: +₹150 | Second target bought: +₹250
                                <br />Each missed target: −₹100
                            </p>
                        </div>
                    ) : (
                        <div>
                            <p style={{ marginBottom: "15px" }}>First target bought: +₹150; second target bought: +₹250; each missed target: −₹100.</p>
                            {["playerOne", "playerTwo"].map((field, index) => (
                                <div className="form-field" key={field}>
                                    <label>Target Player {index + 1}</label>
                                    <select className="input" value={newSecretTargets[field]}
                                        onChange={event => setNewSecretTargets({ ...newSecretTargets, [field]: event.target.value })}>
                                        <option value="">Select Player</option>
                                        {players.map(player => <option key={player.id} value={player.name}>{player.name}</option>)}
                                    </select>
                                </div>
                            ))}
                            <button className="button" onClick={submitSecretTargets}
                                disabled={!newSecretTargets.playerOne || !newSecretTargets.playerTwo}>
                                Lock Secret Targets
                            </button>
                        </div>
                    )}

                </div>

            )}

            {isCaptain && !auctionStatus?.auctionStarted && (

                <div className="section-card">

                    <h2>🎯 Reverse Target</h2>

                    {reverseTargets ? (
                        <div>
                            <div className="message-success">
                                ✅ Target submitted: <strong>{reverseTargets.playerName}</strong>
                                <br />Rival Captain: <strong>{reverseTargets.rivalCaptain}</strong>
                            </div>
                            <p style={{ marginTop: "15px", fontSize: "0.95em" }}>
                                ℹ️ If {reverseTargets.rivalCaptain} buys {reverseTargets.playerName}, they lose ₹200.
                            </p>
                        </div>
                    ) : (
                        <div>
                            <p style={{ marginBottom: "15px" }}>If the selected rival captain buys this player, ₹200 is immediately deducted from their purse.</p>
                            <div className="form-field">
                                <label>Rival Captain</label>
                                <select className="input" value={newReverseTarget.rivalCaptain}
                                    onChange={event => setNewReverseTarget({ ...newReverseTarget, rivalCaptain: event.target.value })}>
                                    <option value="">Select Rival</option>
                                    {captains.filter(c => c.captainName !== username).map(captain => (
                                        <option key={captain.captainName} value={captain.captainName}>{captain.captainName}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-field">
                                <label>Targeted Player</label>
                                <select className="input" value={newReverseTarget.playerName}
                                    onChange={event => setNewReverseTarget({ ...newReverseTarget, playerName: event.target.value })}>
                                    <option value="">Select Player</option>
                                    {players.map(player => <option key={player.id} value={player.name}>{player.name}</option>)}
                                </select>
                            </div>
                            <button className="button" onClick={submitReverseTarget}
                                disabled={!newReverseTarget.rivalCaptain || !newReverseTarget.playerName}>
                                🎯 Submit Reverse Target
                            </button>
                        </div>
                    )}

                </div>

            )}

            <div className="event-columns">
                <div className="section-card event-panel">

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
                                    "SECRET_TARGET_SETTLED" &&
                                    "🎯 Secret Target"}

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

                            {event.amount !== 0 && (

                                <div>
                                    ₹{event.amount > 0 ? "+" : ""}{event.amount}
                                </div>

                            )}

                        </div>

                    ))}

                </div>

                <div className="event-panel">
                    <LiveActivity />
                </div>
            </div>

        </div>

    );
}

export default Dashboard;