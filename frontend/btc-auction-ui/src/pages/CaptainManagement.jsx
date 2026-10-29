import { useEffect, useState } from "react";
import { API_URL } from "../config";

function CaptainManagement() {
    const [captains, setCaptains] = useState([]);
    const [message, setMessage] = useState("");
    const [newCaptain, setNewCaptain] = useState({
        captainName: "",
        password: "",
        totalPoints: ""
    });

    const loadCaptains = async () => {
        const response = await fetch(`${API_URL}/api/admin/captains`);
        setCaptains(await response.json());
    };

    useEffect(() => {
        let cancelled = false;
        fetch(`${API_URL}/api/admin/captains`)
            .then(response => response.json())
            .then(data => {
                if (!cancelled) {
                    setCaptains(data);
                }
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const updateCaptain = async (captain, index) => {
        const response = await fetch(`${API_URL}/api/admin/captains`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentName: captain.currentName,
                captainName: captain.captainName,
                password: captain.password,
                totalPoints: Number(captain.totalPoints)
            })
        });
        setMessage(await response.text());
        if (response.ok) {
            setCaptains(previous => previous.map((item, itemIndex) =>
                itemIndex === index
                    ? { ...item, currentName: item.captainName, password: "" }
                    : item
            ));
        }
    };

    const addCaptain = async () => {
        const response = await fetch(`${API_URL}/api/admin/captains`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentName: "",
                captainName: newCaptain.captainName,
                password: newCaptain.password,
                totalPoints: Number(newCaptain.totalPoints)
            })
        });
        setMessage(await response.text());
        if (response.ok) {
            await loadCaptains();
            setNewCaptain({ captainName: "", password: "", totalPoints: "" });
        }
    };

    const deleteCaptain = async (captainName) => {
        if (!window.confirm(`Delete captain ${captainName}?`)) {
            return;
        }
        const response = await fetch(
            `${API_URL}/api/admin/captains/${encodeURIComponent(captainName)}`,
            { method: "DELETE" }
        );
        setMessage(await response.text());
        if (response.ok) {
            await loadCaptains();
        }
    };

    return (
        <div className="captain-management-page">
            <header className="captain-management-header">
                <div>
                    <span className="captain-management-kicker">TEAM ACCOUNTS</span>
                    <h2>Captain Management</h2>
                </div>
                <span className="captain-count">{captains.length} captains</span>
            </header>

            <section className="captain-create-section" aria-labelledby="captain-create-title">
                <div className="captain-section-heading">
                    <h3 id="captain-create-title">Add Captain</h3>
                </div>
                <div className="captain-create-grid">
                    <div className="form-field">
                        <label htmlFor="new-captain-name">Captain Name</label>
                        <input id="new-captain-name" className="input" value={newCaptain.captainName}
                            onChange={event => setNewCaptain({ ...newCaptain, captainName: event.target.value })} />
                    </div>
                    <div className="form-field">
                        <label htmlFor="new-captain-password">Account Password</label>
                        <input id="new-captain-password" className="input" type="password" value={newCaptain.password}
                            onChange={event => setNewCaptain({ ...newCaptain, password: event.target.value })} />
                    </div>
                    <div className="form-field">
                        <label htmlFor="new-captain-purse">Initial Auction Purse</label>
                        <div className="captain-currency-input">
                            <span aria-hidden="true">&#8377;</span>
                            <input id="new-captain-purse" type="number" min="0" step="50" value={newCaptain.totalPoints}
                                onChange={event => setNewCaptain({ ...newCaptain, totalPoints: event.target.value })} />
                        </div>
                    </div>
                    <button className="button captain-create-button" type="button" onClick={addCaptain}
                        disabled={!newCaptain.captainName || !newCaptain.password || newCaptain.totalPoints === ""}>
                        Create Captain
                    </button>
                </div>
            </section>

            <section className="captain-list-section" aria-labelledby="captain-list-title">
                <div className="captain-section-heading">
                    <h3 id="captain-list-title">Current Captains</h3>
                    <span>{captains.length}</span>
                </div>
                {captains.length === 0 ? (
                    <p className="table-empty">No captains created yet.</p>
                ) : (
                    <div className="captain-management-list">
                        {captains.map((captain, index) => (
                            <article className="captain-management-card" key={captain.currentName || index}>
                                <div className="captain-edit-grid">
                                    <div className="form-field">
                                        <label htmlFor={`captain-name-${index}`}>Captain Name</label>
                                        <input id={`captain-name-${index}`} className="input" value={captain.captainName || ""}
                                            onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                                itemIndex === index ? { ...item, captainName: event.target.value } : item
                                            ))} />
                                    </div>
                                </div>
                                <div className="captain-credentials-grid">
                                    <div className="form-field">
                                        <label htmlFor={`captain-password-${index}`}>Account Password</label>
                                        <input id={`captain-password-${index}`} className="input" type="password"
                                            placeholder="Leave blank to keep current password"
                                            value={captain.password || ""}
                                            onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                                itemIndex === index ? { ...item, password: event.target.value } : item
                                            ))} />
                                    </div>
                                    <div className="form-field">
                                        <label htmlFor={`captain-purse-${index}`}>Auction Purse</label>
                                        <div className="captain-currency-input">
                                            <span aria-hidden="true">&#8377;</span>
                                            <input id={`captain-purse-${index}`} type="number" min="0" step="50"
                                                value={captain.totalPoints ?? 0}
                                                onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                                    itemIndex === index ? { ...item, totalPoints: event.target.value } : item
                                                ))} />
                                        </div>
                                    </div>
                                </div>
                                <div className="captain-card-actions">
                                    <button className="button" type="button" onClick={() => updateCaptain(captain, index)}>
                                        Save Captain
                                    </button>
                                    <button className="button-secondary" type="button" onClick={() => deleteCaptain(captain.currentName)}>
                                        Delete Captain
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </section>

            {message && <div className="message-success">{message}</div>}
        </div>
    );
}

export default CaptainManagement;
