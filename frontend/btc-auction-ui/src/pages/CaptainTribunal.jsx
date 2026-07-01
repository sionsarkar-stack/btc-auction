import { useEffect, useState } from "react";

import { API_URL } from "../config";

function CaptainTribunal() {

    const username = localStorage.getItem("username");
    const role = localStorage.getItem("role");

    const [captains, setCaptains] = useState([]);
    const [players, setPlayers] = useState([]);

    const [trustedCaptain, setTrustedCaptain] = useState("");

    const [votes, setVotes] = useState({});

    const [message, setMessage] = useState("");

    const [loading, setLoading] = useState(true);
    const [status, setStatus] = useState([]);

    useEffect(() => {

        loadData();

        if (role === "ADMIN") {

            const interval = setInterval(
                loadData,
                5000
            );

            return () => clearInterval(interval);

        }

    }, []);

    const loadData = async () => {

        try {

            const captainResponse =
                await fetch(
                    `${API_URL}/api/teams`
                );

            const statusResponse =
                await fetch(
                    `${API_URL}/api/tribunal/status`
                );

            const statusData =
                await statusResponse.json();

            setStatus(statusData);

            const captainData =
                await captainResponse.json();

            setCaptains(captainData);

            const playerResponse =
                await fetch(
                    `${API_URL}/api/players/available`
                );

            const playerData =
                await playerResponse.json();

            setPlayers(playerData);

        }

        catch (error) {

            console.error(error);

            setMessage(
                "Unable to load tribunal."
            );

        }

        finally {

            setLoading(false);

        }

    };

    const submitTribunal = async () => {

        if (!trustedCaptain) {

            setMessage(
                "Please select a Trusted Captain."
            );

            return;
        }

        if (
            Object.keys(votes).length !==
            captains.length - 1
        ) {

            setMessage(
                "Please vote against every rival captain."
            );

            return;
        }

        if (
            Object.values(votes).some(v => !v)
        ) {

            setMessage(
                "Please select a player for every rival captain."
            );

            return;
        }

        const selectedPlayers =
            Object.values(votes);

        const uniquePlayers =
            new Set(selectedPlayers);

        if (
            selectedPlayers.length !==
            uniquePlayers.size
        ) {

            setMessage(
                "You cannot select the same player for multiple rival captains."
            );

            return;
        }

        try {

            await fetch(
                `${API_URL}/api/tribunal/trusted`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({

                        captainName:
                            username,

                        trustedCaptain

                    })
                }
            );

            for (const targetCaptain in votes) {

                await fetch(
                    `${API_URL}/api/tribunal/vote`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/json"
                        },
                        body: JSON.stringify({

                            votingCaptain:
                                username,

                            targetCaptain,

                            playerName:
                                votes[targetCaptain]

                        })
                    }
                );

            }

            setMessage(
                "Captain Tribunal Submitted Successfully."
            );

            await loadData();

        }

        catch (error) {

            console.error(error);

            setMessage(
                "Unable to submit tribunal."
            );

        }

    };

    if (loading) {

        return <div>Loading...</div>;

    }

    const myStatus =
        status.find(
            s => s.captainName === username
        );

    const generateTribunal = async () => {

        try {

            const response =
                await fetch(
                    `${API_URL}/api/tribunal/generate`,
                    {
                        method: "POST"
                    }
                );

            const result =
                await response.text();

            setMessage(result);

            loadData();

        }

        catch (error) {

            console.error(error);

        }

    };

    const allSubmitted =
        status.length > 0 &&
        status.every(c => c.submitted);

    return (

        <div className="app-container">

            <div className="page-header">

                <h1 className="page-title">

                    ⚖️ Captain Tribunal

                </h1>

                <p>

                    Select one Trusted Captain and
                    choose one player against each rival.

                </p>

            </div>

            {role === "CAPTAIN" && (

                <>

                    <div className="form-card">

                        <h2>

                            Trusted Captain

                        </h2>

                        <select
                            className="select"
                            disabled={myStatus?.submitted}
                            value={trustedCaptain}
                            onChange={(e) =>
                                setTrustedCaptain(
                                    e.target.value
                                )
                            }
                        >

                            <option value="">
                                Select Trusted Captain
                            </option>

                            {captains
                                .filter(c =>
                                    c.captainName !== username
                                )
                                .map(c => (

                                    <option
                                        key={c.captainName}
                                        value={c.captainName}
                                    >

                                        {c.captainName}

                                    </option>

                                ))}

                        </select>

                    </div>

                    <div className="form-card">

                        <h2>

                            Vote Against Rival Captains

                        </h2>

                        {captains
                            .filter(c =>
                                c.captainName !== username
                            )
                            .map(captain => (

                                <div
                                    key={captain.captainName}
                                    className="form-field"
                                >

                                    <label htmlFor={`vote-${captain.captainName}`}>
                                        Against {captain.captainName}
                                    </label>

                                    <select
                                        id={`vote-${captain.captainName}`}
                                        className="select"
                                        disabled={myStatus?.submitted}
                                        value={votes[captain.captainName] || ""}
                                        onChange={(e) =>
                                            setVotes({
                                                ...votes,
                                                [captain.captainName]: e.target.value
                                            })
                                        }
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

                                </div>

                            ))}

                    </div>

                    <button
                        className="button"
                        disabled={myStatus?.submitted}
                        onClick={submitTribunal}
                    >

                        {myStatus?.submitted
                            ? "Tribunal Submitted ✅"
                            : "Submit Tribunal"}

                    </button>

                </>

            )}

            {role === "ADMIN" && (

                <>

                    <div className="form-card">

                        <h2>
                            Tribunal Status
                        </h2>

                        {status.map(captain => (

                            <div
                                key={captain.captainName}
                                className="activity-item"
                                style={{
                                    borderLeft:
                                        captain.submitted
                                            ? "5px solid green"
                                            : "5px solid orange"
                                }}
                            >

                                <h3>

                                    {captain.captainName}

                                    {" "}

                                    {captain.submitted
                                        ? "✅"
                                        : "⏳"}

                                </h3>

                                <p>

                                    <strong>

                                        Trusted Captain:

                                    </strong>

                                    {" "}

                                    {captain.trustedCaptain || "-"}

                                </p>

                                <p>

                                    <strong>

                                        Votes Submitted:

                                    </strong>

                                    {" "}

                                    {captain.votes.length}/3

                                </p>

                                <strong>

                                    Votes

                                </strong>

                                <ul>

                                    {captain.votes.map(vote => (

                                        <li
                                            key={
                                                vote.targetCaptain
                                            }
                                        >

                                            Against

                                            {" "}

                                            {vote.targetCaptain}

                                            {" → "}

                                            {vote.playerName}

                                        </li>

                                    ))}

                                </ul>

                            </div>

                        ))}

                    </div>

                    <div className="form-card">

                        <button
                            className="button-secondary"
                            onClick={loadData}
                        >

                            Refresh

                        </button>
                        <button
                            className="button"
                            disabled={!allSubmitted}
                            onClick={generateTribunal}
                        >

                            Generate Tribunal

                        </button>

                        {!allSubmitted && (

                            <p>

                                Waiting for all captains to submit.

                            </p>

                        )}

                    </div>

                </>

            )}

            {message && (

                <div
                    className="message-success"
                >

                    {message}

                </div>

            )}

        </div>

    );

}

export default CaptainTribunal;