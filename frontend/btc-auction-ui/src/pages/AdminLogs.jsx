import { useCallback, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import { showToast } from "../services/toast";

function LogEntry({ log }) {
    const isCurseSelection = log.actionType === "CURSE_SELECTED";

    return (
        <article className="admin-log-entry">
            <div className="admin-log-entry-heading">
                <strong>{isCurseSelection ? "Curse Selected" : log.actionType}</strong>
                <time>{log.timestamp}</time>
            </div>
            <p className="admin-log-player">{log.playerName || "Auction action"}</p>
            {log.newCaptain && <p><strong>{isCurseSelection ? "Selected by" : "Captain:"}</strong> {log.newCaptain}</p>}
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

function CurseSelectionSection({ selections }) {
    return (
        <section className="admin-log-section">
            <div className="admin-log-section-heading">
                <h2>🎯 Curse Selections</h2>
                <span>{selections.length}</span>
            </div>
            {selections.length === 0 ? (
                <p className="table-empty">No curse players selected yet.</p>
            ) : (
                <div className="admin-log-list">
                    {selections.map(selection => (
                        <article className="admin-log-entry" key={selection.id}>
                            <div className="admin-log-entry-heading">
                                <strong>{selection.playerName}</strong>
                                <span>{selection.captainName}</span>
                            </div>
                            <p className="admin-log-reason">Selected as a curse player</p>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

function AdminLogs() {
    const [logs, setLogs] = useState([]);
    const [curseSelections, setCurseSelections] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    const loadLogs = useCallback(async (showLoading = true) => {
        if (showLoading) setIsLoading(true);
        setError("");

        try {
            const [logsResponse, curseSelectionsResponse] = await Promise.all([
                fetch(`${API_URL}/api/admin/logs`, { credentials: "include" }),
                fetch(`${API_URL}/api/curse-players/admin-selections`, { credentials: "include" }),
            ]);
            if (!logsResponse.ok || !curseSelectionsResponse.ok) {
                throw new Error("Unable to load admin logs");
            }
            const [data, selections] = await Promise.all([
                logsResponse.json(),
                curseSelectionsResponse.json(),
            ]);
            setLogs(data.slice().reverse());
            setCurseSelections(selections);
        } catch (loadError) {
            console.error(loadError);
            setError("Unable to load audit logs. Check the connection and try again.");
        } finally {
            if (showLoading) setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const initialLoad = window.setTimeout(() => {
            void loadLogs();
        }, 0);
        const interval = window.setInterval(() => {
            void loadLogs(false);
        }, 30000);
        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
        });
        client.onConnect = () => {
            client.subscribe("/topic/auction", () => {
                void loadLogs(false);
            });
        };
        client.activate();

        return () => {
            window.clearTimeout(initialLoad);
            window.clearInterval(interval);
            void client.deactivate({ force: true });
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

    const generalLogs = logs.filter(log => log.actionType !== "CURSE_SELECTED");

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
                <CurseSelectionSection selections={curseSelections} />
                <LogSection title="General Auction Logs" icon="📋" logs={generalLogs} emptyMessage="No general admin activity yet." />
            </div>
        </div>
    );
}

export default AdminLogs;
