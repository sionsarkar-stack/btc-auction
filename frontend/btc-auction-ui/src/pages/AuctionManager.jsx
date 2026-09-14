import { useEffect, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import SilentBidManager from "./SilentBidManager";
import CasinoReel from "../components/CasinoReel";
import LiveActivity from "../components/LiveActivity";

function AuctionManager() {

    const [teams, setTeams] = useState([]);
    const [players, setPlayers] = useState([]);
    const [currentAuction, setCurrentAuction] = useState(null);

    const [playerName, setPlayerName] = useState("");
    const [captainName, setCaptainName] = useState("");
    const [soldPrice, setSoldPrice] = useState("");

    const [message, setMessage] = useState("");

    const [clubbedPairs, setClubbedPairs] = useState([]);
    const [randomEvents, setRandomEvents] = useState([]);
    const [pairPlayerOne, setPairPlayerOne] = useState("");
    const [pairPlayerTwo, setPairPlayerTwo] = useState("");
    const [isConnected, setIsConnected] = useState(false);
    const [isBusy, setIsBusy] = useState(false);
    const [auctionStatus, setAuctionStatus] = useState(null);
    const [protectionPlayers, setProtectionPlayers] = useState([]);

    const loadData = useCallback(async () => {

        try {

            const teamsResponse = await fetch(
                `${API_URL}/api/teams`
            );

            const playersResponse = await fetch(
                `${API_URL}/api/players/available`
            );

            const currentAuctionResponse = await fetch(
                `${API_URL}/api/auction/current`
            );

            const statusResponse = await fetch(`${API_URL}/api/auction/status`);
            const protectionResponse = await fetch(`${API_URL}/api/protection-players`);

            const clubbedPairResponse = await fetch(
                `${API_URL}/api/auction/clubbed-pair`
            );

            const randomEventsResponse = await fetch(
                `${API_URL}/api/random-events`
            );

            const teamsData =
                await teamsResponse.json();

            const playersData =
                await playersResponse.json();

            const currentAuction =
                await currentAuctionResponse.json();

            const statusData = await statusResponse.json();
            const protectionData = await protectionResponse.json();

            const clubbedPairData =
                await clubbedPairResponse.json();

            const randomEventsData =
                await randomEventsResponse.json();

            const nominatedPlayer = currentAuction?.currentPlayer;
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
            setCurrentAuction(currentAuction);
            setAuctionStatus(statusData);
            setProtectionPlayers(protectionData);
            setClubbedPairs(Array.isArray(clubbedPairData) ? clubbedPairData : []);
            setRandomEvents(randomEventsData);
        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to load data."
            );

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

    const callSold = async () => {

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

        setIsBusy(true);
        try {
            const response = await fetch(
                `${API_URL}/api/auction/call-sold`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        captainName,
                        currentBid: Number(soldPrice)
                    })
                }
            );

            setMessage(await response.text());
        } catch (error) {
            console.error(error);
            setMessage("Unable to call SOLD. Check the connection and try again.");
        } finally {
            setIsBusy(false);
        }

    };

    const sellPlayer = async () => {

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

        try {
            setIsBusy(true);

            const response = await fetch(
                `${API_URL}/api/auction/sold`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json",
                    },
                    body: JSON.stringify({
                        playerName,
                        captainName,
                        soldPrice:
                            Number(soldPrice),
                    }),
                }
            );

            const result =
                await response.text();

            setMessage(result);

            setPlayerName("");
            setCaptainName("");
            setSoldPrice("");

            await loadData();

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to sell player."
            );
        } finally {
            setIsBusy(false);

        }
    };

    const undoSale = async () => {

        if (!window.confirm("Undo the last sale? This changes team and player records.")) {
            return;
        }


        try {
            setIsBusy(true);

            const response = await fetch(
                `${API_URL}/api/auction/undo`,
                {
                    method: "POST",
                }
            );

            const result =
                await response.text();

            setMessage(result);

            await loadData();

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to undo sale."
            );
        } finally {
            setIsBusy(false);

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

    const enableProtectionSelection = async () => {
        setIsBusy(true);
        try {
            const response = await fetch(`${API_URL}/api/admin/protection-selection/enable`, { method: "POST" });
            setMessage(await response.text());
            await loadData();
        } finally {
            setIsBusy(false);
        }
    };

    const autoSelectProtectionPlayers = async () => {
        setIsBusy(true);
        try {
            const response = await fetch(`${API_URL}/api/protection-players/auto-select`, { method: "POST" });
            setMessage(await response.text());
            await loadData();
        } finally {
            setIsBusy(false);
        }
    };

    const protectionReady = auctionStatus?.protectionSelectionEnabled
        && teams.length > 0
        && teams.every(team => protectionPlayers.filter(
            item => item.captainName === team.captainName
        ).length === 2);

    const startReAuction = async () => {
        if (!window.confirm("Start the one allowed re-auction round? Unsold players will use half their original base price.")) {
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

    const updateCurrentAuction = async (captain, price) => {

        if (!captain || !price) {
            return;
        }

        await fetch(
            `${API_URL}/api/auction/update-current`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    captainName: captain,
                    currentBid: Number(price)
                })
            }
        );

    };

    const saveClubbedPair = async () => {
        if (!pairPlayerOne || !pairPlayerTwo) {
            setMessage("Select both players for the A + E pair.");
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
                            disabled={isBusy || !protectionReady}
                        >
                            🚀 Start Auction
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={enableProtectionSelection}
                            disabled={isBusy || auctionStatus?.protectionSelectionEnabled || auctionStatus?.auctionStarted}
                        >
                            🛡️ Enable Selection of Protection Player
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={autoSelectProtectionPlayers}
                            disabled={isBusy || auctionStatus?.auctionStarted}
                        >
                            🧪 Bypass: Auto-select Protection Players
                        </button>

                        {protectionReady && (
                            <div className="message-success protection-ready-message">
                                Protection players are ready for every captain. Start Auction is enabled.
                            </div>
                        )}

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={spinWheel}
                            data-testid="spin-wheel"
                            disabled={isBusy || !auctionStatus?.auctionStarted || wheelPlayers.length === 0 || Boolean(currentAuction?.currentPlayer)}
                            title={currentAuction?.currentPlayer ? "Resolve the active player before spinning again." : undefined}
                        >
                            🎡 Spin Wheel
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={startReAuction}
                            disabled={isBusy}
                        >
                            🔄 Start Re-auction
                        </button>

                    </div>

                    {randomEvents.length > 0 && (
                        <div style={{ marginBottom: "20px", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "10px", background: "#f8fafc" }}>
                            <h3 style={{ margin: "0 0 12px" }}>Random Event Library</h3>
                            <div style={{ display: "grid", gap: "8px" }}>
                                {randomEvents.map((event) => {
                                    const isCurrentRandomEvent =
                                        auctionStatus?.pendingRandomEventType === event.type;

                                    return (
                                        <div
                                            key={event.id}
                                            className={`random-event-library-item${isCurrentRandomEvent ? " is-active" : ""}`}
                                            aria-label={isCurrentRandomEvent ? `${event.type} is active` : undefined}
                                        >
                                            <div className="random-event-library-item-heading">
                                                <strong>{event.type}</strong>
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
                        <h3 style={{ margin: 0 }}>A + E Pair (Admin)</h3>
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(180px, 1fr))", gap: "12px" }}>
                            <select className="select" value={pairPlayerOne} onChange={(e) => setPairPlayerOne(e.target.value)}>
                                <option value="">Select Player 1</option>
                                {players.map((player) => (
                                    <option key={`pair-one-${player.name}`} value={player.name}>{player.name}</option>
                                ))}
                            </select>
                            <select className="select" value={pairPlayerTwo} onChange={(e) => setPairPlayerTwo(e.target.value)}>
                                <option value="">Select Player 2</option>
                                {players.map((player) => (
                                    <option key={`pair-two-${player.name}`} value={player.name}>{player.name}</option>
                                ))}
                            </select>
                        </div>
                        <div className="button-group">
                            <button className="button" type="button" onClick={saveClubbedPair} disabled={isBusy || !pairPlayerOne || !pairPlayerTwo}>
                                💼 Save A + E Pair
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

                    <div className="wheel-layout" style={{ display: "grid", gridTemplateColumns: "minmax(280px, 360px) 1fr", gap: "20px", alignItems: "center", marginBottom: "24px" }}>
                        <CasinoReel
                            players={wheelPlayers}
                            selectedPlayer={currentAuction?.currentPlayer}
                            spinStartedAt={auctionStatus?.wheelSpinStartedAt}
                            spinEndsAt={auctionStatus?.wheelSpinEndsAt}
                            seasonName={auctionStatus?.seasonName}
                        />

                        <div>
                            <h3 style={{ margin: "0 0 10px", color: "#d97706" }}>💎 {auctionStatus?.seasonName || "BTC Season 12"} Auction Reel</h3>
                            <p style={{ margin: "0 0 8px" }}>Every unsold player is in the live spinning drum. High-roller slot easing locks onto nominees automatically.</p>
                            <p style={{ fontSize: "13px", color: "#64748b", margin: 0 }}>
                                {wheelPlayers.length} player{wheelPlayers.length === 1 ? "" : "s"} remaining in wheel drum.
                            </p>
                        </div>
                    </div>

                    <h2>Sell Player</h2>

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
                                onChange={async (e) => {

                                    const captain = e.target.value;

                                    setCaptainName(captain);

                                    await updateCurrentAuction(
                                        captain,
                                        soldPrice
                                    );

                                }}
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
                                onChange={async (e) => {

                                    const price = e.target.value;

                                    setSoldPrice(price);

                                    await updateCurrentAuction(
                                        captainName,
                                        price
                                    );

                                }}
                            />

                        </div>

                        <div className="button-group">

                            <button
                                className="button-secondary"
                                type="button"
                                onClick={callSold}
                                disabled={isBusy || isWheelSpinning}
                            >

                                🟢 CALL SOLD

                            </button>


                            <button
                                className="button"
                                type="submit"
                                disabled={isBusy || isWheelSpinning}
                            >

                                ✅ CONFIRM SALE

                            </button>

                            <button
                                className="button-secondary"
                                type="button"
                                onClick={undoSale}
                                disabled={isBusy || isWheelSpinning}
                            >

                                ↩️ UNDO LAST SALE

                            </button>

                        </div>

                    </form>

                    {message && (

                        <div
                            className={
                                message
                                    .includes(
                                        "Unable"
                                    )
                                    ? "message-error"
                                    : "message-success"
                            }
                        >
                            {message}
                        </div>

                    )}

                </div>

                {!isWheelSpinning && <SilentBidManager />}
            </div>
        </>
    );
}

export default AuctionManager;
