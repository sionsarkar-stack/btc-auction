import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import { showToast } from "../services/toast";

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

    const [auctionStarted, setAuctionStarted] =
        useState(false);

    const loadData = () => {
        fetch(`${API_URL}/api/players`)
            .then(response => response.json())
            .then(setPlayers);

        fetch(`${API_URL}/api/teams`)
            .then(response => response.json())
            .then(setCaptains);

        fetch(`${API_URL}/api/auction/status`)
            .then(response => response.json())
            .then(data => setAuctionStarted(data.auctionStarted))
            .catch(() => { });

        if (isAdmin) {

            fetch(`${API_URL}/api/reverse-target`)
                .then(response => response.json())
                .then(setAllTargets);

        } else {

            fetch(
                `${API_URL}/api/reverse-target/${username}`
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

    }, []);

    const submit = async () => {

        const response =
            await fetch(
                `${API_URL}/api/reverse-target`,
                {

                    method: "POST",

                    headers: {

                        "Content-Type":
                            "application/json"

                    },

                    body: JSON.stringify(target)

                });

        showToast(await response.text());

        window.location.reload();

    };

    if (isAdmin) {

        return (

            <div className="app-container">

                <div className="form-card">

                    <h1>

                        🎯 Reverse Targets

                    </h1>

                    {auctionStarted && (
                        <div className="message-error" style={{ padding: "15px", marginBottom: "20px" }}>
                            🔒 Target selection locked — auction in progress.
                        </div>
                    )}

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

                {auctionStarted && (
                    <div className="message-error" style={{ padding: "15px", marginBottom: "20px" }}>
                        🔒 Auction has started. Target selection is now locked.
                    </div>
                )}

                <p style={{ marginBottom: "20px" }}>

                    If the selected rival captain buys this player,
                    ₹200 is immediately deducted from that captain's purse.

                </p>

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
                            className="input"
                            value={
                                target.rivalCaptain
                            }
                            disabled={auctionStarted || submitted}
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
                            className="input"
                            value={
                                target.playerName
                            }
                            disabled={auctionStarted || submitted}
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
                            disabled={auctionStarted || submitted}
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
