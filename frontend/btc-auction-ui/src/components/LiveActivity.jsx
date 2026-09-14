import { useEffect, useEffectEvent, useRef, useState } from "react";
import EventOverlay from "./EventOverlay";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";

const eventNames = {
    RTM_CLAIMED: "🔄 RTM Claimed",
    RTM_TRIGGERED: "🔄 RTM Pressed",
    RTM_ACCEPTED: "✅ RTM Accepted",
    RTM_DECLINED: "❌ RTM Declined",
    PLAYER_SOLD: "🏆 Player Sold",
    STARTING_BID_SUBMITTED: "🎯 Starting Bid Submitted",
    STARTING_BID_WINNER: "🏆 Highest Starting Bid",
    VALUE_BET_REWARD: "💰 Value Bet Winner",
    PROTECTION_REVEALED: "🛡️ Protection Revealed",
    SILENT_BID_SOLD: "🤐 Silent Auction Winner",
    WILDCARD_TRIGGERED: "🃏 Wildcard Triggered",
};

const overlayEvents = {
    RTM_CLAIMED: true,
    RTM_TRIGGERED: true,
    RTM_ACCEPTED: true,
    RTM_DECLINED: true,
    PLAYER_SOLD: true,
    STARTING_BID_WINNER: true,
    VALUE_BET_REWARD: true,
    PROTECTION_REVEALED: true,
    SILENT_BID_SOLD: true,
    WILDCARD_TRIGGERED: true
};

function LiveActivity({ showActivity = true }) {

    const [events, setEvents] = useState([]);

    const [overlayEvent, setOverlayEvent] = useState(null);

    const lastEventId = useRef(null);

    const overlayTimer = useRef(null);

    const isActive = useRef(false);

    const eventRequestControllers = useRef(new Set());

    const loadEvents = useEffectEvent(() => {
        if (!isActive.current) {
            return;
        }

        const controller = new AbortController();

        eventRequestControllers.current.add(controller);

        fetch(`${API_URL}/api/events`, { signal: controller.signal })
            .then(response => response.json())
            .then(data => {
                if (!isActive.current) {
                    return;
                }

                const latest =
                    data.length > 0
                        ? data[data.length - 1]
                        : null;

                if (latest && latest.id !== lastEventId.current) {
                    lastEventId.current = latest.id;

                    if (overlayEvents[latest.eventType]) {
                        switch (latest.eventType) {

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
            .catch(error => {
                if (isActive.current && error.name !== "AbortError") {
                    console.error(error);
                }
            })
            .finally(() => {
                eventRequestControllers.current.delete(controller);
            });

    });

    useEffect(() => {

        isActive.current = true;

        const stopLiveUpdates = () => {

            isActive.current = false;

            eventRequestControllers.current.forEach(controller => controller.abort());

            eventRequestControllers.current.clear();

        };

        window.addEventListener("pagehide", stopLiveUpdates);

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

            window.removeEventListener("pagehide", stopLiveUpdates);
            stopLiveUpdates();
            clearTimeout(overlayTimer.current);

            void client.deactivate({ force: true });

        };

    }, []);

    return (

        <>

            <EventOverlay event={overlayEvent} />

            {showActivity && <div className="section-card">

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

            </div>}

        </>

    );

}

export default LiveActivity;