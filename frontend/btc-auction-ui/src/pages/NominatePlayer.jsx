import { useEffect, useState } from "react";

function NominatePlayer() {

    const [players, setPlayers] = useState([]);
    const [selectedPlayer, setSelectedPlayer] = useState("");
    const [message, setMessage] = useState(null);

    const loadPlayers = async () => {

        try {

            const response = await fetch(
                "http://localhost:8080/api/players/available"
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

                "http://localhost:8080/api/auction/nominate",

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

                        <select
                            className="select"
                            value={selectedPlayer}
                            onChange={(e) =>
                                setSelectedPlayer(
                                    e.target.value
                                )
                            }
                        >

                            <option value="">
                                Select Player
                            </option>

                            {players.map((player) => (

                                <option
                                    key={player.name}
                                    value={player.name}
                                >
                                    {player.name} ({player.seed})
                                </option>

                            ))}

                        </select>

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