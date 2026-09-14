import { useEffect, useRef, useState } from "react";
import { API_URL } from "../config";
import { playCasinoTick, playJackpotChime } from "../services/soundService";

function CasinoReel({
    players = [],
    selectedPlayer = "",
    spinStartedAt,
    spinEndsAt,
    className = "",
    title,
    seasonName: seasonNameProp,
    onSpinComplete,
}) {
    const [rollingPlayer, setRollingPlayer] = useState("");
    const [jackpotFlash, setJackpotFlash] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [fetchedSeasonName, setFetchedSeasonName] = useState("");
    const windowRef = useRef(null);
    const itemRefs = useRef({});

    const availablePlayers = players || [];
    const spinStartTimestamp = Number(spinStartedAt);
    const spinEndTimestamp = Number(spinEndsAt);
    const hasSharedSpinWindow = Number.isFinite(spinStartTimestamp)
        && Number.isFinite(spinEndTimestamp)
        && spinStartTimestamp > 0
        && spinEndTimestamp > spinStartTimestamp;
    const isAwaitingReveal = hasSharedSpinWindow && currentTime < spinEndTimestamp;
    const isCurrentlyRolling = isAwaitingReveal && currentTime >= spinStartTimestamp;

    useEffect(() => {
        let isMounted = true;
        if (!seasonNameProp) {
            fetch(`${API_URL}/api/config`)
                .then(res => res.json())
                .then(data => {
                    if (isMounted && data?.seasonName) {
                        setFetchedSeasonName(data.seasonName);
                    }
                })
                .catch(() => { });
        }
        return () => {
            isMounted = false;
        };
    }, [seasonNameProp]);

    const activeSeasonName = seasonNameProp || fetchedSeasonName || "BTC Season 12";
    const reelHeaderTitle = title || `${activeSeasonName} Auction Reel`;

    // The server timestamps let every connected screen reveal the same nominee together.
    useEffect(() => {
        if (!hasSharedSpinWindow) {
            return undefined;
        }

        if (availablePlayers.length === 0) {
            return undefined;
        }

        let timeoutId = null;
        let isCancelled = false;

        const playerNames = availablePlayers.map(p => (typeof p === "string" ? p : p.name));

        const rollStep = () => {
            if (isCancelled) return;

            const now = Date.now();
            if (now < spinStartTimestamp) {
                timeoutId = setTimeout(rollStep, spinStartTimestamp - now);
                return;
            }

            const duration = spinEndTimestamp - spinStartTimestamp;
            const elapsed = now - spinStartTimestamp;
            const progress = Math.min(1, elapsed / duration);
            setCurrentTime(now);

            if (progress >= 1) {
                // Final lock-in
                const finalWinner = selectedPlayer || playerNames[0] || "WAITING FOR SPIN";
                setRollingPlayer(finalWinner);
                setJackpotFlash(true);
                playJackpotChime();

                if (itemRefs.current[finalWinner] && windowRef.current) {
                    itemRefs.current[finalWinner].scrollIntoView({
                        behavior: "smooth",
                        block: "nearest",
                    });
                }

                if (onSpinComplete) {
                    onSpinComplete(finalWinner);
                }

                setTimeout(() => {
                    if (!isCancelled) setJackpotFlash(false);
                }, 2500);
                return;
            }

            setJackpotFlash(false);
            const cycleProgress = 1 - Math.pow(1 - progress, 2.3);
            const cycleCount = Math.max(playerNames.length * 6, 24);
            const currentIndex = Math.floor(cycleProgress * cycleCount) % playerNames.length;
            const currentName = playerNames[currentIndex];
            setRollingPlayer(currentName);
            playCasinoTick();

            if (itemRefs.current[currentName] && windowRef.current) {
                itemRefs.current[currentName].scrollIntoView({
                    behavior: "auto",
                    block: "nearest",
                });
            }

            // Gradually ease out: starts at 35ms, slows down to ~180ms
            const nextDelay = 35 + Math.pow(progress, 2.2) * 160;
            timeoutId = setTimeout(rollStep, Math.min(nextDelay, spinEndTimestamp - now));
        };

        timeoutId = setTimeout(rollStep, Math.max(0, spinStartTimestamp - Date.now()));

        return () => {
            isCancelled = true;
            if (timeoutId) clearTimeout(timeoutId);
        };
    }, [hasSharedSpinWindow, spinStartTimestamp, spinEndTimestamp, selectedPlayer, availablePlayers.length]); // eslint-disable-line react-hooks/exhaustive-deps

    const displayedSelection = isAwaitingReveal
        ? (isCurrentlyRolling ? rollingPlayer || "SPINNING..." : "GET READY...")
        : selectedPlayer || "WAITING FOR SPIN";
    const isJackpot = jackpotFlash && !isAwaitingReveal;

    return (
        <div
            className={`casino-reel las-vegas-reel ${isCurrentlyRolling ? "spinning" : ""} ${isJackpot ? "jackpot-hit" : ""} ${className}`.trim()}
            aria-label="Las Vegas Casino Player Reel"
        >
            {/* Top Marquee Bulb Strip */}
            <div className="casino-marquee-bulbs">
                <span className="bulb bulb-1" />
                <span className="bulb bulb-2" />
                <span className="bulb bulb-3" />
                <span className="bulb bulb-4" />
                <span className="bulb bulb-5" />
                <span className="bulb bulb-6" />
                <span className="bulb bulb-7" />
                <span className="bulb bulb-8" />
            </div>

            {/* Neon Casino Sign Header */}
            <div className="casino-reel-header">
                <span className="casino-neon-crown">👑</span>
                <span className="casino-header-title">{reelHeaderTitle}</span>
                <span className="casino-neon-dice">🎲</span>
            </div>

            {/* Main Reel Casing / Slot Window */}
            <div className="casino-slot-casing">
                <div className="casino-payline-indicator left">▶</div>
                <div className="casino-payline-indicator right">◀</div>
                <div className="casino-glass-glare" />

                <div className="casino-reel-window" ref={windowRef}>
                    {availablePlayers.map((player) => {
                        const playerName = typeof player === "string" ? player : player.name;
                        const playerSeed = typeof player === "object" ? (player.seed || player.category) : "";
                        const isSelected = displayedSelection?.toLowerCase() === playerName?.toLowerCase();

                        return (
                            <div
                                key={playerName}
                                ref={(el) => {
                                    itemRefs.current[playerName] = el;
                                }}
                                className={`casino-reel-item ${isSelected ? "selected" : ""}`}
                            >
                                <span className="reel-item-name">{playerName}</span>
                                {playerSeed && (
                                    <span className="reel-item-seed">{playerSeed}</span>
                                )}
                            </div>
                        );
                    })}

                    {availablePlayers.length === 0 && (
                        <div className="casino-reel-item empty">No available players in wheel</div>
                    )}
                </div>
            </div>

            {/* Las Vegas Jackpot Winner Board */}
            <div className={`casino-reel-result ${isJackpot ? "jackpot-win" : ""}`}>
                <div className="casino-result-kicker">
                    {isCurrentlyRolling ? "⚡ ROLLING REEL..." : isAwaitingReveal ? "🎲 REEL READY..." : isJackpot ? "💎 JACKPOT NOMINEE 💎" : "SELECTED NOMINEE"}
                </div>
                <div className="casino-result-name">
                    {displayedSelection}
                </div>
            </div>
        </div>
    );
}

export default CasinoReel;
