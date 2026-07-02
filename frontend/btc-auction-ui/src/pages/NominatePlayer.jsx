import { useEffect, useState } from "react";

import { API_URL } from "../config";

function NominatePlayer() {

    const [players, setPlayers] = useState([]);
    const [selectedPlayer, setSelectedPlayer] = useState("");
    const [message, setMessage] = useState(null);
    const [forbiddenPlayer, setForbiddenPlayer] = useState("");

    const loadPlayers = async () => {

        try {

            const response = await fetch(
                `${API_URL}/api/players/available`
            );

            const data = await response.json();

            setPlayers(data);

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to load available players."
            );

        }
    };

    useEffect(() => {

        loadPlayers();

        fetch(

            `${API_URL}/api/forbidden/${localStorage.getItem("username")}`

        )

            .then(res => res.json())

            .then(data => {

                if (data) {

                    setForbiddenPlayer(data.playerName);

                }

            })

            .catch(() => { });

    }, []);




    const nominatePlayer = async () => {

        const player = players.find(
            (p) => p.name === selectedPlayer
        );

        if (!player) {

            setMessage(
                "Please select a player."
            );

            return;
        }

        try {

            const response = await fetch(

                `${API_URL}/api/auction/nominate`,

                {

                    method: "POST",

                    headers: {

                        "Content-Type": "application/json",

                    },

                    body: JSON.stringify({

                        playerName: player.name,

                        seed: player.seed,

                        captainName: localStorage.getItem("username")

                    }),

                }

            );

            const result =
                await response.text();

            setMessage(result);

            if (result === "Player nominated") {

                setSelectedPlayer("");

                await loadPlayers();
            }

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to nominate player."
            );

        }
    };

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    🏏 Nominate Player
                </h1>
            </div>

            <div className="form-card">

                <h2>Select Player</h2>

                <form
                    onSubmit={(event) => {
                        event.preventDefault();
                        nominatePlayer();
                    }}
                >

                    <div className="form-field">

                        <label>
                            Available Players
                        </label>


                        {forbiddenPlayer && (

                            <div className="message-error">

                                🚫 Tribunal Ban:
                                <strong> {forbiddenPlayer}</strong>
                                <br />
                                You cannot nominate this player.

                            </div>

                        )}
                        <div className="player-grid">

                            {players.map((player) => {

                                const isForbidden =
                                    player.name === forbiddenPlayer;

                                return (

                                    <div
                                        key={player.name}
                                        className={`player-card ${selectedPlayer === player.name
                                            ? "selected"
                                            : ""
                                            } ${isForbidden
                                                ? "forbidden"
                                                : ""
                                            }`}
                                        onClick={
                                            isForbidden
                                                ? undefined
                                                : () => setSelectedPlayer(player.name)
                                        }
                                    >

                                        <div className="player-name">

                                            {player.name}

                                        </div>

                                        <div className="player-seed">

                                            Seed {player.seed}

                                        </div>
                                        {selectedPlayer === player.name && (

                                            <div className="selected-tag">

                                                ✅ Selected

                                            </div>

                                        )}

                                        {isForbidden && (

                                            <div className="tribunal-tag">

                                                🚫 Tribunal Ban

                                            </div>

                                        )}
                                    </div>

                                );

                            })}

                        </div>

                        {selectedPlayer && (

                            <div className="message-success">

                                ✅ Selected Player:
                                <strong> {selectedPlayer}</strong>

                            </div>

                        )}

                    </div>

                    <button
                        className="button"
                        type="submit"
                    >
                        NOMINATE
                    </button>

                </form>

                {message && (

                    <div
                        className={
                            message.includes("already sold")
                                ||
                                message.includes("not found")
                                ||
                                message.includes("Unable")
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

export default NominatePlayer;