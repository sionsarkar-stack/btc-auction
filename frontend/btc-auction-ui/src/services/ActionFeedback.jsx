export default function ActionFeedback({
    feedback,
    onAcknowledge,
    onRefresh,
    recoveryStatus,
    acknowledgementLabel = "I verified the current state",
}) {
    if (!feedback) {
        return null;
    }

    const className = feedback.status === "success"
        ? "message-success"
        : feedback.status === "error"
            ? "message-error"
            : "message-warning";

    return (
        <div className={className}>
            <span role={feedback.status === "error" || feedback.status === "unknown" ? "alert" : "status"}
                aria-live={feedback.status === "error" || feedback.status === "unknown" ? "assertive" : "polite"}>
                {feedback.message}
            </span>
            {feedback.status === "unknown" && (
                onRefresh ? (
                    recoveryStatus === "refreshed" ? (
                        <button className="button-secondary" type="button" onClick={onAcknowledge}>
                            {acknowledgementLabel}
                        </button>
                    ) : (
                        <button className="button-secondary" type="button" onClick={onRefresh} disabled={recoveryStatus === "refreshing"}>
                            {recoveryStatus === "refreshing" ? "Refreshing auction state..." : "Refresh auction state"}
                        </button>
                    )
                ) : (
                    <button className="button-secondary" type="button" onClick={onAcknowledge}>
                        {acknowledgementLabel}
                    </button>
                )
            )}
        </div>
    );
}