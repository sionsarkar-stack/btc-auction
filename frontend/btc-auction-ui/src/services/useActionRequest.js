import { useRef, useState } from "react";
import { ApiError, apiRequest } from "./apiClient";

const ACTION_TIMEOUT_MS = 20_000;

export function useActionRequest() {
    const [pendingAction, setPendingAction] = useState(null);
    const [feedback, setFeedback] = useState(null);
    const [outcomeUnknown, setOutcomeUnknown] = useState(false);
    const inFlight = useRef(false);
    const outcomeUnknownRef = useRef(false);

    const acknowledgeOutcome = () => {
        outcomeUnknownRef.current = false;
        setOutcomeUnknown(false);
        setFeedback(null);
    };

    const runAction = async (
        label,
        url,
        options,
        isSuccessful = () => true,
        successMessage = responseMessage => responseMessage
    ) => {
        if (inFlight.current || outcomeUnknownRef.current) {
            return { started: false };
        }

        inFlight.current = true;
        setPendingAction(label);
        setFeedback({ status: "pending", message: `${label} in progress...` });

        const controller = new AbortController();
        const timeoutId = window.setTimeout(() => controller.abort(), ACTION_TIMEOUT_MS);

        try {
            const message = await apiRequest(url, {
                ...options,
                signal: controller.signal,
                responseType: "text",
            });

            const success = isSuccessful(message);
            setFeedback({
                status: success ? "success" : "error",
                message: success
                    ? successMessage(message) || `${label} complete.`
                    : message || `${label} failed.`,
            });
            return { started: true, success, message };
        } catch (error) {
            if (controller.signal.aborted || error.name === "AbortError") {
                const message = "Request timed out. It may have completed; refresh and verify auction state before retrying.";
                outcomeUnknownRef.current = true;
                setOutcomeUnknown(true);
                setFeedback({ status: "unknown", message });
                return { started: true, success: false, unknown: true, message };
            }

            if (error instanceof ApiError && (error.status === 0 || error.status >= 500)) {
                const message = error.status === 0
                    ? error.message
                    : "The server could not confirm this action. It may have completed; refresh and verify state before retrying.";
                outcomeUnknownRef.current = true;
                setOutcomeUnknown(true);
                setFeedback({ status: "unknown", message });
                return { started: true, success: false, unknown: true, message };
            }

            setFeedback({ status: "error", message: error.message || `${label} failed.` });
            return { started: true, success: false, message: error.message };
        } finally {
            window.clearTimeout(timeoutId);
            setPendingAction(null);
            inFlight.current = false;
        }
    };

    return {
        acknowledgeOutcome,
        feedback,
        outcomeUnknown,
        pendingAction,
        runAction,
    };
}