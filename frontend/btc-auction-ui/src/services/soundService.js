// Web Audio API synthesizer for Las Vegas casino sound effects
let audioCtx = null;

const getAudioContext = () => {
    try {
        if (!audioCtx) {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                audioCtx = new AudioContextClass();
            }
        }
        if (audioCtx && audioCtx.state === "suspended") {
            audioCtx.resume();
        }
        return audioCtx;
    } catch {
        return null;
    }
};

export const playCasinoTick = () => {
    try {
        const ctx = getAudioContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = "triangle";
        osc.frequency.setValueAtTime(950, now);
        osc.frequency.exponentialRampToValueAtTime(200, now + 0.035);

        gain.gain.setValueAtTime(0.09, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.035);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.035);
    } catch {
        // ignore audio errors
    }
};

export const playJackpotChime = () => {
    try {
        const ctx = getAudioContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const notes = [523.25, 659.25, 783.99, 1046.50, 1318.51]; // C5, E5, G5, C6, E6

        notes.forEach((freq, index) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            const noteStart = now + index * 0.065;
            const noteEnd = noteStart + 0.4;

            osc.type = "triangle";
            osc.frequency.setValueAtTime(freq, noteStart);

            gain.gain.setValueAtTime(0.16, noteStart);
            gain.gain.exponentialRampToValueAtTime(0.001, noteEnd);

            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.start(noteStart);
            osc.stop(noteEnd);
        });
    } catch {
        // ignore audio errors
    }
};

export const calculateRollDuration = (playerCount) => {
    const maxPlayers = 35;
    const maxDuration = 3000; // maximum 3 seconds
    const minDuration = 800;  // minimum duration for fewer players
    if (!playerCount || playerCount <= 1) return minDuration;
    if (playerCount >= maxPlayers) return maxDuration;
    return Math.round(minDuration + ((playerCount - 1) / (maxPlayers - 1)) * (maxDuration - minDuration));
};
