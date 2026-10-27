import { useEffect, useRef, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import SilentBidManager from "./SilentBidManager";
import PlayerWalkout from "../components/PlayerWalkout";
import LiveActivity from "../components/LiveActivity";
import ActionFeedback from "../services/ActionFeedback";
import { useActionRequest } from "../services/useActionRequest";
import { apiRequest } from "../services/apiClient";

function AuctionManager() {

    const [teams, setTeams] = useState([]);
    const [players, setPlayers] = useState([]);
    const [currentAuction, setCurrentAuction] = useState(null);

    const [playerName, setPlayerName] = useState("");
    const [captainName, setCaptainName] = useState("");
    const [soldPrice, setSoldPrice] = useState("");
    const [callSoldPlayer, setCallSoldPlayer] = useState(null);

    const [message, setMessage] = useState("");
    const [isManagerDataReady, setIsManagerDataReady] = useState(false);
    const [isManagerDataLoading, setIsManagerDataLoading] = useState(true);
    const [managerDataError, setManagerDataError] = useState("");

    const [clubbedPairs, setClubbedPairs] = useState([]);
    const [randomEvents, setRandomEvents] = useState([]);
    const [pairPlayerOne, setPairPlayerOne] = useState("");
    const [pairPlayerTwo, setPairPlayerTwo] = useState("");
    const [isConnected, setIsConnected] = useState(false);
    const [isBusy, setIsBusy] = useState(false);
    const {
        acknowledgeOutcome,
        feedback: actionFeedback,
        outcomeUnknown,
        pendingAction,
        runAction,
    } = useActionRequest();
    const actionBusy = isBusy || Boolean(pendingAction);
    const [auctionStatus, setAuctionStatus] = useState(null);
    const [allCaptainsPassed, setAllCaptainsPassed] = useState(false);
    const [openingBidTie, setOpeningBidTie] = useState(null);
    const [openingBids, setOpeningBids] = useState([]);
    const [curseSelectionCounts, setCurseSelectionCounts] = useState({});
    const previousNominatedPlayer = useRef(null);

    const loadData = useCallback(async () => {
        setIsManagerDataLoading(true);
        try {
            const [teamsData, playersData, currentAuction, statusData, curseReadiness, clubbedPairData, randomEventsData, eventsData] = await Promise.all([
                apiRequest("/api/teams"),
                apiRequest("/api/players/available"),
                apiRequest("/api/auction/current"),
                apiRequest("/api/auction/status"),
                apiRequest("/api/curse-players/readiness"),
                apiRequest("/api/auction/clubbed-pair"),
                apiRequest("/api/random-events"),
                apiRequest("/api/events"),
            ]);

            if (!Array.isArray(teamsData) || !Array.isArray(playersData) || !Array.isArray(eventsData)) {
                throw new Error("The server returned invalid captain, player, or event data. Retry the refresh.");
            }

            const nominatedPlayer = currentAuction?.currentPlayer;
            const [allPassed, currentOpeningBids] = statusData?.auctionPhase === "BLIND_OPENING_BID" && nominatedPlayer
                ? await Promise.all([
                    apiRequest(`/api/auction/blind-opening-bid/${encodeURIComponent(nominatedPlayer)}/all-passed`),
                    apiRequest(`/api/auction/blind-opening-bid/${encodeURIComponent(nominatedPlayer)}`),
                ])
                : [false, []];

            const latestEvent = eventsData.length > 0
                ? eventsData[eventsData.length - 1]
                : null;
            const latestOpeningBidTie = latestEvent?.eventType === "STARTING_BID_TIE"
                && latestEvent.playerName === nominatedPlayer
                ? latestEvent
                : null;

            const hasNominatedPlayer = playersData.some(
                player => player.name === nominatedPlayer
            );
            const sellablePlayers = nominatedPlayer && !hasNominatedPlayer
                ? [
                    ...playersData,
                    {
                        name: nominatedPlayer,
                        basePrice: currentAuction.basePrice
                    }
                ]
                : playersData;

            setTeams(teamsData);
            setPlayers(sellablePlayers);
            if (currentAuction?.currentPlayer !== previousNominatedPlayer.current) {
                previousNominatedPlayer.current = currentAuction?.currentPlayer || null;
                setPlayerName(currentAuction?.currentPlayer || "");
                setCallSoldPlayer(null);
            }
            setCurrentAuction(currentAuction);
            setAuctionStatus(statusData);
            setCurseSelectionCounts(curseReadiness || {});
            setAllCaptainsPassed(allPassed);
            setOpeningBidTie(latestOpeningBidTie);
            setOpeningBids(Array.isArray(currentOpeningBids) ? currentOpeningBids : []);
            setClubbedPairs(Array.isArray(clubbedPairData) ? clubbedPairData : []);
            setRandomEvents(randomEventsData);
            setManagerDataError("");
            setIsManagerDataReady(true);
            return true;
        } catch (error) {
            console.error(error);
            const errorMessage = error.message || "Unable to load auction data. Retry the refresh.";
            setManagerDataError(errorMessage);
            setIsManagerDataReady(false);
            setMessage(errorMessage);
            return false;
        } finally {
            setIsManagerDataLoading(false);
        }
    }, []);

    useEffect(() => {
        let mounted = true;
        const initData = async () => {
            if (mounted) {
                await loadData();
            }
        };
        initData();

        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => {
                if (mounted) setIsConnected(true);
                client.subscribe("/topic/auction", loadData);
            },
            onDisconnect: () => {
                if (mounted) setIsConnected(false);
            },
            onWebSocketClose: () => {
                if (mounted) setIsConnected(false);
            },
        });

        client.activate();

        return () => {
            mounted = false;
            client.deactivate();
        };
    }, [loadData]);

    useEffect(() => {
        if (auctionStatus?.auctionPhase !== "SPINNING") {
            return undefined;
        }

        const intervalId = window.setInterval(() => {
            void loadData();
        }, 500);
        return () => window.clearInterval(intervalId);
    }, [auctionStatus?.auctionPhase, loadData]);

    const refreshAndAcknowledgeOutcome = async () => {
        if (await loadData()) {
            acknowledgeOutcome();
        }
    };

    const callSold = async () => {

        if (isBusy || outcomeUnknown) {
            return;
        }

        if (
            !playerName ||
            !captainName ||
            !soldPrice
        ) {

            setMessage(
                "Please fill all fields."
            );

            return;

        }

        if (!window.confirm(`Call SOLD for ${playerName} to ${captainName} at ₹${soldPrice}?`)) {
            return;
        }

        const action = await runAction(
            "Calling SOLD",
            `${API_URL}/api/auction/call-sold`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ captainName, currentBid: Number(soldPrice) }),
            },
            result => result.includes("Waiting for RTM")
        );
        if (action.success) {
            setCallSoldPlayer(playerName);
            await loadData();
        }

    };

    const sellPlayer = async () => {

        if (isBusy || outcomeUnknown) {
            return;
        }

        if (
            !playerName ||
            !captainName ||
            !soldPrice
        ) {

            setMessage(
                "Please fill all fields."
            );

            return;
        }

        if (!window.confirm(`Confirm sale of ${playerName} to ${captainName} for ₹${soldPrice}?`)) {
            return;
        }

        const soldPlayer = playerName;
        const action = await runAction(
            "Confirming sale",
            `${API_URL}/api/auction/sold`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ playerName, captainName, soldPrice: Number(soldPrice) }),
            },
            result => result.startsWith(`${soldPlayer} sold to `)
        );

        if (action.success) {
            setPlayerName("");
            setCaptainName("");
            setSoldPrice("");
            await loadData();
        }
    };

    const undoSale = async () => {

        if (isBusy || outcomeUnknown) {
            return;
        }

        if (!window.confirm("Undo the last sale? This changes team and player records.")) {
            return;
        }


        const action = await runAction(
            "Undoing sale",
            `${API_URL}/api/auction/undo`,
            { method: "POST" },
            result => result === "Last sale undone"
        );
        if (action.success) {
            setCallSoldPlayer(null);
            await loadData();
        }
    };

    const startAuction = async () => {

        if (!window.confirm("Start the auction now? Players will be selected by the spinning wheel.")) {
            return;
        }

        try {
            setIsBusy(true);
            const response = await fetch(
                `${API_URL}/api/auction/start`,
                { method: "POST" }
            );

            setMessage(await response.text());
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to start auction.");
        } finally {
            setIsBusy(false);
        }

    };

    const spinWheel = async () => {
        try {
            setIsBusy(true);
            const response = await fetch(`${API_URL}/api/auction/spin-wheel`, { method: "POST" });
            setMessage(await response.text());
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to spin the wheel.");
        } finally {
            setIsBusy(false);
        }
    };

    const enableCurseSelection = async () => {
        setIsBusy(true);
        try {
            const response = await fetch(`${API_URL}/api/curse-players/selection/enable`, { method: "POST" });
            setMessage(await response.text());
            await loadData();
        } finally {
            setIsBusy(false);
        }
    };

    const autoSelectCursePlayers = async () => {
        if (!canAutoSelectCurses) {
            setMessage(autoSelectRequirement || "Add at least one captain and two available players first.");
            return;
        }

        setIsBusy(true);
        try {
            const result = await apiRequest("/api/curse-players/auto-select", { method: "POST" });
            setMessage(result);
            if (result === "Two curse players were auto-selected for every captain.") {
                await loadData();
            }
        } catch (error) {
            setMessage(error.message || "Unable to auto-select curse players.");
        } finally {
            setIsBusy(false);
        }
    };

    const startReAuction = async () => {
        if (!window.confirm("Restart the auction? All unsold players will return to the wheel at half their original base price.")) {
            return;
        }

        try {
            setIsBusy(true);
            const response = await fetch(`${API_URL}/api/auction/re-auction/start`, { method: "POST" });
            setMessage(await response.text());
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to start the re-auction.");
        } finally {
            setIsBusy(false);
        }
    };

    const cancelReAuction = async () => {
        if (!window.confirm("Cancel re-auction? Every sale made after restart will be rolled back and all round-one unsold players will return to the wheel.")) {
            return;
        }

        try {
            setIsBusy(true);
            const response = await fetch(`${API_URL}/api/auction/re-auction/cancel`, { method: "POST" });
            setMessage(await response.text());
            setPlayerName("");
            setCaptainName("");
            setSoldPrice("");
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to cancel the re-auction.");
        } finally {
            setIsBusy(false);
        }
    };

    const markUnsold = async () => {
        const nominatedPlayer = currentAuction?.currentPlayer;
        if (!nominatedPlayer) {
            setMessage("No player is currently nominated.");
            return;
        }

        if (!window.confirm(`Mark ${nominatedPlayer} unsold?`)) {
            return;
        }

        try {
            setIsBusy(true);
            const response = await fetch(`${API_URL}/api/auction/mark-unsold`, { method: "POST" });
            setMessage(await response.text());
            setPlayerName("");
            setCaptainName("");
            setSoldPrice("");
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to mark the player unsold.");
        } finally {
            setIsBusy(false);
        }
    };

    const saveClubbedPair = async () => {
        if (!pairPlayerOne || !pairPlayerTwo) {
            setMessage("Select one A-seeded player and one C-seeded player for the A + C pair.");
            return;
        }

        const response = await fetch(`${API_URL}/api/auction/clubbed-pair`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ playerOne: pairPlayerOne, playerTwo: pairPlayerTwo })
        });

        const text = await response.text();
        setMessage(text);
        await loadData();
    };

    const clearClubbedPair = async () => {
        const response = await fetch(`${API_URL}/api/auction/clubbed-pair`, { method: "DELETE" });
        const text = await response.text();
        setMessage(text);
        setPairPlayerOne("");
        setPairPlayerTwo("");
        await loadData();
    };

    const deleteClubbedPair = async (id) => {
        if (!id) return;
        const response = await fetch(`${API_URL}/api/auction/clubbed-pair/${id}`, { method: "DELETE" });
        const text = await response.text();
        setMessage(text);
        await loadData();
    };

    const wheelPlayers = players.filter(player =>
        !clubbedPairs.some(pair =>
            pair.playerTwo?.toLowerCase() === player.name?.toLowerCase()
        )
    );
    const isWheelSpinning = auctionStatus?.auctionPhase === "SPINNING";
    const cursesReady = auctionStatus?.curseSelectionEnabled
        && isManagerDataReady
        && teams.length > 0
        && teams.every(team => curseSelectionCounts[team.captainName] === 2);
    const messageIsError = /unable|cannot|required|at least|not found|failed|must/i.test(message);
    const availableCursePlayerCount = players.filter(player => !player.sold && !player.deferredToReAuction).length;
    const canAutoSelectCurses = isManagerDataReady
        && !isManagerDataLoading
        && teams.length > 0
        && availableCursePlayerCount >= 2;
    const autoSelectRequirements = [
        !isManagerDataReady && isManagerDataLoading ? "Checking captain and player data..." : null,
        isManagerDataReady && teams.length === 0 ? "Add at least one captain before bypassing curse selection." : null,
        isManagerDataReady && availableCursePlayerCount < 2 ? "Import at least two available players before bypassing curse selection." : null,
    ].filter(Boolean);
    const autoSelectRequirement = autoSelectRequirements.join(" ");

    return (
        <>
            <LiveActivity showActivity={false} />
            <div className="app-container">

                <div className="page-header">
                    <h1 className="page-title">
                        🏏 BTC AUCTION MANAGER
                    </h1>
                    <div className={isConnected ? "connection-status connected" : "connection-status"}>
                        <span className="connection-dot" />
                        {isConnected ? "Live updates connected" : "Connecting to live updates..."}
                    </div>
                </div>

                <div className="form-card">

                    <div className="button-group manager-actions" style={{ marginBottom: "16px" }}>
                        <button
                            className="button"
                            onClick={startAuction}
                            disabled={isBusy || !isManagerDataReady || !cursesReady}
                        >
                            🚀 Start Auction
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={enableCurseSelection}
                            disabled={isBusy || !isManagerDataReady || availableCursePlayerCount < 2 || auctionStatus?.curseSelectionEnabled || auctionStatus?.auctionStarted}
                        >
                            Enable Curse Selection
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={autoSelectCursePlayers}
                            disabled={actionBusy || !canAutoSelectCurses || auctionStatus?.auctionStarted}
                        >
                            Bypass: Auto-select Curses
                        </button>

                        {!auctionStatus?.auctionStarted && autoSelectRequirement && (
                            <p className={isManagerDataReady ? "message-error" : "form-hint"}
                                role={isManagerDataReady ? "alert" : "status"}>
                                {autoSelectRequirement}
                            </p>
                        )}

                        {managerDataError && (
                            <div className="message-error" role="alert">
                                <span>{managerDataError}</span>
                                <button className="button-secondary" type="button" onClick={loadData} disabled={isManagerDataLoading}>
                                    {isManagerDataLoading ? "Retrying..." : "Retry data refresh"}
                                </button>
                            </div>
                        )}

                        {cursesReady && (
                            <div className="message-success protection-ready-message">
                                All captains have secretly selected two Curse players.
                            </div>
                        )}

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={startReAuction}
                            disabled={isBusy || auctionStatus?.auctionRound !== 1 || auctionStatus?.auctionStarted}
                        >
                            🔄 Restart Auction
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={cancelReAuction}
                            disabled={isBusy || auctionStatus?.auctionRound !== 2}
                        >
                            ✖ Cancel Re-auction
                        </button>

                    </div>

                    {randomEvents.length > 0 && (
                        <div style={{ marginBottom: "20px", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "10px", background: "#f8fafc" }}>
                            <h3 style={{ margin: "0 0 12px" }}>Random Event Library</h3>
                            <div style={{ display: "grid", gap: "8px" }}>
                                {randomEvents.map((event) => {
                                    const isCurrentRandomEvent =
                                        auctionStatus?.pendingRandomEventType === event.type;
                                    const eventName = event.type === "RTM_LOCKDOWN"
                                        ? "NO MERCY"
                                        : event.type.replaceAll("_", " ");

                                    return (
                                        <div
                                            key={event.id}
                                            className={`random-event-library-item${isCurrentRandomEvent ? " is-active" : ""}`}
                                            aria-label={isCurrentRandomEvent ? `${eventName} is active` : undefined}
                                        >
                                            <div className="random-event-library-item-heading">
                                                <strong>{eventName}</strong>
                                                {isCurrentRandomEvent && (
                                                    <span className="random-event-active-badge">
                                                        Active now{auctionStatus?.pendingRandomEventPlayer
                                                            ? `: ${auctionStatus.pendingRandomEventPlayer}`
                                                            : ""}
                                                    </span>
                                                )}
                                            </div>
                                            <span className="random-event-library-description">
                                                {event.description}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    <div className="pair-config" style={{ marginBottom: "20px", display: "grid", gap: "12px", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "10px", background: "#f8fafc" }}>
                        <h3 style={{ margin: 0 }}>A + C Pair (Admin)</h3>
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(180px, 1fr))", gap: "12px" }}>
                            <select className="select" value={pairPlayerOne} onChange={(e) => setPairPlayerOne(e.target.value)}>
                                <option value="">Select A Player</option>
                                {players.filter(player => player.seed?.toUpperCase() === "A").map((player) => (
                                    <option key={`pair-one-${player.name}`} value={player.name}>{player.name}</option>
                                ))}
                            </select>
                            <select className="select" value={pairPlayerTwo} onChange={(e) => setPairPlayerTwo(e.target.value)}>
                                <option value="">Select C Player</option>
                                {players.filter(player => player.seed?.toUpperCase() === "C").map((player) => (
                                    <option key={`pair-two-${player.name}`} value={player.name}>{player.name}</option>
                                ))}
                            </select>
                        </div>
                        <div className="button-group">
                            <button className="button" type="button" onClick={saveClubbedPair} disabled={isBusy || !pairPlayerOne || !pairPlayerTwo}>
                                💼 Save A + C Pair
                            </button>
                            <button className="button-secondary" type="button" onClick={clearClubbedPair} disabled={isBusy || clubbedPairs.length === 0}>
                                🧹 Clear Pair
                            </button>
                        </div>
                        {clubbedPairs.length > 0 ? (
                            <div className="message-success" style={{ margin: 0, display: "grid", gap: "8px" }}>
                                {clubbedPairs.map(pair => (
                                    <div
                                        key={pair.id || `${pair.playerOne}-${pair.playerTwo}`}
                                        style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px" }}
                                    >
                                        <span>
                                            <strong>{pair.playerOne}</strong> + <strong>{pair.playerTwo}</strong>
                                        </span>
                                        {pair.id && (
                                            <button
                                                className="button-secondary"
                                                type="button"
                                                style={{ padding: "4px 10px", fontSize: "13px", minHeight: "unset", color: "#991b1b", borderColor: "#fecaca" }}
                                                onClick={() => deleteClubbedPair(pair.id)}
                                                disabled={isBusy}
                                            >
                                                🗑️ Delete
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="message-success" style={{ background: "#e2e8f0", color: "#0f172a", margin: 0 }}>
                                No A + E pair configured yet.
                            </div>
                        )}
                    </div>

                    <div className="wheel-layout" style={{ display: "grid", gridTemplateColumns: "minmax(280px, 360px) minmax(240px, 1fr) minmax(320px, 1.2fr)", gap: "20px", alignItems: "center", marginBottom: "24px" }}>
                        <div>
                            <button
                                className="button-secondary"
                                type="button"
                                onClick={spinWheel}
                                data-testid="spin-wheel"
                                disabled={actionBusy || outcomeUnknown || !auctionStatus?.auctionStarted || wheelPlayers.length === 0 || Boolean(currentAuction?.currentPlayer)}
                                title={currentAuction?.currentPlayer ? "Resolve the active player before spinning again." : undefined}
                                style={{ width: "100%", marginBottom: "14px" }}
                            >
                                Call Next Player
                            </button>
                            <PlayerWalkout
                                players={wheelPlayers}
                                walkoutPlayers={auctionStatus?.walkoutPlayerNames}
                                selectedPlayer={currentAuction?.currentPlayer}
                                spinStartedAt={auctionStatus?.wheelSpinStartedAt}
                                spinEndsAt={auctionStatus?.wheelSpinEndsAt}
                                serverTime={auctionStatus?.serverTime}
                                seasonName={auctionStatus?.seasonName}
                            />
                        </div>

                        <div>
                            <h3 style={{ margin: "0 0 10px" }}>{auctionStatus?.seasonName || "Auction"} Player Walkout</h3>
                            <p style={{ margin: "0 0 8px" }}>The stadium lights sweep the available roster before the selected player walks onto the pitch.</p>
                            <p style={{ fontSize: "13px", color: "#64748b", margin: 0 }}>
                                {wheelPlayers.length} player{wheelPlayers.length === 1 ? "" : "s"} waiting in the tunnel.
                            </p>
                        </div>

                        <div className="section-card" style={{ margin: 0 }}>
                            <h3 style={{ margin: "0 0 10px" }}>Opening Bids</h3>
                            {auctionStatus?.auctionPhase === "BLIND_OPENING_BID" && openingBids.length > 0 ? (
                                <table className="standings-table opening-bid-table">
                                    <thead>
                                        <tr>
                                            <th>Captain</th>
                                            <th>Status</th>
                                            <th>Bid</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {openingBids.map(bid => (
                                            <tr key={bid.id || bid.captainName}>
                                                <td>{bid.captainName}</td>
                                                <td>{bid.submitted ? (bid.passed ? "Passed" : "Submitted") : "Waiting"}</td>
                                                <td>{bid.submitted && !bid.passed ? `₹${bid.bidAmount}` : "-"}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : (
                                <p className="table-empty">Opening bid round has not started.</p>
                            )}
                        </div>
                    </div>

                    <h2>Sell Player</h2>

                    {allCaptainsPassed && (
                        <p className="message-success" role="status">
                            All captains passed on the opening bid. This player is ready to be marked unsold.
                        </p>
                    )}

                    {openingBidTie && (
                        <div className="message-warning opening-bid-tie-alert" role="alert">
                            <strong>Opening bid tie</strong>
                            <span>
                                {openingBidTie.captainName} tied at ₹{openingBidTie.amount}.
                                Ask these captains to submit the opening bid again or pass.
                            </span>
                        </div>
                    )}

                    <div className="form-field">

                        <label htmlFor="current-nominated-player">
                            Player nominated
                        </label>

                        <input
                            id="current-nominated-player"
                            className="input"
                            type="text"
                            data-testid="current-nominated-player"
                            value={isWheelSpinning ? "Wheel spinning..." : currentAuction?.currentPlayer || "No player nominated"}
                            disabled
                            readOnly
                        />

                    </div>

                    <form
                        onSubmit={(e) => {
                            e.preventDefault();
                            sellPlayer();
                        }}
                    >

                        <div className="form-field">

                            <label htmlFor="sale-player">
                                Player
                            </label>

                            <select
                                id="sale-player"
                                className="select"
                                value={playerName}
                                disabled={isBusy || isWheelSpinning}
                                onChange={(e) =>
                                    setPlayerName(
                                        e.target.value
                                    )
                                }
                            >

                                <option value="">
                                    Select Player
                                </option>

                                {currentAuction?.currentPlayer &&
                                    !players.some(
                                        player => player.name === currentAuction.currentPlayer
                                    ) && (
                                        <option value={currentAuction.currentPlayer}>
                                            {currentAuction.currentPlayer}
                                            {" "}
                                            (₹{currentAuction.basePrice})
                                        </option>
                                    )}

                                {players.map(
                                    (player) => (

                                        <option
                                            key={player.name}
                                            value={
                                                player.name
                                            }
                                        >
                                            {player.name}
                                            {" "}
                                            (
                                            ₹{player.basePrice}
                                            )
                                        </option>

                                    )
                                )}

                            </select>

                        </div>

                        <div className="form-field">

                            <label htmlFor="winning-captain">
                                Winning Captain
                            </label>

                            <select
                                id="winning-captain"
                                className="select"
                                value={captainName}
                                disabled={isBusy || isWheelSpinning}
                                onChange={(e) => setCaptainName(e.target.value)}
                            >

                                <option value="">
                                    Select Captain
                                </option>

                                {teams.map(
                                    (team) => (

                                        <option
                                            key={
                                                team.captainName
                                            }
                                            value={
                                                team.captainName
                                            }
                                        >
                                            {
                                                team.captainName
                                            }
                                        </option>

                                    )
                                )}

                            </select>

                        </div>

                        <div className="form-field">

                            <label htmlFor="sold-price">
                                Sold Price
                            </label>

                            <input
                                id="sold-price"
                                className="input"
                                type="number"
                                value={soldPrice}
                                disabled={isBusy || isWheelSpinning}
                                onChange={(e) => setSoldPrice(e.target.value)}
                            />

                        </div>

                        <div className="button-group">

                            <button
                                className="button-secondary"
                                type="button"
                                onClick={callSold}
                                disabled={actionBusy
                                    || outcomeUnknown
                                    || isWheelSpinning
                                    || auctionStatus?.auctionPhase === "SOLD"
                                    || callSoldPlayer === currentAuction?.currentPlayer}
                            >

                                {pendingAction === "Calling SOLD" ? "Calling SOLD..." : "🟢 CALL SOLD"}

                            </button>


                            <button
                                className="button"
                                type="submit"
                                disabled={actionBusy || outcomeUnknown || isWheelSpinning}
                            >

                                {pendingAction === "Confirming sale" ? "Confirming sale..." : "✅ CONFIRM SALE"}

                            </button>

                            <button
                                className="button-secondary"
                                type="button"
                                onClick={markUnsold}
                                disabled={actionBusy || outcomeUnknown || isWheelSpinning || !currentAuction?.currentPlayer || auctionStatus?.auctionPhase === "SOLD"}
                            >
                                ◌ Mark Unsold
                            </button>

                            <button
                                className="button-secondary"
                                type="button"
                                onClick={undoSale}
                                disabled={actionBusy || outcomeUnknown || isWheelSpinning}
                            >

                                {pendingAction === "Undoing sale" ? "Undoing sale..." : "↩️ UNDO LAST SALE"}

                            </button>

                        </div>

                    </form>

                    {message && (

                        <div
                            className={
                                messageIsError ? "message-error" : "message-success"
                            }
                        >
                            {message}
                        </div>

                    )}

                    <ActionFeedback
                        feedback={actionFeedback}
                        onAcknowledge={refreshAndAcknowledgeOutcome}
                    />

                </div>

                {!isWheelSpinning && <SilentBidManager />}
            </div>
        </>
    );
}

export default AuctionManager;
