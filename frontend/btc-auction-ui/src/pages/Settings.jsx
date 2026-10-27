import { useEffect, useState } from "react";

import { API_URL } from "../config";
import ActionFeedback from "../services/ActionFeedback";
import { useActionRequest } from "../services/useActionRequest";

function Settings() {

    const [config, setConfig] =
        useState(null);

    const {
        feedback,
        outcomeUnknown,
        pendingAction,
        runAction,
    } = useActionRequest();
    const actionBusy = Boolean(pendingAction);

    const refreshBeforeRetry = () => window.location.reload();

    const resetAuction = async () => {

        if (
            !window.confirm(
                "Reset entire auction?"
            )
        ) {
            return;
        }

        await runAction(
            "Resetting auction",
            `${API_URL}/api/auction/reset`,
            { method: "POST" },
            result => result === "Auction Reset Successfully"
        );
    };

    useEffect(() => {

        fetch(
            `${API_URL}/api/config`
        )
            .then(res => res.json())
            .then(data => setConfig(data));

    }, []);

    const saveConfig = async () => {

        await runAction(
            "Saving settings",
            `${API_URL}/api/config`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(config),
            },
            result => result.trim().length > 0,
            () => "Settings saved."
        );
    };

    if (!config) {
        return <div>Loading...</div>;
    }

    return (

        <div className="form-card">

            <h2>
                Auction Settings
            </h2>

            <h3>Season Details</h3>

            <div className="form-field">
                <label>Season Name</label>
                <input className="input" value={config.seasonName || ""}
                    onChange={event => setConfig({ ...config, seasonName: event.target.value })} />
            </div>

            <button
                className="button"
                onClick={saveConfig}
                disabled={actionBusy || outcomeUnknown}
            >
                {pendingAction === "Saving settings" ? "Saving..." : "Save Settings"}
            </button>

            <button
                className="button-secondary"
                onClick={resetAuction}
                disabled={actionBusy || outcomeUnknown}
            >
                {pendingAction === "Resetting auction" ? "Resetting..." : "Reset Auction"}
            </button>

            <hr style={{ marginTop: "30px", marginBottom: "30px" }} />

            <ActionFeedback
                feedback={feedback}
                onAcknowledge={refreshBeforeRetry}
                acknowledgementLabel="Refresh and re-enable controls"
            />

        </div>

    );
}

export default Settings;
