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
        <div className="form-card">
            <h2>Captain Management</h2>
            <p>Add, update, or delete captain accounts and their starting points.</p>

            <div className="form-card" style={{ marginBottom: "16px" }}>
                <h3>Add Captain</h3>
                <div className="form-field">
                    <label>Captain Name</label>
                    <input className="input" value={newCaptain.captainName}
                        onChange={event => setNewCaptain({ ...newCaptain, captainName: event.target.value })} />
                </div>
                <div className="form-field">
                    <label>Captain Account Password</label>
                    <input className="input" type="password" value={newCaptain.password}
                        onChange={event => setNewCaptain({ ...newCaptain, password: event.target.value })} />
                </div>
                <div className="form-field">
                    <label>Starting Points</label>
                    <input className="input" type="number" min="0" value={newCaptain.totalPoints}
                        onChange={event => setNewCaptain({ ...newCaptain, totalPoints: event.target.value })} />
                </div>
                <button className="button" type="button" onClick={addCaptain}
                    disabled={!newCaptain.captainName || !newCaptain.password || newCaptain.totalPoints === ""}>
                    Create Captain
                </button>
            </div>

            {captains.map((captain, index) => (
                <div className="form-card" key={captain.currentName || index} style={{ marginBottom: "16px" }}>
                    <div className="form-field">
                        <label>Captain Name</label>
                        <input className="input" value={captain.captainName || ""}
                            onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                itemIndex === index ? { ...item, captainName: event.target.value } : item
                            ))} />
                    </div>
                    <div className="form-field">
                        <label>Captain Account Password</label>
                        <input className="input" type="password" placeholder="Leave blank to keep current password"
                            value={captain.password || ""}
                            onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                itemIndex === index ? { ...item, password: event.target.value } : item
                            ))} />
                    </div>
                    <div className="form-field">
                        <label>Starting Points</label>
                        <input className="input" type="number" min="0" value={captain.totalPoints ?? 0}
                            onChange={event => setCaptains(previous => previous.map((item, itemIndex) =>
                                itemIndex === index ? { ...item, totalPoints: event.target.value } : item
                            ))} />
                    </div>
                    <div className="button-group">
                        <button className="button" type="button" onClick={() => updateCaptain(captain, index)}>
                            Save Captain
                        </button>
                        <button className="button-secondary" type="button" onClick={() => deleteCaptain(captain.currentName)}>
                            Delete Captain
                        </button>
                    </div>
                </div>
            ))}

            {message && <div className="message-success">{message}</div>}
        </div>
    );
}

export default CaptainManagement;
