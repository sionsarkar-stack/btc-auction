import { useEffect, useState, useRef } from "react";
import EventOverlay from "./EventOverlay";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";

function LiveActivity() {

    const [events, setEvents] = useState([]);

    const [overlayEvent, setOverlayEvent] = useState(null);

    const lastEventId = useRef(null);

    const overlayTimer = useRef(null);

    useEffect(() => {

        loadEvents();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000
        });

        client.onConnect = () => {

            client.subscribe("/topic/auction", () => {

                loadEvents();

            });

        };

        client.activate();

        return () => {

            clearTimeout(overlayTimer.current);

            client.deactivate();

        };

    }, []);

    const loadEvents = () => {

        fetch(`${API_URL}/api/events`)
            .then(response => response.json())
            .then(data => {

                const latest =
                    data.length > 0
                        ? data[data.length - 1]
                        : null;

                if (latest && latest.id !== lastEventId.current) {
                    lastEventId.current = latest.id;

                    if (overlayEvents[latest.eventType]) {
                        switch (latest.eventType) {

                            case "BOUNTY":

                                navigator.vibrate?.(200);
                                new Audio("/sounds/bounty.mp3").play().catch(() => { });

                                break;

                            case "GOLDEN_BOUNTY":

                                navigator.vibrate?.([200, 100, 200]);
                                new Audio("/sounds/last-strike.mp3").play().catch(() => { });

                                break;

                            case "RTM_CLAIMED":

                                navigator.vibrate?.([400]);
                                new Audio("/sounds/veto.mp3").play().catch(() => { });

                                break;


                            default:
                                break;
                        }

                        setOverlayEvent(latest);

                        clearTimeout(overlayTimer.current);

                        overlayTimer.current = setTimeout(() => {

                            setOverlayEvent(null);

                        }, 5000);
                    }
                }

                setEvents(
                    data
                        .slice()
                        .reverse()
                );

            })
            .catch(error =>
                console.error(error));

    };

    const eventNames = {
        RTM_CLAIMED: "🔄 RTM Claimed",
        RTM_TRIGGERED: "🔄 RTM Pressed",
        RTM_ACCEPTED: "✅ RTM Accepted",
        RTM_DECLINED: "❌ RTM Declined",
        PLAYER_SOLD: "🏆 Player Sold",
        LAST_STRIKE: "⚡ Last Strike",
        PLAYER_VETOED: "❌ Nomination Vetoed",
        BOUNTY: "🎁 Bounty",
        GOLDEN_BOUNTY: "🏆 Golden Bounty",
        SECRET_TARGET_SETTLED: "🎯 Secret Target",
        REVERSE_TARGET_TRIGGERED: "🛡️ Reverse Target"
    };

    const overlayEvents = {
        BOUNTY: true,
        GOLDEN_BOUNTY: true,
        RTM_CLAIMED: true,
        RTM_TRIGGERED: true,
        RTM_ACCEPTED: true,
        RTM_DECLINED: true,
        PLAYER_SOLD: true,
        LAST_STRIKE: true,
        PLAYER_VETOED: true
    };

    return (

        <>

            <EventOverlay event={overlayEvent} />

            <div className="section-card">

                <h2>

                    🔴 Live Activity

                </h2>

                {events.length === 0 ? (

                    <p>

                        No events yet

                    </p>

                ) : (

                    events.map(event => (

                        <div
                            key={event.id}
                            className="activity-item"
                        >

                            <strong>

                                {eventNames[event.eventType] || event.eventType}

                            </strong>

                            <br />

                            <div>
                                {event.playerName}
                                {event.captainName && ` → ${event.captainName}`}
                                {event.amount !== 0 && ` Final ₹${event.amount > 0 ? "+" : ""}${event.amount}`}
                            </div>

                            <br />

                            <small>

                                {event.details}

                            </small>

                        </div>

                    ))

                )}

            </div>

        </>

    );

}

export default LiveActivity;