import { useEffect } from "react";

function EventOverlay({ event }) {

    let icon = "🃏";
    let title = "JOKER";
    let message = "";
    let captain = event?.captainName
        ? `🔥 ${event.captainName.toUpperCase()} 🔥`
        : "";

    switch (event?.eventType) {

        case "PLAYER_VETOED":

            icon = "🚫";
            title = "VETO";
            message = "NOMINATION CANCELLED";
            break;

        case "RTM_CLAIMED":

            icon = "🔄";
            title = "RIGHT TO MATCH";
            message = "Waiting for Auctioneer Approval";
            break;

        case "BOUNTY":

            icon = "🎁";
            title = "BOUNTY";
            message = "BONUS AWARDED";
            break;

        case "GOLDEN_BOUNTY":

            icon = "🏆";
            title = "GOLDEN BOUNTY";
            message = "MEGA BONUS AWARDED";
            break;

        case "LAST_STRIKE":

            icon = "⚡";
            title = "LAST STRIKE";
            message = "AUCTION REOPENED AT +₹100";
            break;

        case "JOKER_USED":

            icon = "🃏";
            title = "JOKER";
            message = event.details || "";
            break;

        default:

            icon = "🃏";
            title = "JOKER";
            message = "";

    }

    // Flash screen
    useEffect(() => {
        if (!event) {
            return;
        }

        document.body.classList.add("joker-flash");

        const flashTimeout = setTimeout(() => {

            document.body.classList.remove("joker-flash");

        }, 350);

        return () => clearTimeout(flashTimeout);

    }, []);

    // Play sound + vibrate
    useEffect(() => {
        if (!event) {
            return;
        }

        let sound = "";

        switch (event.eventType) {

            case "PLAYER_VETOED":
                sound = "/sounds/veto.mp3";
                break;

            case "RTM_CLAIMED":
                sound = "/sounds/rtm.mp3";
                break;

            case "BOUNTY":
                sound = "/sounds/bounty.mp3";
                break;

            case "GOLDEN_BOUNTY":
                sound = "/sounds/golden.mp3";
                break;

            case "LAST_STRIKE":
                sound = "/sounds/strike.mp3";
                break;

            case "JOKER_USED":
                sound = "/sounds/joker.mp3";
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

        <div className={`joker-overlay ${event.eventType.toLowerCase()}`}>

            <div className="joker-card">

                <div className="joker-icon">

                    {icon}

                </div>

                <div className="joker-title">

                    {title}

                </div>

                <div className="joker-captain">

                    {captain}

                </div>

                <div className="joker-type">

                    {message}

                </div>

            </div>

        </div>

    );

}

export default EventOverlay;