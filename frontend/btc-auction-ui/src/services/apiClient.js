import { API_URL } from "../config";

export class ApiError extends Error {
    constructor(message, status = 0, options = {}) {
        super(message, options);
        this.name = "ApiError";
        this.status = status;
    }
}

export async function apiRequest(path, { responseType = "auto", ...options } = {}) {
    let response;
    try {
        response = await fetch(`${API_URL}${path}`, {
            credentials: "include",
            ...options,
        });
    } catch (error) {
        if (error.name === "AbortError") {
            throw error;
        }
        throw new ApiError("Unable to reach the auction server. Check your connection and retry.", 0, { cause: error });
    }

    if (response.status === 401) {
        window.dispatchEvent(new Event("auction-session-expired"));
        throw new ApiError("Your session expired. Sign in again to continue.", response.status);
    }

    const contentType = response.headers.get("content-type") || "";
    let body = null;
    if (response.status !== 204) {
        if (responseType === "text") {
            body = await response.text();
        } else if (contentType.includes("application/json")) {
            try {
                body = await response.json();
            } catch (error) {
                throw new ApiError("The server returned an invalid response. Retry the request.", response.status, { cause: error });
            }
        } else {
            body = await response.text();
        }
    }

    if (!response.ok) {
        const message = typeof body === "string"
            ? body
            : body?.message || body?.error || `The request failed (HTTP ${response.status}).`;
        throw new ApiError(message, response.status);
    }

    return body;
}
