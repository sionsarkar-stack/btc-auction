import { useEffect, useState } from "react";

import { API_URL } from "../config";

function Settings() {

    const [config, setConfig] =
        useState(null);

    const [message, setMessage] =
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
                `${API_URL}/api/auction/reset`,
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
            `${API_URL}/api/config`
        )
            .then(res => res.json())
            .then(data => setConfig(data));

    }, []);

    const saveConfig = async () => {

        const response =
            await fetch(
                `${API_URL}/api/config`,
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

    if (!config) {
        return <div>Loading...</div>;
    }

    return (

        <div className="form-card">

            <h2>
                Auction Settings
            </h2>

            <h3>Protection Rules</h3>

            <div className="form-field">
                <label>Season Name</label>
                <input className="input" value={config.seasonName || ""}
                    onChange={event => setConfig({ ...config, seasonName: event.target.value })} />
            </div>

            {[
                "protectionBonus",
                "protectionPenalty"
            ].map(field => (

                <div
                    key={field}
                    className="form-field"
                >

                    <label>
                        {field === "protectionBonus"
                            ? "Protection Purse Bonus"
                            : "Protection Purse Penalty"}
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

            <hr style={{ marginTop: "30px", marginBottom: "30px" }} />

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
