import { useEffect, useState } from "react";
import { API_URL } from "../config";

function SecretTargets() {
    const username = localStorage.getItem("username");
    const role = localStorage.getItem("role");
    const [players, setPlayers] = useState([]);
    const [submitted, setSubmitted] = useState(null);
    const [targets, setTargets] = useState({ captainName: username, playerOne: "", playerTwo: "" });
    const [message, setMessage] = useState("");

    useEffect(() => {
        fetch(`${API_URL}/api/players`).then(response => response.json()).then(setPlayers);
        if (role === "CAPTAIN") {
            fetch(`${API_URL}/api/secret-targets/${username}`)
                .then(response => response.ok ? response.json() : null)
                .then(setSubmitted)
                .catch(() => {});
        }
    }, [role, username]);

    const submit = async () => {
        const response = await fetch(`${API_URL}/api/secret-targets`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(targets)
        });
        const result = await response.text();
        setMessage(result);
        if (response.ok && result === "Secret targets saved.") setSubmitted(targets);
    };

    if (role !== "CAPTAIN") return null;

    return (
        <div className="app-container">
            <div className="form-card" style={{ maxWidth: "700px", margin: "40px auto" }}>
                <h1>🎯 Secret Targets — Double Down</h1>
                <p>Choose two players before the auction. Both bought: +₹400; one: +₹50 net; neither: −₹200.</p>
                {submitted ? (
                    <div className="message-success">Targets locked: {submitted.playerOne} and {submitted.playerTwo}</div>
                ) : (
                    <>
                        {["playerOne", "playerTwo"].map((field, index) => (
                            <div className="form-field" key={field}>
                                <label>Target Player {index + 1}</label>
                                <select className="input" value={targets[field]}
                                    onChange={event => setTargets({ ...targets, [field]: event.target.value })}>
                                    <option value="">Select Player</option>
                                    {players.map(player => <option key={player.id} value={player.name}>{player.name}</option>)}
                                </select>
                            </div>
                        ))}
                        <button className="button" onClick={submit}>Lock Secret Targets</button>
                    </>
                )}
                {message && <div className="message-success">{message}</div>}
            </div>
        </div>
    );
}

export default SecretTargets;
