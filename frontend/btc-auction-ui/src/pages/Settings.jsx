import { useEffect, useState } from "react";

function Settings() {

    const [config, setConfig] =
        useState(null);

    const [message, setMessage] =
        useState("");

    const [players, setPlayers] =
        useState([]);

    const [playerOne, setPlayerOne] =
        useState("");

    const [playerTwo, setPlayerTwo] =
        useState("");

    const [playerThree, setPlayerThree] =
        useState("");

    const [playerFour, setPlayerFour] =
        useState("");

    const [goldenPlayer, setGoldenPlayer] =
        useState("");

    const resetAuction = async () => {

        if (
            !window.confirm(
                "Reset entire auction?"
            )
        ) {
            return;
        }

        const response =
            await fetch(
                "http://localhost:8080/api/auction/reset",
                {
                    method: "POST"
                }
            );

        const result =
            await response.text();

        setMessage(result);
    };

    useEffect(() => {

        fetch(
            "http://localhost:8080/api/config"
        )
            .then(res => res.json())
            .then(data => setConfig(data));

        fetch(
            "http://localhost:8080/api/players"
        )
            .then(res => res.json())
            .then(data => setPlayers(data));

        fetch(
            "http://localhost:8080/api/bounty"
        )
            .then(res => res.json())
            .then(data => {

                if (data.length > 0) {

                    setPlayerOne(
                        data[0]?.playerName || ""
                    );

                    setPlayerTwo(
                        data[1]?.playerName || ""
                    );

                    setPlayerThree(
                        data[2]?.playerName || ""
                    );

                    setPlayerFour(
                        data[3]?.playerName || ""
                    );

                    const golden =
                        data.find(
                            p => p.golden
                        );

                    if (golden) {

                        setGoldenPlayer(
                            golden.playerName
                        );
                    }
                }
            });

    }, []);

    const saveConfig = async () => {

        const response =
            await fetch(
                "http://localhost:8080/api/config",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify(config)
                }
            );

        if (response.ok) {

            setMessage(
                "Settings Saved"
            );
        }
    };

    const saveBountyPlayers =
        async () => {

            const response =
                await fetch(
                    "http://localhost:8080/api/bounty",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/json"
                        },
                        body:
                            JSON.stringify({
                                playerOne,
                                playerTwo,
                                playerThree,
                                playerFour,
                                goldenPlayer
                            })
                    }
                );

            if (response.ok) {

                setMessage(
                    "Bounty Players Saved"
                );
            }
        };

    if (!config) {
        return <div>Loading...</div>;
    }

    return (

        <div className="form-card">

            <h2>
                Auction Settings
            </h2>

            <div className="form-field">

                <label>
                    Season Name
                </label>

                <input
                    className="input"
                    value={config.seasonName}
                    onChange={(e) =>
                        setConfig({
                            ...config,
                            seasonName:
                                e.target.value
                        })
                    }
                />

            </div>

            {[
                "squadSize",
                "targetBonus",
                "targetCompletionBonus",
                "bountyBonus",
                "goldenBountyBonus",
                "stealPenalty"
            ].map(field => (

                <div
                    key={field}
                    className="form-field"
                >

                    <label>
                        {field}
                    </label>

                    <input
                        type="number"
                        className="input"
                        value={config[field]}
                        onChange={(e) =>
                            setConfig({
                                ...config,
                                [field]:
                                    Number(
                                        e.target.value
                                    )
                            })
                        }
                    />

                </div>

            ))}

            <button
                className="button"
                onClick={saveConfig}
            >
                Save Settings
            </button>

            <button
                className="button-secondary"
                onClick={resetAuction}
            >
                Reset Auction
            </button>

            <hr
                style={{
                    marginTop: "30px",
                    marginBottom: "30px"
                }}
            />

            <h2>
                Bounty Players
            </h2>

            {[

                {
                    label:
                        "Bounty Player 1",
                    value:
                        playerOne,
                    setter:
                        setPlayerOne
                },

                {
                    label:
                        "Bounty Player 2",
                    value:
                        playerTwo,
                    setter:
                        setPlayerTwo
                },

                {
                    label:
                        "Bounty Player 3",
                    value:
                        playerThree,
                    setter:
                        setPlayerThree
                },

                {
                    label:
                        "Bounty Player 4",
                    value:
                        playerFour,
                    setter:
                        setPlayerFour
                }

            ].map(item => (

                <div
                    key={item.label}
                    className="form-field"
                >

                    <label>
                        {item.label}
                    </label>

                    <select
                        className="input"
                        value={item.value}
                        onChange={(e) =>
                            item.setter(
                                e.target.value
                            )
                        }
                    >

                        <option value="">
                            Select Player
                        </option>

                        {players.map(
                            player => (

                                <option
                                    key={
                                        player.id
                                    }
                                    value={
                                        player.name
                                    }
                                >
                                    {
                                        player.name
                                    }
                                </option>

                            )
                        )}

                    </select>

                </div>

            ))}

            <div className="form-field">

                <label>
                    Golden Bounty
                </label>

                <select
                    className="input"
                    value={goldenPlayer}
                    onChange={(e) =>
                        setGoldenPlayer(
                            e.target.value
                        )
                    }
                >

                    <option value="">
                        Select Golden
                        Bounty
                    </option>

                    {[
                        playerOne,
                        playerTwo,
                        playerThree,
                        playerFour
                    ]
                        .filter(Boolean)
                        .map(player => (

                            <option
                                key={player}
                                value={player}
                            >
                                {player}
                            </option>

                        ))}

                </select>

            </div>

            <button
                className="button"
                onClick={
                    saveBountyPlayers
                }
            >
                Save Bounty Players
            </button>

            {message && (

                <div
                    className=
                    "message-success"
                >
                    {message}
                </div>

            )}

        </div>

    );
}

export default Settings;