import { useEffect, useState } from "react";

import { API_URL } from "../config";

function ManualSale() {

    const [players, setPlayers] = useState([]);
    const [teams, setTeams] = useState([]);

    const [playerName, setPlayerName] = useState("");
    const [newCaptain, setNewCaptain] = useState("");
    const [newPrice, setNewPrice] = useState("");

    const [message, setMessage] = useState("");

    useEffect(() => {

        fetch(`${API_URL}/api/players`)
            .then(response => response.json())
            .then(data => setPlayers(data));

        fetch(`${API_URL}/api/teams`)
            .then(response => response.json())
            .then(data => setTeams(data));

    }, []);

    const applyManualSale = async () => {

        const response =
            await fetch(
                `${API_URL}/api/auction/manual-sale`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        playerName,
                        newCaptain,
                        newPrice: Number(newPrice)
                    })
                }
            );

        const result = await response.text();

        setMessage(result);
    };

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    Manual Sale / Trade
                </h1>
            </div>

            <div className="form-card">

                <div className="form-field">
                    <label>Player</label>

                    <select
                        className="select"
                        value={playerName}
                        onChange={(e) =>
                            setPlayerName(e.target.value)
                        }
                    >
                        <option value="">
                            Select Player
                        </option>

                        {players.map(player => (
                            <option
                                key={player.id}
                                value={player.name}
                            >
                                {player.name}
                            </option>
                        ))}
                    </select>

                </div>

                <div className="form-field">
                    <label>New Captain</label>

                    <select
                        className="select"
                        value={newCaptain}
                        onChange={(e) =>
                            setNewCaptain(e.target.value)
                        }
                    >
                        <option value="">
                            Select Captain
                        </option>

                        {teams.map(team => (
                            <option
                                key={team.id}
                                value={team.captainName}
                            >
                                {team.captainName}
                            </option>
                        ))}
                    </select>

                </div>

                <div className="form-field">
                    <label>New Price</label>

                    <input
                        className="input"
                        type="number"
                        value={newPrice}
                        onChange={(e) =>
                            setNewPrice(e.target.value)
                        }
                    />
                </div>

                <button
                    className="button"
                    onClick={applyManualSale}
                >
                    APPLY ADJUSTMENT
                </button>

                {message && (
                    <div className="message-success">
                        {message}
                    </div>
                )}

            </div>

        </div>
    );
}

export default ManualSale;