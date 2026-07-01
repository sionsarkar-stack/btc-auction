import { useEffect, useState } from "react";

import { API_URL } from "../config";

function SilentBidManager() {

    const [players, setPlayers] = useState([]);
    const [playerName, setPlayerName] = useState("");
    const [bids, setBids] = useState([]);
    const [message, setMessage] = useState("");
    const [winner, setWinner] = useState(null);
    const [roundStarted, setRoundStarted] = useState(false);

    useEffect(() => {

        loadPlayers();
        loadBids();

        const interval = setInterval(() => {

            loadBids();

        }, 2000);

        return () => clearInterval(interval);

    }, []);

    const loadPlayers = async () => {

        const response = await fetch(
            `${API_URL}/api/players/available`
        );

        setPlayers(await response.json());

    };

    const loadBids = async () => {

        const response = await fetch(
            `${API_URL}/api/silent-bid/all`
        );

        setBids(await response.json());

    };

    const startRound = async () => {

        if (!playerName) {

            alert("Select a player.");

            return;

        }

        const response = await fetch(
            `${API_URL}/api/silent-bid/start`,
            {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    playerName

                })

            });

        setMessage(await response.text());

        setRoundStarted(true);

        setWinner(null);

        loadBids();

    };

    const revealWinner = async () => {

        const response =
            await fetch(
                `${API_URL}/api/silent-bid/winner`
            );

        if (!response.ok) {

            alert(await response.text());

            return;

        }

        const data =
            await response.json();

        if (data.tie) {

            alert(
                "Tie detected!\n\n" +
                data.tiedCaptains.join(", ") +
                "\n\nPlease rebid."
            );

            setWinner(null);

            loadBids();

            return;

        }

        setWinner(
            data.winner
        );

    };

    const sellWinner = async () => {

        if (!winner) {

            alert("Reveal winner first.");

            return;

        }

        if (!window.confirm(

            `Sell ${winner.playerName} to ${winner.captainName} for ₹${winner.bidAmount}?`

        )) {

            return;

        }

        const response =
            await fetch(

                `${API_URL}/api/silent-bid/sell`,

                {

                    method: "POST"

                }

            );

        alert(
            await response.text()
        );

        setWinner(null);

        setRoundStarted(false);

        setPlayerName("");

        loadPlayers();

        loadBids();

    };

    const resetRound = async () => {

        const response = await fetch(
            `${API_URL}/api/silent-bid/clear`,
            {
                method: "POST"
            });

        setMessage(await response.text());

        setWinner(null);

        setRoundStarted(false);

        setPlayerName("");

        loadBids();

    };

    return (

        <div className="app-container">

            <div className="form-card">

                <h1>

                    🔒 Silent Bid Manager

                </h1>

                <select
                    className="select"
                    value={playerName}
                    disabled={roundStarted}
                    onChange={(e) =>
                        setPlayerName(e.target.value)}
                >

                    <option value="">
                        Select Player
                    </option>

                    {players.map(player => (

                        <option
                            key={player.name}
                            value={player.name}
                        >

                            {player.name} ({player.seed})

                        </option>

                    ))}

                </select>

                <button
                    className="button"
                    style={{
                        marginTop: "20px"
                    }}
                    disabled={roundStarted}
                    onClick={startRound}
                >

                    🔒 Start Silent Bid Round

                </button>

                {message && (

                    <div className="message-success">

                        {message}

                    </div>

                )}

            </div>

            <div
                className="form-card"
                style={{
                    marginTop: "25px"
                }}
            >

                <h2>

                    Incoming Bids

                </h2>

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
                                    backgroundColor:

                                        winner &&
                                            winner.captainName === bid.captainName

                                            ? "#d4edda"

                                            : "transparent"
                                }}
                            >

                                <td>

                                    {bid.captainName}

                                </td>

                                <td>

                                    {bid.submitted

                                        ? `₹${bid.bidAmount}`

                                        : "-"}

                                </td>

                                <td>

                                    {bid.submitted

                                        ? "✅ Submitted"

                                        : "⌛ Waiting"}

                                </td>

                            </tr>

                        ))}

                    </tbody>

                </table>

                {winner && (

                    <div
                        className="message-success"
                        style={{
                            marginTop: "20px",
                            fontSize: "22px"
                        }}
                    >

                        🏆 Winner

                        <br />

                        {winner.captainName}

                        <br />

                        ₹{winner.bidAmount}

                    </div>

                )}

                <div
                    className="button-group"
                    style={{
                        marginTop: "25px"
                    }}
                >

                    <button
                        className="button"
                        onClick={revealWinner}
                    >

                        🏆 Reveal Winner

                    </button>

                    <button
                        className="button"
                        disabled={!winner}
                        onClick={sellWinner}
                    >

                        💰 Sell Winner

                    </button>

                    <button
                        className="button-secondary"
                        onClick={resetRound}
                    >

                        🔄 Reset Round

                    </button>

                </div>

            </div>

        </div>

    );

}

export default SilentBidManager;