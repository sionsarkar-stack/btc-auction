import { useCallback, useEffect, useState } from "react";

import { API_URL } from "../config";
import { showToast } from "../services/toast";

const protectionActions = [
    "PROTECTION_PLAYER_SELECTED",
    "PROTECTION_BOOST",
    "PROTECTION_PENALTY",
    "PROTECTION_BONUS",
];

const valueBetActions = [
    "VALUE_BET_SUBMITTED",
    "VALUE_BET_REWARD",
];

function LogEntry({ log }) {
    return (
        <article className="admin-log-entry">
            <div className="admin-log-entry-heading">
                <strong>{log.actionType}</strong>
                <time>{log.timestamp}</time>
            </div>
            <p className="admin-log-player">{log.playerName || "Auction action"}</p>
            {log.newCaptain && <p><strong>Captain:</strong> {log.newCaptain}</p>}
            {log.oldCaptain && <p><strong>Transfer:</strong> {log.oldCaptain} → {log.newCaptain}</p>}
            {(log.oldPrice || log.newPrice) ? (
                <p><strong>Price:</strong> ₹{log.oldPrice} → ₹{log.newPrice}</p>
            ) : null}
            {log.reason && <p className="admin-log-reason">{log.reason}</p>}
        </article>
    );
}

function LogSection({ title, icon, logs, emptyMessage }) {
    return (
        <section className="admin-log-section">
            <div className="admin-log-section-heading">
                <h2>{icon} {title}</h2>
                <span>{logs.length}</span>
            </div>
            {logs.length === 0 ? (
                <p className="table-empty">{emptyMessage}</p>
            ) : (
                <div className="admin-log-list">
                    {logs.map(log => <LogEntry key={log.id} log={log} />)}
                </div>
            )}
        </section>
    );
}

function AdminLogs() {
    const [logs, setLogs] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    const loadLogs = useCallback(async () => {
        setIsLoading(true);
        setError("");

        try {
            const response = await fetch(`${API_URL}/api/admin/logs`, {
                credentials: "include"
            });
            if (!response.ok) throw new Error("Unable to load audit logs");
            const data = await response.json();
            setLogs(data.slice().reverse());
        } catch (loadError) {
            console.error(loadError);
            setError("Unable to load audit logs. Check the connection and try again.");
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const initialLoad = window.setTimeout(() => {
            void loadLogs();
        }, 0);
        const interval = window.setInterval(() => {
            void loadLogs();
        }, 5000);
        return () => {
            window.clearTimeout(initialLoad);
            window.clearInterval(interval);
        };
    }, [loadLogs]);

    const clearLogs = async () => {
        if (!window.confirm("Clear all admin logs for the current auction?")) return;

        const response = await fetch(`${API_URL}/api/admin/logs/clear`, {
            method: "POST",
            credentials: "include"
        });
        const result = await response.text();
        showToast(result, response.ok ? "success" : "error");
        if (response.ok) setLogs([]);
    };

    const protectionLogs = logs.filter(log => protectionActions.includes(log.actionType));
    const valueBetLogs = logs.filter(log => valueBetActions.includes(log.actionType));
    const generalLogs = logs.filter(log => !protectionActions.includes(log.actionType)
        && !valueBetActions.includes(log.actionType));

    return (
        <div className="app-container">
            <div className="page-header admin-log-header">
                <div>
                    <h1 className="page-title">Admin Audit Logs</h1>
                    <p className="admin-log-subtitle">Live history for the current auction</p>
                </div>
                <div className="button-group admin-log-actions">
                    <button className="button-secondary" type="button" onClick={loadLogs} disabled={isLoading}>
                        {isLoading ? "Refreshing..." : "Refresh logs"}
                    </button>
                    <button className="button-secondary danger-action" type="button" onClick={clearLogs}>
                        Clear logs
                    </button>
                </div>
            </div>

            {error && <div className="message-error">{error}</div>}
            {isLoading && logs.length === 0 && !error ? <p>Loading audit logs...</p> : null}

            <div className="admin-log-sections">
                <LogSection title="Protection Players" icon="🛡️" logs={protectionLogs} emptyMessage="No protection-player activity yet." />
                <LogSection title="Value Bets" icon="💰" logs={valueBetLogs} emptyMessage="No Value Bet activity yet." />
                <LogSection title="General Auction Logs" icon="📋" logs={generalLogs} emptyMessage="No general admin activity yet." />
            </div>
        </div>
    );
}

export default AdminLogs;
