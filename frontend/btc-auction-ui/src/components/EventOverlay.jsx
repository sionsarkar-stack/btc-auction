import { useEffect } from "react";

function EventOverlay({ event, onDismiss }) {

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

        case "PURSE_FLUSH":
            icon = "💸";
            title = "AUCTION PURSE REFILLED";
            message = `Every captain received ₹${event.amount} for bidding.`;
            break;

        case "CURSE_REVEALED":
            icon = "🎯";
            title = "CURSE TRIGGERED";
            message = `${event.playerName} · ${event.details || `Transfer ${event.amount} tournament points`}`;
            break;

        case "CURSE_CANCELLED":
            icon = "🎯";
            title = "CURSE CANCELLED";
            message = `${event.playerName} · ${event.details}`;
            break;

        case "BLIND_BID_BONUS":
            icon = "🏆";
            title = "BLIND BID BONUS";
            message = `${event.captainName} · +${event.amount} tournament points`;
            break;

        case "BLIND_BONANZA":
            icon = "🎊";
            title = "BLIND BONANZA IN PLAY";
            message = "Highest blind bidder can earn +500 tournament points if they buy the player.";
            break;

        case "BLIND_BONANZA_REWARDED":
            icon = "🎊";
            title = "BLIND BONANZA AWARDED";
            message = `${event.captainName} · +${event.amount} tournament points`;
            break;

        case "MARKET_CRASH":

            icon = "📉";
            title = "MARKET CRASH";
            message = `${event.playerName || "Current player"} · Price reduced when SOLD was called`;
            break;

        case "RTM_LOCKDOWN":
            icon = "🚫";
            title = "NO MERCY";
            message = event.details || "RTM and Wild Pick are banned for this player.";
            break;

        case "STARTING_BID_WINNER":

            icon = "🎯";
            title = "HIGHEST STARTING BID";
            message = `${event.captainName} · ₹${event.amount}`;
            break;

        case "STARTING_BID_TIE":

            icon = "⚖️";
            title = "OPENING BID TIE";
            message = `${event.captainName} · ₹${event.amount}. Submit again or pass.`;
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

                <button
                    className="event-overlay-dismiss"
                    type="button"
                    onClick={onDismiss}
                    aria-label="Dismiss event announcement"
                    title="Dismiss announcement"
                >
                    ×
                </button>

                <div className="event-overlay-content" role="status" aria-live="polite">
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

        </div>

    );

}

export default EventOverlay;