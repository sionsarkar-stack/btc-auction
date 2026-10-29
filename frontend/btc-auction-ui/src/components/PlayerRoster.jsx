import { useCallback, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import { apiRequest } from "../services/apiClient";

function PlayerRoster() {
    const [players, setPlayers] = useState([]);
    const [loadError, setLoadError] = useState("");

    const loadPlayers = useCallback(async () => {
        try {
            const data = await apiRequest("/api/players");
            setPlayers(Array.isArray(data) ? data : []);
            setLoadError("");
        } catch (error) {
            setLoadError(error.message || "Unable to load players.");
        }
    }, []);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => client.subscribe("/topic/auction", () => void loadPlayers()),
        });
        const initialLoad = window.setTimeout(() => void loadPlayers(), 0);
        const refreshInterval = window.setInterval(() => void loadPlayers(), 30000);
        client.activate();

        return () => {
            window.clearTimeout(initialLoad);
            window.clearInterval(refreshInterval);
            void client.deactivate();
        };
    }, [loadPlayers]);

    return (
        <section className="section-card player-roster-directory" aria-labelledby="player-roster-heading">
            <div className="player-roster-heading">
                <h2 id="player-roster-heading">Player list</h2>
                <span>{players.length} players</span>
            </div>
            {loadError ? (
                <p className="table-empty" role="alert">{loadError}</p>
            ) : players.length === 0 ? (
                <p className="table-empty" role="status">Loading player list...</p>
            ) : (
                <ul className="player-roster-grid">
                    {players.map(player => (
                        <li key={player.id || player.name}>
                            <div className="player-roster-player">
                                <strong>{player.name}</strong>
                                <span className="player-roster-base-price">₹{player.basePrice}</span>
                            </div>
                            <div className="player-roster-status">
                                {player.sold ? (
                                    <>
                                        <span className="status-badge sold">Sold</span>
                                        {player.team && <span className="player-roster-detail">{player.team} · ₹{player.finalPrice || player.soldPrice}</span>}
                                    </>
                                ) : player.deferredToReAuction ? (
                                    <span className="status-badge held">Held for re-auction</span>
                                ) : (
                                    <span className="status-badge available">Available</span>
                                )}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

export default PlayerRoster;