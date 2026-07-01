import { useEffect, useState } from "react";

import { API_URL } from "../config";

function AdminLogs() {

    const [logs, setLogs] = useState([]);

    useEffect(() => {

        fetch(`${API_URL}/api/admin/logs`)
            .then(response => response.json())
            .then(data => setLogs(data));

    }, []);

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    Admin Audit Logs
                </h1>
            </div>

            <div className="card-list">

                {logs.length === 0 ? (
                    <p>No audit logs found.</p>
                ) : (
                    logs
                        .slice()
                        .reverse()
                        .map(log => (

                            <div
                                key={log.id}
                                className="section-card"
                            >

                                <h3>
                                    {log.playerName}
                                </h3>

                                <p>
                                    <strong>Action:</strong>{" "}
                                    {log.actionType}
                                </p>

                                <p>
                                    <strong>Transfer:</strong>{" "}
                                    {log.oldCaptain}
                                    {" → "}
                                    {log.newCaptain}
                                </p>

                                <p>
                                    <strong>Price:</strong>{" "}
                                    ₹{log.oldPrice}
                                    {" → "}
                                    ₹{log.newPrice}
                                </p>

                                <p>
                                    <strong>Reason:</strong>{" "}
                                    {log.reason}
                                </p>

                                <p>
                                    <strong>Time:</strong>{" "}
                                    {log.timestamp}
                                </p>

                            </div>
                        ))
                )}

            </div>

        </div>
    );
}

export default AdminLogs;