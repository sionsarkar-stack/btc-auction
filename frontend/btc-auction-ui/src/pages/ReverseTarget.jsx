import { useEffect, useState } from "react";

function ReverseTarget() {

    const username =
        localStorage.getItem("username");

    const role =
        localStorage.getItem("role");

    const isAdmin =
        role === "ADMIN";

    const [players, setPlayers] =
        useState([]);

    const [captains, setCaptains] =
        useState([]);

    const [target, setTarget] =
        useState({

            captainName: username,
            rivalCaptain: "",
            playerName: ""

        });

    const [submitted, setSubmitted] =
        useState(null);

    const [allTargets, setAllTargets] =
        useState([]);

    useEffect(() => {

        fetch("http://localhost:8080/api/players")
            .then(response => response.json())
            .then(setPlayers);

        fetch("http://localhost:8080/api/teams")
            .then(response => response.json())
            .then(setCaptains);

        if (isAdmin) {

            fetch("http://localhost:8080/api/reverse-target")
                .then(response => response.json())
                .then(setAllTargets);

        } else {

            fetch(
                `http://localhost:8080/api/reverse-target/${username}`
            )
                .then(response => {

                    if (!response.ok) {

                        return null;

                    }

                    return response.json();

                })
                .then(data => {

                    if (data) {

                        setSubmitted(data);

                    }

                })
                .catch(() => { });

        }

    }, []);

    const submit = async () => {

        const response =
            await fetch(
                "http://localhost:8080/api/reverse-target",
                {

                    method: "POST",

                    headers: {

                        "Content-Type":
                            "application/json"

                    },

                    body: JSON.stringify(target)

                });

        alert(
            await response.text()
        );

        window.location.reload();

    };

    if (isAdmin) {

        return (

            <div className="app-container">

                <div className="form-card">

                    <h1>

                        🎯 Reverse Targets

                    </h1>

                    <table className="table">

                        <thead>

                            <tr>

                                <th>Captain</th>

                                <th>Rival Captain</th>

                                <th>Player</th>

                            </tr>

                        </thead>

                        <tbody>

                            {allTargets.map(target => (

                                <tr
                                    key={target.id}
                                >

                                    <td>

                                        {target.captainName}

                                    </td>

                                    <td>

                                        {target.rivalCaptain}

                                    </td>

                                    <td>

                                        {target.playerName}

                                    </td>

                                </tr>

                            ))}

                        </tbody>

                    </table>

                </div>

            </div>

        );

    }

    return (

        <div className="app-container">

            <div
                className="form-card"
                style={{
                    maxWidth: "700px",
                    margin: "40px auto"
                }}
            >

                <h1>

                    🎯 Reverse Target

                </h1>

                {submitted ? (

                    <>

                        <h2>

                            ✅ Already Submitted

                        </h2>

                        <p>

                            <strong>

                                Rival Captain:

                            </strong>

                            {" "}

                            {submitted.rivalCaptain}

                        </p>

                        <p>

                            <strong>

                                Player:

                            </strong>

                            {" "}

                            {submitted.playerName}

                        </p>

                    </>

                ) : (

                    <>

                        <label>

                            Rival Captain

                        </label>

                        <select
                            value={
                                target.rivalCaptain
                            }
                            onChange={event =>
                                setTarget({
                                    ...target,
                                    rivalCaptain:
                                        event.target.value
                                })
                            }
                        >

                            <option value="">

                                Select Captain

                            </option>

                            {captains
                                .filter(captain =>
                                    captain.captainName !==
                                    username
                                )
                                .map(captain => (

                                    <option
                                        key={
                                            captain.captainName
                                        }
                                        value={
                                            captain.captainName
                                        }
                                    >

                                        {captain.captainName}

                                    </option>

                                ))}

                        </select>

                        <br />

                        <br />

                        <label>

                            Player

                        </label>

                        <select
                            value={
                                target.playerName
                            }
                            onChange={event =>
                                setTarget({
                                    ...target,
                                    playerName:
                                        event.target.value
                                })
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

                        <br />

                        <br />

                        <button
                            className="button"
                            onClick={submit}
                        >

                            🎯 Submit Reverse Target

                        </button>

                    </>

                )}

            </div>

        </div>

    );

}

export default ReverseTarget;