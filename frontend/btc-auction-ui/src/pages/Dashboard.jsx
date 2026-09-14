import { useEffect, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import LiveActivity from "../components/LiveActivity";
import CasinoReel from "../components/CasinoReel";
import { showToast } from "../services/toast";

const RANDOM_EVENT_BANTER = {
    MARKET_BOOM: "The price elevator has skipped the safety briefing.",
    MARKET_CRASH: "The price has found a banana peel. Brace for the landing.",
    RTM_LOCKDOWN: "The RTM gatekeeper has misplaced the key.",
    SILENT_AUCTION: "Shhh. Even the calculator is whispering.",
    VALUE_BET: "Dust off the crystal ball and trust your oddly specific hunch.",
};

function Dashboard() {

    const [dashboard, setDashboard] =
        useState(null);

    const [events, setEvents] =
        useState([]);

    const [rtm, setRtm] =

        useState(null);

    const [rtmBid, setRtmBid] =
        useState("");

    const [silentBidActive, setSilentBidActive] =
        useState(false);

    const [silentRound, setSilentRound] =
        useState(null);

    const [allSilentBids, setAllSilentBids] =
        useState([]);

    const [silentBid, setSilentBid] =
        useState("");

    const [openingBid, setOpeningBid] =
        useState("");

    const [openingBidStatus, setOpeningBidStatus] =
        useState(null);

    const [liveBid, setLiveBid] =
        useState("");

    const [valueBetPrediction, setValueBetPrediction] =
        useState("");

    const [dashboardView, setDashboardView] =
        useState("overview");

    const role =
        localStorage.getItem("role");

    const username =
        localStorage.getItem("username");

    const isAdmin =
        role === "ADMIN";

    const isCaptain =
        role === "CAPTAIN";

    const isViewer =
        role === "VIEWER";


    const isRtmCaptain =
        rtm &&
        rtm.captainName === username;

    const isOriginalCaptain =
        rtm &&
        rtm.originalCaptain === username;

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

    const [, setCaptains] =
        useState([]);

    const [teamQuery, setTeamQuery] =
        useState("");

    const [playerQuery, setPlayerQuery] =
        useState("");

    const [playerFilter, setPlayerFilter] =
        useState("ALL");

    const [playerCategory, setPlayerCategory] =
        useState("ALL");

    const [protectionPlayers, setProtectionPlayers] =
        useState([]);

    const [newProtectionPlayer, setNewProtectionPlayer] =
        useState("");

    const loadDashboard = useCallback(() => {
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
            fetch(`${API_URL}/api/protection-players/${encodeURIComponent(username)}`)
                .then(r => r.json())
                .then(setProtectionPlayers)
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

        fetch(`${API_URL}/api/silent-bid/active`)
            .then(response => response.json())
            .then(active => {
                setSilentBidActive(active);
                if (active) {
                    fetch(`${API_URL}/api/silent-bid/all`)
                        .then(r => r.json())
                        .then(data => setAllSilentBids(data || []))
                        .catch(() => setAllSilentBids([]));

                    if (isCaptain) {
                        return fetch(`${API_URL}/api/silent-bid/${encodeURIComponent(username)}`)
                            .then(response => response.json())
                            .then(setSilentRound);
                    }
                } else {
                    setAllSilentBids([]);
                }
                setSilentRound(null);
                return null;
            })
            .catch(() => {
                setSilentBidActive(false);
                setSilentRound(null);
                setAllSilentBids([]);
            });

        fetch(`${API_URL}/api/auction/status`)
            .then(response => response.json())
            .then(status => {
                if (isCaptain && status.auctionPhase === "BLIND_OPENING_BID") {
                    return fetch(`${API_URL}/api/auction/current`)
                        .then(response => response.json())
                        .then(current => current?.currentPlayer
                            ? fetch(`${API_URL}/api/auction/blind-opening-bid/${encodeURIComponent(current.currentPlayer)}/${encodeURIComponent(username)}`)
                                .then(response => response.json())
                                .then(setOpeningBidStatus)
                            : setOpeningBidStatus(null));
                }
                setOpeningBidStatus(null);
                return null;
            })
            .catch(() => setOpeningBidStatus(null));
    }, [isCaptain, username]);

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
    }, [loadDashboard]);

    const submitProtectionPlayer = async () => {
        const response = await fetch(`${API_URL}/api/protection-players`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ captainName: username, playerName: newProtectionPlayer })
        });
        showToast(await response.text());
        setNewProtectionPlayer("");
        loadDashboard();
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

    const submitLiveBid = async () => {
        if (!liveBid || Number(liveBid) <= 0) return;
        const response = await fetch(`${API_URL}/api/auction/update-current`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ captainName: username, currentBid: Number(liveBid) })
        });
        showToast(await response.text());
        setLiveBid("");
        loadDashboard();
    };

    const submitValueBet = async () => {
        if (!valueBetPrediction || Number(valueBetPrediction) <= 0) return;
        const response = await fetch(`${API_URL}/api/value-bet/submit`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                playerName: dashboard.currentAuction.currentPlayer,
                captainName: username,
                predictedPrice: Number(valueBetPrediction)
            })
        });
        showToast(await response.text());
        setValueBetPrediction("");
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

    const submitSilentBid = async () => {
        if (!silentRound || !silentBid || Number(silentBid) <= 0) return;
        const response = await fetch(`${API_URL}/api/silent-bid/submit`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                playerName: silentRound.playerName,
                captainName: username,
                bidAmount: Number(silentBid)
            })
        });
        const result = await response.text();
        const bidAccepted = result === "Bid submitted.";
        showToast(result, bidAccepted ? "success" : "error");
        if (bidAccepted) {
            setSilentBid("");
        }
        loadDashboard();
    };

    const submitOpeningBid = async () => {
        if (!openingBid || Number(openingBid) <= 0 || !dashboard.currentAuction.currentPlayer) return;
        const response = await fetch(`${API_URL}/api/auction/blind-opening-bid/submit`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                playerName: dashboard.currentAuction.currentPlayer,
                captainName: username,
                bidAmount: Number(openingBid)
            })
        });
        showToast(await response.text());
        setOpeningBid("");
        loadDashboard();
    };

    const activateWildcard = async () => {
        if (!window.confirm("Activate Wildcard? This cancels every submitted blind opening bid and returns the player to the nomination lot.")) {
            return;
        }

        const response = await fetch(`${API_URL}/api/auction/wild-pick`, {
            method: "POST",
            credentials: "include"
        });
        const message = await response.text();
        const activated = message.includes("activated Wildcard");

        showToast(message, activated ? "success" : "error");
        if (activated) {
            setOpeningBid("");
        }
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
                "CAPTAIN_TRIBUNAL",
                "RTM_TRIGGERED",
                "RTM_BID_SUBMITTED",
                "RTM_ACCEPTED",
                "RTM_DECLINED",
                "PROTECTION_REVEALED",
                "VALUE_BET_REWARD",
                "STARTING_BID_WINNER",
                "WILDCARD_TRIGGERED"
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

    const currentAuction = dashboard.currentAuction;
    const rtmLocked = auctionStatus?.rtmLockdownPlayer?.toLowerCase()
        === currentAuction.currentPlayer?.toLowerCase();
    const effectiveMaxBid = currentTeam?.maxBid || 0;
    const isBidding = ["OPENING_BID", "BIDDING"].includes(auctionStatus?.auctionPhase);
    const activeRandomEventType = auctionStatus?.pendingRandomEventType;
    const activeRandomEventDescription = auctionStatus?.pendingRandomEventDescription
        || "A surprise auction rule is in play.";
    const showPublicRandomEvent = activeRandomEventType
        && activeRandomEventType !== "VALUE_BET";

    return (

        <div>

            {isCaptain && (
                <div className="button-group captain-tabs" style={{ marginBottom: "18px" }}>
                    <button className={dashboardView === "overview" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("overview")}>Current Auction</button>
                    <button className={dashboardView === "standings" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("standings")}>Team Standing</button>
                    <button className={dashboardView === "players" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("players")}>Player Intelligence</button>
                </div>
            )}

            {showPublicRandomEvent && (
                <section className="live-random-event-banner" role="status" aria-live="polite">
                    <span className="live-random-event-live">LIVE</span>
                    <div className="live-random-event-copy">
                        <p className="live-random-event-kicker">Random event in play</p>
                        <h2>{activeRandomEventType.replaceAll("_", " ")} is live</h2>
                        <p className="live-random-event-description">{activeRandomEventDescription}</p>
                        <p className="live-random-event-banter">
                            Event desk: {RANDOM_EVENT_BANTER[activeRandomEventType]
                                || "The auction rulebook has started improvising."}
                        </p>
                        {auctionStatus?.pendingRandomEventPlayer && (
                            <p className="live-random-event-player">
                                Affecting: <strong>{auctionStatus.pendingRandomEventPlayer}</strong>
                            </p>
                        )}
                    </div>
                </section>
            )}

            {isCaptain && dashboardView === "overview" && (
                <div className="section-card captain-actions" style={{ marginBottom: "18px" }}>
                    <h2>Auction Actions</h2>
                    {silentBidActive && silentRound && !silentRound.submitted && (
                        <div className="captain-action-row silent-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            <input className="input" type="number" min="1"
                                step={Number(silentBid) > 1000 ? 100 : 50}
                                placeholder="Secret silent bid"
                                value={silentBid} onChange={event => setSilentBid(event.target.value)} />
                            <button className="button" onClick={submitSilentBid}>Submit Silent Bid</button>
                        </div>
                    )}
                    {silentBidActive && silentRound?.submitted && (
                        <p className="message-success">Silent bid submitted. Waiting for the other captains.</p>
                    )}
                    {isBidding && auctionStatus?.auctionPhase !== "SOLD" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            <input className="input" type="number" min="1" max={effectiveMaxBid}
                                step={Number(liveBid) > 1000 ? 100 : 50}
                                placeholder={`Start bid (max ₹${effectiveMaxBid})`}
                                value={liveBid} onChange={event => setLiveBid(event.target.value)} />
                            <button className="button" onClick={submitLiveBid}>Place Bid</button>
                        </div>
                    )}
                    {auctionStatus?.auctionPhase === "BLIND_OPENING_BID" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            {openingBidStatus && !openingBidStatus.submitted && (
                                <>
                                    <input className="input" type="number" min="1"
                                        data-testid="blind-opening-bid"
                                        step={Number(openingBid) > 1000 ? 100 : 50}
                                        placeholder="Opening bid"
                                        value={openingBid}
                                        onChange={event => setOpeningBid(event.target.value)} />
                                    <button className="button" data-testid="place-blind-opening-bid" onClick={submitOpeningBid}>Place Blind Opening Bid</button>
                                </>
                            )}
                            {currentTeam && !currentTeam.wildPickUsed && (
                                <button className="button-secondary" type="button" data-testid="activate-wildcard" onClick={activateWildcard}>
                                    Activate Wildcard
                                </button>
                            )}
                        </div>
                    )}
                    {auctionStatus?.auctionPhase === "BLIND_OPENING_BID"
                        && openingBidStatus?.submitted && (
                            <p className="message-success">Opening bid submitted. Waiting for all captains.</p>
                        )}
                    {["OPENING_BID", "BLIND_OPENING_BID"].includes(auctionStatus?.auctionPhase)
                        && auctionStatus?.valueBetPlayer?.toLowerCase() === currentAuction.currentPlayer?.toLowerCase()
                        && currentAuction.currentPlayer && (
                            <div className="captain-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                                <input className="input" type="number" min="1"
                                    step={Number(valueBetPrediction) > 1000 ? 100 : 50}
                                    placeholder="Predict final price"
                                    value={valueBetPrediction}
                                    onChange={event => setValueBetPrediction(event.target.value)} />
                                <button className="button-secondary" onClick={submitValueBet}>Submit Value Bet</button>
                            </div>
                        )}
                    {auctionStatus?.auctionPhase === "SOLD" && !isCurrentHighestBidder && !rtmLocked && !rtm && !rtmAlreadyUsed && (
                        <button className="button" onClick={claimRtm}>Claim RTM</button>
                    )}
                    {rtmLocked && <p>RTM is locked for this player.</p>}
                    {isRtmCaptain && rtm?.status === "CLAIMED" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px" }}>
                            <input className="input" type="number" min={currentAuction.currentBid + 100}
                                max={effectiveMaxBid} value={rtmBid}
                                onChange={event => setRtmBid(event.target.value)} placeholder="RTM price" />
                            <button className="button" onClick={submitRtmBid}>Submit RTM Price</button>
                        </div>
                    )}
                    {isOriginalCaptain && rtm?.status === "BID_SUBMITTED" && (
                        <div>
                            <p>RTM price: <strong>₹{rtm.bidAmount}</strong></p>
                            <button className="button" onClick={acceptRtm}>Accept</button>
                            <button className="button-secondary" onClick={declineRtm} style={{ marginLeft: "8px" }}>Reject</button>
                        </div>
                    )}
                </div>
            )}

            {silentBidActive && allSilentBids.length > 0 && (
                <div className="section-card silent-incoming-card" style={{ marginBottom: "18px" }}>
                    <h2>🔒 Silent Bid Incoming Bids — {allSilentBids[0]?.playerName || currentAuction.currentPlayer}</h2>
                    <div className="silent-bid-matrix-wrap">
                        <table className="standings-table silent-bid-matrix">
                            <thead>
                                <tr>
                                    <th scope="col">Player</th>
                                    {allSilentBids.map(bid => (
                                        <th key={bid.id || bid.captainName} scope="col">{bid.captainName}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td className="silent-bid-player">
                                        {allSilentBids[0]?.playerName || currentAuction.currentPlayer}
                                    </td>
                                    {allSilentBids.map(bid => (
                                        <td key={bid.id || bid.captainName} className="silent-bid-cell">
                                            <span className={bid.submitted ? "silent-bid-status submitted" : "silent-bid-status"}>
                                                {bid.submitted ? "Submitted" : "Waiting"}
                                            </span>
                                        </td>
                                    ))}
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {(dashboardView === "overview" || (!isCaptain && !isAdmin)) && <div className="section-card current-auction">

                <h2>
                    🎤 Current Auction
                </h2>

                <CasinoReel
                    players={players.filter(player => !player.sold)}
                    selectedPlayer={currentAuction.currentPlayer}
                    spinStartedAt={auctionStatus?.wheelSpinStartedAt}
                    spinEndsAt={auctionStatus?.wheelSpinEndsAt}
                    className="captain-casino-reel"
                    seasonName={auctionStatus?.seasonName}
                />

                <p style={{ marginTop: "16px" }}>
                    <strong>Current Bid:</strong>{" "}
                    ₹{dashboard.currentAuction.currentBid}
                </p>

            </div>}

            {(dashboardView === "standings" || !isCaptain) && <div className="section-card">

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

            </div>}

            {(dashboardView === "players" || (!isCaptain && !isViewer)) && <div className="section-card">
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
            </div>}
            {auctionStatus?.auctionRound === 3 && (
                <div className="section-card">
                    <h2>🔄 Unsold Players After Re-auction</h2>
                    <p>These players remain unsold. Admin can sell them manually.</p>
                    {players.filter(player => !player.sold).length > 0 ? (
                        <div className="player-directory-wrap">
                            <table className="standings-table player-directory-table">
                                <thead>
                                    <tr>
                                        <th>Player</th>
                                        <th>Category</th>
                                        <th>Original Base Price</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {players.filter(player => !player.sold).map(player => (
                                        <tr key={`unsold-${player.id || player.name}`}>
                                            <td><strong>{player.name}</strong></td>
                                            <td>{player.category || player.seed || "Uncategorized"}</td>
                                            <td>₹{player.basePrice}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <p className="table-empty">No players remain unsold.</p>
                    )}
                </div>
            )}
            {isCaptain && auctionStatus?.protectionSelectionEnabled && !auctionStatus?.auctionStarted && (

                <div className="section-card">
                    <h2>Player Protection</h2>
                    <p>Select exactly 2 protection players before the auction starts.</p>
                    {protectionPlayers.map(protection => (
                        <div key={protection.id}>{protection.playerName}</div>
                    ))}
                    {protectionPlayers.length < 2 && (
                        <>
                            <select className="input" value={newProtectionPlayer}
                                onChange={event => setNewProtectionPlayer(event.target.value)}>
                                <option value="">Select protection player</option>
                                {players.filter(player => !protectionPlayers.some(item => item.playerName === player.name))
                                    .map(player => <option key={player.id} value={player.name}>{player.name}</option>)}
                            </select>
                            <button className="button" onClick={submitProtectionPlayer}
                                disabled={!newProtectionPlayer}>
                                Save Protection Player
                            </button>
                        </>
                    )}
                </div>

            )}

            {(dashboardView === "overview" || !isCaptain) && <div className="event-columns">
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

                                {event.eventType === "VALUE_BET_REWARD" &&
                                    "💰 VALUE BET WINNERS"}

                                {event.eventType ===
                                    "BOUNTY" &&
                                    "🎁 BOUNTY"}

                                {event.eventType ===
                                    "GOLDEN_BOUNTY" &&
                                    "🏆 GOLDEN BOUNTY"}

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

                                {event.eventType === "WILDCARD_TRIGGERED" &&
                                    "🃏 WILDCARD TRIGGERED"}

                            </strong>

                            {event.eventType === "VALUE_BET_REWARD" ? (

                                <div>{event.captainName}</div>

                            ) : (

                                <>

                                    <div>

                                        {event.playerName}
                                        {event.captainName && ` → ${event.captainName}`}

                                    </div>

                                    <div>
                                        {event.details}
                                    </div>

                                    {event.amount !== 0 && (

                                        <div>
                                            ₹{event.amount > 0 ? "+" : ""}{event.amount}
                                        </div>

                                    )}

                                </>

                            )}

                        </div>

                    ))}

                </div>

                <div className="event-panel">
                    <LiveActivity />
                </div>
            </div>}

        </div>

    );
}

export default Dashboard;