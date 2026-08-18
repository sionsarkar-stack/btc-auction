import { useEffect, useState } from "react";

import { API_URL } from "../config";

function NominatePlayer() {

    const [players, setPlayers] = useState([]);
    const [selectedPlayer, setSelectedPlayer] = useState("");
    const [message, setMessage] = useState(null);
    const [nominationAnnouncement, setNominationAnnouncement] = useState(null);
    const [isNominating, setIsNominating] = useState(false);
    const [currentAuction, setCurrentAuction] = useState(null);

    const username = localStorage.getItem("username");

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

    const loadCurrentAuction = async () => {
        try {
            const response = await fetch(`${API_URL}/api/auction/current`);
            setCurrentAuction(await response.json());
        } catch (error) {
            console.error(error);
        }
    };

    useEffect(() => {

        loadPlayers();
        loadCurrentAuction();

    }, []);




    const nominatePlayer = async (selectedPlayerName = selectedPlayer) => {

        const player = players.find(
            (p) => p.name === selectedPlayerName
        );

        if (!player) {

            setMessage(
                "Please select a player."
            );

            return;
        }

        try {
            setSelectedPlayer(selectedPlayerName);
            setIsNominating(true);

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

                        captainName: username

                    }),

                }

            );

            const result =
                await response.text();

            setMessage(result);

            if (result === "Player nominated") {
                const nominatedBy = localStorage.getItem("username") || "Unknown captain";
                setNominationAnnouncement({
                    playerName: player.name,
                    basePrice: player.basePrice,
                    nominatedBy
                });
                navigator.vibrate?.([120, 80, 220]);
                window.setTimeout(() => setNominationAnnouncement(null), 4500);

                setSelectedPlayer("");

                await loadPlayers();
                await loadCurrentAuction();
            }

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to nominate player."
            );

        } finally {
            setIsNominating(false);

        }
    };

    const cancelNomination = async () => {
        if (!window.confirm(`Cancel your nomination for ${currentAuction.currentPlayer}?`)) {
            return;
        }

        try {
            const response = await fetch(
                `${API_URL}/api/auction/cancel-nomination?captainName=${encodeURIComponent(username)}`,
                { method: "POST" }
            );

            setMessage(await response.text());
            await loadCurrentAuction();
            await loadPlayers();
        } catch (error) {
            console.error(error);
            setMessage("Unable to cancel nomination.");
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

                <div className="nomination-board-heading">
                    <div>
                        <p className="nomination-eyebrow">LIVE NOMINATION BOARD</p>
                        <h2>Select Player</h2>
                    </div>
                    <div className="nomination-count">
                        <strong>{players.length}</strong>
                        <span>available</span>
                    </div>
                </div>

                <div className="nomination-instruction">
                    <span>01</span>
                    <p>Choose a player card to nominate them instantly into the auction.</p>
                    <span className="nomination-pulse" />
                </div>

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


                        <div className="player-grid">

                            {players.map((player) => {

                                return (

                                    <div
                                        key={player.name}
                                        className={`player-card ${selectedPlayer === player.name
                                            ? "selected"
                                            : ""} ${isNominating && selectedPlayer === player.name
                                                ? "nominating"
                                                : ""}`}
                                        role="button"
                                        tabIndex={0}
                                        aria-label={`Nominate ${player.name} for base price ${player.basePrice}`}
                                        onKeyDown={(event) => {
                                            if (event.key === "Enter" || event.key === " ") {
                                                event.preventDefault();
                                                nominatePlayer(player.name);
                                            }
                                        }}
                                        onClick={() => nominatePlayer(player.name)}
                                    >

                                        <span className="player-card-index">
                                            {String(players.indexOf(player) + 1).padStart(2, "0")}
                                        </span>

                                        <div className="player-name">

                                            {player.name}

                                        </div>

                                        <div className="player-seed">

                                            {player.seed || "Unseeded"} · Base ₹{player.basePrice}

                                        </div>
                                        {isNominating && selectedPlayer === player.name && (

                                            <div className="selected-tag">

                                                ◌ NOMINATING...

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

                        {nominationAnnouncement && (
                            <div className="nomination-announcement" role="status">
                                <div className="nomination-announcement-title">
                                    ✅ PLAYER NOMINATED
                                </div>
                                <strong>{nominationAnnouncement.playerName}</strong>
                                <span>Base Price: ₹{nominationAnnouncement.basePrice}</span>
                                <span>Nominated by: {nominationAnnouncement.nominatedBy}</span>
                            </div>
                        )}

                    </div>

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

                {currentAuction?.currentPlayer &&
                    currentAuction.nominatedBy?.toLowerCase() === username?.toLowerCase() && (

                        <div style={{ marginTop: "18px" }}>
                            <button
                                className="button-secondary"
                                type="button"
                                onClick={cancelNomination}
                                disabled={isNominating}
                            >
                                ❌ CANCEL MY NOMINATION
                            </button>
                        </div>

                    )}

            </div>

        </div>
    );
}

export default NominatePlayer;
