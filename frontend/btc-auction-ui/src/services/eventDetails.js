export function eventDetails(event) {
    if (event.eventType === "CURSE_REVEALED") {
        return `${event.playerName}: effective price increased by ₹${event.amount}; each curse setter receives ₹${event.amount} in their purse.`;
    }
    if (["BLIND_BID_BONUS", "BLIND_BONANZA_REWARDED"].includes(event.eventType)) {
        return `${event.captainName}: effective price reduced by ₹${event.amount}.`;
    }
    if (event.eventType === "BLIND_BONANZA") {
        return "Highest blind bidder receives a ₹500 effective-price discount if they buy the player.";
    }
    return event.details;
}
