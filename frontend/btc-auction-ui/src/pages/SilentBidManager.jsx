import { useEffect, useState } from "react";

import { API_URL } from "../config";
import { showToast } from "../services/toast";

function SilentBidManager() {
    const [players, setPlayers] = useState([]);
    const [playerName, setPlayerName] = useState("");
    const [bids, setBids] = useState([]);
    const [message, setMessage] = useState("");
    const [winner, setWinner] = useState(null);
    const [roundStarted, setRoundStarted] = useState(false);
    const [soldCalled, setSoldCalled] = useState(false);

    useEffect(() => {
        loadPlayers();
        loadBids();

        const interval = setInterval(loadBids, 2000);
        return () => clearInterval(interval);
    }, []);

    const loadPlayers = async () => {
        const response = await fetch(`${API_URL}/api/players/available`);
        setPlayers(await response.json());
    };

    const loadBids = async () => {
        const response = await fetch(`${API_URL}/api/silent-bid/all`);
        setBids(await response.json());
    };

    const startRound = async () => {
        if (!playerName) {
            showToast("Select a player.", "error");
            return;
        }

        const response = await fetch(`${API_URL}/api/silent-bid/start`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ playerName }),
        });

        setMessage(await response.text());
        setRoundStarted(true);
        setWinner(null);
        setSoldCalled(false);
        await loadBids();
    };

    const revealWinner = async () => {
        const response = await fetch(`${API_URL}/api/silent-bid/winner`);

        if (!response.ok) {
            showToast(await response.text(), "error");
            return;
        }

        const data = await response.json();

        if (data.tie) {
            showToast(`Tie detected: ${data.tiedCaptains.join(", ")}. Please rebid.`, "error");
            setWinner(null);
            await loadBids();
            return;
        }

        setWinner(data.winner);
    };

    const callSoldWinner = async () => {
        if (!winner) {
            showToast("Reveal the winner first.", "error");
            return;
        }

        if (!window.confirm(
            `Call SOLD for ${winner.playerName} to ${winner.captainName} at ₹${winner.bidAmount} and open RTM?`
        )) {
            return;
        }

        const response = await fetch(`${API_URL}/api/silent-bid/call-sold`, {
            method: "POST",
        });
        const result = await response.text();
        const successful = result === "Waiting for RTM / Last Strike.";

        showToast(result, successful ? "success" : "error");
        setSoldCalled(successful);
    };

    const sellWinner = async () => {
        if (!winner || !soldCalled) {
            showToast("Call SOLD before confirming the sale.", "error");
            return;
        }

        if (!window.confirm(
            `Confirm final sale of ${winner.playerName} for ₹${winner.bidAmount}? Resolve any RTM first.`
        )) {
            return;
        }

        const response = await fetch(`${API_URL}/api/silent-bid/sell`, {
            method: "POST",
        });
        const result = await response.text();
        const completed = result.includes(" sold to ");

        showToast(result, completed ? "success" : "error");

        if (!completed) {
            return;
        }

        setWinner(null);
        setRoundStarted(false);
        setPlayerName("");
        setSoldCalled(false);
        await loadPlayers();
        await loadBids();
    };

    const resetRound = async () => {
        const response = await fetch(`${API_URL}/api/silent-bid/clear`, {
            method: "POST",
        });

        setMessage(await response.text());
        setWinner(null);
        setRoundStarted(false);
        setPlayerName("");
        setSoldCalled(false);
        await loadBids();
    };

    return (
        <div className="app-container">
            <div className="form-card">
                <h1>🔒 Silent Bid Manager</h1>

                <select
                    className="select"
                    value={playerName}
                    disabled={roundStarted}
                    onChange={event => setPlayerName(event.target.value)}
                >
                    <option value="">Select Player</option>
                    {players.map(player => (
                        <option key={player.name} value={player.name}>
                            {player.name} ({player.seed})
                        </option>
                    ))}
                </select>

                <button
                    className="button"
                    style={{ marginTop: "20px" }}
                    disabled={roundStarted}
                    onClick={startRound}
                >
                    🔒 Start Silent Bid Round
                </button>

                {message && <div className="message-success">{message}</div>}
            </div>

            <div className="form-card" style={{ marginTop: "25px" }}>
                <h2>Incoming Bids</h2>

                <table style={{ width: "100%" }}>
                    <thead>
                        <tr>
                            <th>Captain</th>
                            <th>Bid</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {bids.map(bid => (
                            <tr
                                key={bid.id}
                                style={{
                                    backgroundColor: winner?.captainName === bid.captainName
                                        ? "#d4edda"
                                        : "transparent",
                                }}
                            >
                                <td>{bid.captainName}</td>
                                <td>{bid.submitted ? `₹${bid.bidAmount}` : "-"}</td>
                                <td>{bid.submitted ? "✅ Submitted" : "⌛ Waiting"}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {winner && (
                    <div className="message-success" style={{ marginTop: "20px", fontSize: "22px" }}>
                        🏆 Winner
                        <br />
                        {winner.captainName}
                        <br />
                        ₹{winner.bidAmount}
                    </div>
                )}

                <div className="button-group" style={{ marginTop: "25px" }}>
                    <button className="button" onClick={revealWinner} disabled={soldCalled}>
                        🏆 Reveal Winner
                    </button>
                    <button className="button" onClick={callSoldWinner} disabled={!winner || soldCalled}>
                        🔨 Call SOLD / Open RTM
                    </button>
                    <button className="button" onClick={sellWinner} disabled={!winner || !soldCalled}>
                        💰 Confirm Sale
                    </button>
                    <button className="button-secondary" onClick={resetRound}>
                        🔄 Reset Round
                    </button>
                </div>
            </div>
        </div>
    );
}

export default SilentBidManager;
