export function showToast(message, type = "info") {
    window.dispatchEvent(
        new CustomEvent("auction-toast", {
            detail: { message, type },
        })
    );
}
