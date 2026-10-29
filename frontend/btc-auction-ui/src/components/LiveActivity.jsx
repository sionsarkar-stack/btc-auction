import { useEffect, useEffectEvent, useRef, useState } from "react";
import EventOverlay from "./EventOverlay";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import { eventDetails } from "../services/eventDetails";

const eventNames = {
    RTM_CLAIMED: "🔄 RTM Claimed",
    RTM_TRIGGERED: "🔄 RTM Pressed",
    RTM_ACCEPTED: "✅ RTM Accepted",
    RTM_DECLINED: "❌ RTM Declined",
    RTM_LOCKDOWN: "🚫 NO MERCY",
    PLAYER_SOLD: "🏆 Player Sold",
    PURSE_FLUSH: "💸 Auction Purse Flush",
    MARKET_CRASH: "📉 Market Crash",
    STARTING_BID_SUBMITTED: "🎯 Starting Bid Submitted",
    STARTING_BID_WINNER: "🏆 Highest Starting Bid",
    CURSE_REVEALED: "🎯 Curse Triggered",
    CURSE_CANCELLED: "🎯 Curse Cancelled",
    BLIND_BID_BONUS: "🏆 Blind Bid Bonus",
    BLIND_BONANZA: "🎊 Blind Bonanza",
    BLIND_BONANZA_REWARDED: "🎊 Blind Bonanza Award",
    SILENT_BID_SOLD: "🤐 Silent Auction Winner",
    WILDCARD_TRIGGERED: "🃏 Wildcard Triggered",
};

const overlayEvents = {
    RTM_CLAIMED: true,
    RTM_TRIGGERED: true,
    RTM_ACCEPTED: true,
    RTM_DECLINED: true,
    RTM_LOCKDOWN: true,
    PLAYER_SOLD: true,
    PURSE_FLUSH: true,
    MARKET_CRASH: true,
    STARTING_BID_WINNER: true,
    CURSE_REVEALED: true,
    CURSE_CANCELLED: true,
    BLIND_BID_BONUS: true,
    BLIND_BONANZA: true,
    BLIND_BONANZA_REWARDED: true,
    SILENT_BID_SOLD: true,
    WILDCARD_TRIGGERED: true
};

const formatEventAmount = event => {
    if (event.amount === null || event.amount === undefined || event.amount === 0) {
        return "";
    }

    if (["CURSE_REVEALED", "CURSE_CANCELLED"].includes(event.eventType)) {
        return ` ₹${event.amount} curse adjustment`;
    }

    if (["BLIND_BID_BONUS", "BLIND_BONANZA_REWARDED"].includes(event.eventType)) {
        return ` ₹${event.amount} effective-price discount`;
    }

    if (event.eventType === "PURSE_FLUSH") {
        return ` ₹${event.amount} auction purse for every captain`;
    }

    if (event.eventType === "BLIND_BONANZA") {
        return ` Up to ₹${event.amount} effective-price discount`;
    }

    return ` Final ₹${event.amount > 0 ? "+" : ""}${event.amount}`;
};

const lastDisplayedEventStorageKey = "btc-auction:last-displayed-event-id";

function LiveActivity({ showActivity = true }) {

    const [events, setEvents] = useState([]);

    const [overlayEvent, setOverlayEvent] = useState(null);

    const lastEventId = useRef(
        window.localStorage.getItem(lastDisplayedEventStorageKey)
    );

    const overlayTimer = useRef(null);

    const isActive = useRef(false);

    const eventRequestControllers = useRef(new Set());

    const dismissOverlay = () => {
        clearTimeout(overlayTimer.current);
        setOverlayEvent(null);
    };

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

                if (latest && String(latest.id) !== String(lastEventId.current)) {
                    lastEventId.current = String(latest.id);
                    window.localStorage.setItem(lastDisplayedEventStorageKey, String(latest.id));

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

            <EventOverlay event={overlayEvent} onDismiss={dismissOverlay} />

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
                                {formatEventAmount(event)}
                            </div>

                            <br />

                            <small>

                                {eventDetails(event)}

                            </small>

                        </div>

                    ))

                )}

            </div>}

        </>

    );

}

export default LiveActivity;