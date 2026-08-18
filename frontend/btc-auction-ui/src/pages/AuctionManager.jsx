import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";

function AuctionManager() {

    const [teams, setTeams] = useState([]);
    const [players, setPlayers] = useState([]);
    const [currentAuction, setCurrentAuction] = useState(null);

    const [playerName, setPlayerName] = useState("");
    const [captainName, setCaptainName] = useState("");
    const [soldPrice, setSoldPrice] = useState("");

    const [message, setMessage] = useState("");
    const [auctionMode, setAuctionMode] =
        useState("LIVE");

    const [targetsReady, setTargetsReady] = useState(null);
    const [missingTargetMessage, setMissingTargetMessage] = useState("");
    const [isConnected, setIsConnected] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    const loadData = async () => {

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

            const targetsResponse = await fetch(
                `${API_URL}/api/auction/targets-ready`
            );

            const teamsData =
                await teamsResponse.json();

            const playersData =
                await playersResponse.json();

            const currentAuction =
                await currentAuctionResponse.json();

            const targetsData =
                await targetsResponse.json();

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
            setTargetsReady(targetsData);

            if (!targetsData.allSubmitted) {
                let msg = "⚠️ Waiting for targets:\n";
                if (targetsData.missingSecretTargets && targetsData.missingSecretTargets.length > 0) {
                    msg += "Secret Targets: " + targetsData.missingSecretTargets.join(", ") + "\n";
                }
                if (targetsData.missingReverseTargets && targetsData.missingReverseTargets.length > 0) {
                    msg += "Reverse Targets: " + targetsData.missingReverseTargets.join(", ");
                }
                if (targetsData.bountyCount !== 4) {
                    msg += "\nNormal Bounties: " + (targetsData.bountyCount || 0) + "/4";
                }
                if (targetsData.goldenBountyCount !== 2) {
                    msg += "\nGolden Bounties: " + (targetsData.goldenBountyCount || 0) + "/2";
                }
                setMissingTargetMessage(msg);
            } else {
                setMissingTargetMessage("✅ All captains have submitted their targets!");
            }

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to load data."
            );

        }
    };

    useEffect(() => {
        loadData();

        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => {
                setIsConnected(true);
                client.subscribe("/topic/auction", loadData);
            },
            onDisconnect: () => setIsConnected(false),
            onWebSocketClose: () => setIsConnected(false),
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, []);

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

    const cancelNomination = async () => {

        if (!currentAuction?.currentPlayer) {
            setMessage("No active nomination to cancel.");
            return;
        }

        if (!window.confirm(`Cancel the nomination for ${currentAuction.currentPlayer}?`)) {
            return;
        }

        try {
            setIsBusy(true);

            const response = await fetch(
                `${API_URL}/api/auction/veto`,
                { method: "POST" }
            );

            setMessage(await response.text());
            await loadData();
        } catch (error) {
            console.error(error);
            setMessage("Unable to cancel nomination.");
        } finally {
            setIsBusy(false);
        }
    };

    const startAuction = async () => {

        if (!window.confirm("Start the auction now? Captains will be able to nominate players.")) {
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

    return (
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

                {missingTargetMessage && (
                    <div style={{
                        marginBottom: "15px",
                        padding: "12px",
                        backgroundColor: targetsReady?.allSubmitted ? "#d4edda" : "#fff3cd",
                        borderRadius: "4px",
                        whiteSpace: "pre-wrap",
                        fontSize: "0.95em"
                    }}>
                        {missingTargetMessage}
                    </div>
                )}

                <button
                    className="button"
                    onClick={startAuction}
                    disabled={!targetsReady?.allSubmitted || isBusy}
                    style={{ opacity: !targetsReady?.allSubmitted ? 0.5 : 1, cursor: !targetsReady?.allSubmitted ? "not-allowed" : "pointer" }}
                >

                    🚀 Start Auction

                </button>

                <h2>Sell Player</h2>

                <div className="form-field">

                    <label>
                        Player nominated
                    </label>

                    <input
                        className="input"
                        type="text"
                        value={currentAuction?.currentPlayer || "No player nominated"}
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

                        <label>
                            Player
                        </label>

                        <select
                            className="select"
                            value={playerName}
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

                        <label>
                            Winning Captain
                        </label>

                        <select
                            className="select"
                            value={captainName}
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

                        <label>
                            Sold Price
                        </label>

                        <input
                            className="input"
                            type="number"
                            value={soldPrice}
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
                            disabled={isBusy}
                        >

                            🟢 CALL SOLD

                        </button>


                        <button
                            className="button"
                            type="submit"
                            disabled={isBusy}
                        >

                            ✅ CONFIRM SALE

                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={undoSale}
                            disabled={isBusy}
                        >

                            ↩️ UNDO LAST SALE

                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={cancelNomination}
                            disabled={isBusy || !currentAuction?.currentPlayer}
                        >

                            ❌ CANCEL NOMINATION

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

        </div>
    );
}

export default AuctionManager;
