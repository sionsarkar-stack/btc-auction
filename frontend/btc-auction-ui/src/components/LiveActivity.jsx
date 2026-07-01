import { useEffect, useState, useRef } from "react";
import EventOverlay from "./EventOverlay";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

function LiveActivity() {

    const [events, setEvents] = useState([]);

    const [overlayEvent, setOverlayEvent] = useState(null);

    const lastEventId = useRef(null);

    const overlayTimer = useRef(null);

    useEffect(() => {

        loadEvents();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS("http://localhost:8080/ws"),
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

        fetch("http://localhost:8080/api/events")
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

                            case "JOKER_USED":

                                if ("vibrate" in navigator) {

                                    navigator.vibrate([300, 150, 300]);

                                }
                                new Audio("/sounds/joker.mp3").play().catch(() => { });

                                break;

                            case "BOUNTY":

                                navigator.vibrate?.(200);
                                new Audio("/sounds/bounty.mp3").play().catch(() => { });

                                break;

                            case "GOLDEN_BOUNTY":

                                navigator.vibrate?.([200, 100, 200]);
                                new Audio("/sounds/golden.mp3").play().catch(() => { });

                                break;

                            case "RTM_CLAIMED":

                                navigator.vibrate?.([400]);
                                new Audio("/sounds/rtm.mp3").play().catch(() => { });

                                break;

                            case "LAST_STRIKE":
                                navigator.vibrate?.([150, 100, 150]);
                                new Audio("/sounds/strike.mp3").play().catch(() => { });
                                break;

                            case "PLAYER_VETOED":
                                navigator.vibrate?.([500]);
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
        JOKER_USED: "🃏 Joker Activated",
        RTM_CLAIMED: "🔄 RTM Claimed",
        LAST_STRIKE: "⚡ Last Strike",
        PLAYER_VETOED: "❌ Nomination Vetoed",
        BOUNTY: "🎁 Bounty",
        GOLDEN_BOUNTY: "🏆 Golden Bounty"
    };

    const overlayEvents = {
        JOKER_USED: true,
        BOUNTY: true,
        GOLDEN_BOUNTY: true,
        RTM_CLAIMED: true,
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
                                {event.amount > 0 && ` ₹${event.amount}`}
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