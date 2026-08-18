import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_URL } from "../config";

function SecretTargets() {
    const username = localStorage.getItem("username");
    const role = localStorage.getItem("role");
    const [players, setPlayers] = useState([]);
    const [submitted, setSubmitted] = useState(null);
    const [targets, setTargets] = useState({ captainName: username, playerOne: "", playerTwo: "" });
    const [message, setMessage] = useState("");
    const [auctionStarted, setAuctionStarted] = useState(false);

    const loadData = () => {
        fetch(`${API_URL}/api/players`).then(response => response.json()).then(setPlayers);
        fetch(`${API_URL}/api/auction/status`)
            .then(response => response.json())
            .then(data => setAuctionStarted(data.auctionStarted))
            .catch(() => { });
        if (role === "CAPTAIN") {
            fetch(`${API_URL}/api/secret-targets/${username}`)
                .then(response => response.ok ? response.json() : null)
                .then(setSubmitted)
                .catch(() => { });
        }
    };

    useEffect(() => {
        loadData();
        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
        });
        client.onConnect = () => {
            client.subscribe("/topic/auction", () => {
                loadData();
            });
        };
        client.activate();
        return () => {
            client.deactivate();
        };
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
                {auctionStarted ? (
                    <div className="message-error" style={{ padding: "15px", marginBottom: "20px" }}>
                        🔒 Auction has started. Target selection is now locked.
                    </div>
                ) : (
                    <p>First target bought: +₹150; second target bought: +₹250; each missed target: −₹100.</p>
                )}
                {submitted ? (
                    <div className="message-success">Targets locked: {submitted.playerOne} and {submitted.playerTwo}</div>
                ) : auctionStarted ? (
                    <div className="message-error">Targets not submitted before auction started.</div>
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
                {message && <div className={message.includes("saved") ? "message-success" : "message-error"} style={{ marginTop: "15px" }}>{message}</div>}
            </div>
        </div>
    );
}

export default SecretTargets;
