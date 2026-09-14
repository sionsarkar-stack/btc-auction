import { useEffect } from "react";

function EventOverlay({ event }) {

    let icon = "📣";
    let title = "AUCTION UPDATE";
    let message;
    let captain = event?.captainName
        ? `🔥 ${event.captainName.toUpperCase()} 🔥`
        : "";

    switch (event?.eventType) {

        case "TRIBUNAL_RESULT":

            icon = "⚖️";
            title = "CAPTAIN TRIBUNAL";
            captain = `👑 ${event.captainName.toUpperCase()} 👑`;
            message = `${event.playerName.toUpperCase()} IS BANNED`;

            break;

        case "PLAYER_SOLD":

            icon = "🏆";
            title = "PLAYER SOLD";
            message = `${event.playerName} · FINAL ₹${event.amount}${event.details ? ` · ${event.details}` : ""}`;
            break;

        case "PROTECTION_REVEALED":

            icon = "🛡️";
            title = "PROTECTION REVEALED";
            message = `${event.playerName} · ${event.details || "Protection reward settled"}`;
            break;

        case "STARTING_BID_WINNER":

            icon = "🎯";
            title = "HIGHEST STARTING BID";
            message = `${event.captainName} · ₹${event.amount}`;
            break;

        case "VALUE_BET_REWARD":

            icon = "💰";
            title = "VALUE BET WINNER";
            message = `${event.captainName} · +₹${event.amount}`;
            break;

        case "VALUE_BET":

            icon = "💰";
            title = "VALUE BET ENABLED";
            message = `${event.playerName || "Next player"} · Submit your prediction`;
            break;

        case "WILDCARD_TRIGGERED":

            icon = "🃏";
            title = "WILDCARD TRIGGERED";
            captain = `👑 ${event.captainName?.toUpperCase() || "CAPTAIN"} 👑`;
            message = `${event.playerName} returned to the nomination lot. Admin, spin the wheel again.`;
            break;

        case "RTM_CLAIMED":
        case "RTM_TRIGGERED":

            icon = "🔄";
            title = "RTM PRESSED";
            message = `Offer ₹${event.amount} · Waiting for decision`;
            break;

        case "RTM_ACCEPTED":

            icon = "✅";
            title = "RTM ACCEPTED";
            message = `Player sold at upgraded price ₹${event.amount}`;
            break;

        case "RTM_DECLINED":

            icon = "❌";
            title = "RTM DECLINED";
            message = `Player sold at upgraded price ₹${event.amount}`;
            break;

        default:
            message = event?.details || event?.eventType || "";

    }

    // Flash screen
    useEffect(() => {
        if (!event) {
            return;
        }

        document.body.classList.add("auction-flash");

        const flashTimeout = setTimeout(() => {

            document.body.classList.remove("auction-flash");

        }, 350);

        return () => clearTimeout(flashTimeout);

    }, [event]);

    // Play sound + vibrate
    useEffect(() => {
        if (!event) {
            return;
        }

        let sound = "";

        switch (event.eventType) {
            case "RTM_CLAIMED":
            case "RTM_TRIGGERED":
            case "RTM_ACCEPTED":
            case "RTM_DECLINED":
                sound = "/sounds/veto.mp3";
                break;

            case "PLAYER_SOLD":
                sound = "/sounds/bid-steal.mp3";
                break;

            default:
                return;

        }

        const timer = setTimeout(() => {

            const audio = new Audio(sound);

            audio.volume = 1.0;

            audio.play().catch(() => { });

            if (navigator.vibrate) {

                navigator.vibrate([300, 150, 300]);

            }

        }, 100);

        return () => clearTimeout(timer);

    }, [event]);

    if (!event) {
        return null;
    }

    return (

        <div className={`event-overlay ${event.eventType.toLowerCase()}`}>

            <div className="event-card">

                <div className="event-icon">

                    {icon}

                </div>

                <div className="event-title">

                    {title}

                </div>

                <div className="event-captain">

                    {captain}

                </div>

                <div className="event-message">

                    {message}

                </div>

            </div>

        </div>

    );

}

export default EventOverlay;