import { useEffect, useState } from "react";

import { API_URL } from "../config";
import ActionFeedback from "../services/ActionFeedback";
import { useActionRequest } from "../services/useActionRequest";

function ManualSale() {

    const [players, setPlayers] = useState([]);
    const [teams, setTeams] = useState([]);

    const [playerName, setPlayerName] = useState("");
    const [newCaptain, setNewCaptain] = useState("");
    const [newPrice, setNewPrice] = useState("");

    const {
        feedback,
        outcomeUnknown,
        pendingAction,
        runAction,
    } = useActionRequest();
    const actionBusy = Boolean(pendingAction);

    const refreshBeforeRetry = () => window.location.reload();

    useEffect(() => {

        fetch(`${API_URL}/api/players`)
            .then(response => response.json())
            .then(data => setPlayers(data));

        fetch(`${API_URL}/api/teams`)
            .then(response => response.json())
            .then(data => setTeams(data));

    }, []);

    const applyManualSale = async () => {
        const action = await runAction(
            "Applying sale adjustment",
            `${API_URL}/api/auction/manual-sale`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ playerName, newCaptain, newPrice: Number(newPrice) }),
            },
            result => result === "Manual Sale Updated"
        );
        if (action.success) {
            setPlayerName("");
            setNewCaptain("");
            setNewPrice("");
        }
    };

    const cancelSale = async () => {
        if (!playerName || !window.confirm(`Cancel the sale for ${playerName}?`)) {
            return;
        }

        const saleToCancel = playerName;
        const action = await runAction(
            "Cancelling sale",
            `${API_URL}/api/auction/cancel-sale`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ playerName: saleToCancel }),
            },
            result => result === `Sale cancelled for ${saleToCancel}`
        );
        if (action.success) {
            const refreshed = await fetch(`${API_URL}/api/players`);
            setPlayers(await refreshed.json());
            setPlayerName("");
        }
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
                        disabled={actionBusy || outcomeUnknown}
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
                        disabled={actionBusy || outcomeUnknown}
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
                        disabled={actionBusy || outcomeUnknown}
                        onChange={(e) =>
                            setNewPrice(e.target.value)
                        }
                    />
                </div>

                <button
                    className="button"
                    onClick={applyManualSale}
                    disabled={actionBusy || outcomeUnknown || !playerName || !newCaptain || newPrice === ""}
                >
                    {pendingAction === "Applying sale adjustment" ? "Applying..." : "APPLY ADJUSTMENT"}
                </button>

                <button
                    className="button-secondary"
                    type="button"
                    onClick={cancelSale}
                    disabled={actionBusy || outcomeUnknown || !playerName || !players.find(player => player.name === playerName)?.sold}
                    style={{ marginTop: "12px" }}
                >
                    {pendingAction === "Cancelling sale" ? "Cancelling..." : "CANCEL SELECTED SALE"}
                </button>

                <ActionFeedback
                    feedback={feedback}
                    onAcknowledge={refreshBeforeRetry}
                    acknowledgementLabel="Refresh and re-enable controls"
                />

            </div>

        </div>
    );
}

export default ManualSale;