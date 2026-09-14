import { useEffect, useState } from "react";

import { API_URL } from "../config";
import { showToast } from "../services/toast";

function SilentBidManager() {
    const [currentAuctionPlayer, setCurrentAuctionPlayer] = useState("");
    const [bids, setBids] = useState([]);
    const [message, setMessage] = useState("");
    const [winner, setWinner] = useState(null);
    const [soldCalled, setSoldCalled] = useState(false);

    const loadData = async () => {
        try {
            const [auctionRes, bidsRes] = await Promise.all([
                fetch(`${API_URL}/api/auction/current`),
                fetch(`${API_URL}/api/silent-bid/all`),
            ]);
            if (auctionRes.ok) {
                const auctionData = await auctionRes.json();
                setCurrentAuctionPlayer(auctionData?.currentPlayer || "");
            }
            if (bidsRes.ok) {
                const bidsData = await bidsRes.json();
                setBids(bidsData || []);
            }
        } catch {
            // ignore network errors
        }
    };

    useEffect(() => {
        let isMounted = true;
        const fetchAuctionAndBids = async () => {
            try {
                const [auctionRes, bidsRes] = await Promise.all([
                    fetch(`${API_URL}/api/auction/current`),
                    fetch(`${API_URL}/api/silent-bid/all`),
                ]);
                if (auctionRes.ok) {
                    const auctionData = await auctionRes.json();
                    if (isMounted) setCurrentAuctionPlayer(auctionData?.currentPlayer || "");
                }
                if (bidsRes.ok) {
                    const bidsData = await bidsRes.json();
                    if (isMounted) setBids(bidsData || []);
                }
            } catch {
                // ignore network errors
            }
        };

        fetchAuctionAndBids();
        const interval = setInterval(fetchAuctionAndBids, 2000);
        return () => {
            isMounted = false;
            clearInterval(interval);
        };
    }, []);

    const activePlayer = bids.length > 0 ? (bids[0]?.playerName || "") : currentAuctionPlayer;
    const roundStarted = bids.length > 0;

    const startRound = async () => {
        if (!activePlayer) {
            showToast("No player currently selected in auction wheel.", "error");
            return;
        }

        const response = await fetch(`${API_URL}/api/silent-bid/start`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ playerName: activePlayer }),
        });

        setMessage(await response.text());
        setWinner(null);
        setSoldCalled(false);
        await loadData();
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
            await loadData();
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
        const successful = result.includes("Waiting for RTM.");

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
        setSoldCalled(false);
        await loadData();
    };

    const resetRound = async () => {
        const response = await fetch(`${API_URL}/api/silent-bid/clear`, {
            method: "POST",
        });

        setMessage(await response.text());
        setWinner(null);
        setSoldCalled(false);
        await loadData();
    };

    return (
        <div className="app-container">
            <div className="form-card">
                <h1>🔒 Silent Bid Manager</h1>

                <div style={{ marginTop: "15px", marginBottom: "15px", fontSize: "18px" }}>
                    <span>Selected Player: </span>
                    <strong>{activePlayer || "No player nominated on wheel"}</strong>
                </div>

                {!roundStarted ? (
                    <button
                        className="button"
                        disabled={!activePlayer}
                        onClick={startRound}
                    >
                        🔒 Start Silent Bid Round for {activePlayer || "Current Player"}
                    </button>
                ) : (
                    <div className="message-success" style={{ marginTop: "10px" }}>
                        🔒 Silent Bidding Active for <strong>{activePlayer}</strong>
                    </div>
                )}

                {message && <div className="message-success" style={{ marginTop: "15px" }}>{message}</div>}
            </div>

            <div className="form-card" style={{ marginTop: "25px" }}>
                <h2>Incoming Bids {activePlayer ? `— ${activePlayer}` : ""}</h2>

                <div className="silent-bid-matrix-wrap">
                    <table className="standings-table silent-bid-matrix">
                        <thead>
                            <tr>
                                <th scope="col">Player</th>
                                {bids.map(bid => (
                                    <th key={bid.id} scope="col">{bid.captainName}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {bids.length > 0 ? (
                                <tr>
                                    <td className="silent-bid-player">{activePlayer}</td>
                                    {bids.map(bid => (
                                        <td
                                            key={bid.id}
                                            className={winner?.captainName === bid.captainName
                                                ? "silent-bid-cell silent-bid-winner"
                                                : "silent-bid-cell"}
                                        >
                                            <strong>{bid.submitted ? `₹${bid.bidAmount}` : "-"}</strong>
                                            <span className={bid.submitted ? "silent-bid-status submitted" : "silent-bid-status"}>
                                                {bid.submitted ? "Submitted" : "Waiting"}
                                            </span>
                                        </td>
                                    ))}
                                </tr>
                            ) : (
                                <tr>
                                    <td colSpan="1" className="silent-bid-empty">
                                        No incoming bids. Start a silent bid round to collect bids.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

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
