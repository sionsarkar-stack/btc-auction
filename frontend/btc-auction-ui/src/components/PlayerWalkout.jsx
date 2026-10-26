import { lazy, Suspense, useEffect, useEffectEvent, useRef, useState } from "react";
import { API_URL } from "../config";
import { playGlassBreakSound } from "../services/glassBreakSound";

const BottleDrawScene = lazy(() => import("./BottleDrawScene"));

function getPlayerName(player) {
    return typeof player === "string" ? player : player?.name || "";
}

function PlayerWalkout({
    players = [],
    walkoutPlayers,
    selectedPlayer = "",
    spinStartedAt,
    spinEndsAt,
    serverTime,
    className = "",
    seasonName = "",
    onSpinComplete,
}) {
    const [currentTime, setCurrentTime] = useState(0);
    const [rollingIndex, setRollingIndex] = useState(0);
    const [configuredSeasonName, setConfiguredSeasonName] = useState("");
    const [clockOffset, setClockOffset] = useState(0);
    const [clockReady, setClockReady] = useState(false);
    const soundPlayedForSpin = useRef(null);

    const hasAuthoritativeRoster = Boolean(walkoutPlayers?.length);
    const availablePlayers = (hasAuthoritativeRoster ? walkoutPlayers : players || [])
        .filter(player => getPlayerName(player))
        .slice();
    if (!hasAuthoritativeRoster) {
        availablePlayers.sort((first, second) => {
            const firstName = getPlayerName(first);
            const secondName = getPlayerName(second);
            const firstKey = firstName.toLowerCase();
            const secondKey = secondName.toLowerCase();
            return firstKey === secondKey
                ? firstName < secondName ? -1 : firstName > secondName ? 1 : 0
                : firstKey < secondKey ? -1 : 1;
        });
    }
    const playerNames = availablePlayers.map(getPlayerName);
    const rosterKey = JSON.stringify(playerNames);
    const spinStartTimestamp = Number(spinStartedAt);
    const spinEndTimestamp = Number(spinEndsAt);
    const serverTimestamp = Number(serverTime);
    const hasSharedSpinWindow = Number.isFinite(spinStartTimestamp)
        && Number.isFinite(spinEndTimestamp)
        && spinStartTimestamp > 0
        && spinEndTimestamp > spinStartTimestamp;
    const walkoutProgress = hasSharedSpinWindow
        ? Math.max(0, Math.min(1, (currentTime - spinStartTimestamp) / (spinEndTimestamp - spinStartTimestamp)))
        : 0;
    const isAwaitingReveal = hasSharedSpinWindow && currentTime < spinEndTimestamp;
    const isWalkingOut = isAwaitingReveal && currentTime >= spinStartTimestamp;
    const isClimax = isWalkingOut && walkoutProgress >= 0.9;
    const activeSeasonName = seasonName || configuredSeasonName;
    const matchedPlayer = availablePlayers.find(player =>
        getPlayerName(player).toLowerCase() === selectedPlayer.toLowerCase()
    );
    const winnerIndex = playerNames.findIndex(name => name.toLowerCase() === selectedPlayer.toLowerCase());

    const completeSpin = useEffectEvent(() => {
        onSpinComplete?.(selectedPlayer || playerNames[0] || "");
    });

    useEffect(() => {
        if (!hasSharedSpinWindow || (serverTimestamp > 0 && !clockReady)
            || soundPlayedForSpin.current === spinStartTimestamp) {
            return;
        }

        const shatterAt = spinStartTimestamp + (spinEndTimestamp - spinStartTimestamp) * 0.82;
        const now = Date.now() + clockOffset;
        if (now >= spinEndTimestamp) {
            return;
        }

        const timeoutId = setTimeout(() => {
            if (soundPlayedForSpin.current === spinStartTimestamp) {
                return;
            }
            soundPlayedForSpin.current = spinStartTimestamp;
            playGlassBreakSound();
        }, Math.max(0, shatterAt - now));

        return () => clearTimeout(timeoutId);
    }, [hasSharedSpinWindow, clockReady, serverTimestamp, spinStartTimestamp, spinEndTimestamp, clockOffset]);

    useEffect(() => {
        if (seasonName) {
            return undefined;
        }

        let isMounted = true;
        fetch(`${API_URL}/api/config`)
            .then(response => {
                if (!response.ok) throw new Error("Unable to load auction settings");
                return response.json();
            })
            .then(config => {
                if (isMounted) setConfiguredSeasonName(config?.seasonName || "");
            })
            .catch(() => { });

        return () => {
            isMounted = false;
        };
    }, [seasonName]);

    useEffect(() => {
        if (!Number.isFinite(serverTimestamp) || serverTimestamp <= 0) {
            return undefined;
        }

        const frameId = requestAnimationFrame(() => {
            setClockOffset(serverTimestamp - Date.now());
            setClockReady(true);
        });
        return () => cancelAnimationFrame(frameId);
    }, [serverTimestamp]);

    useEffect(() => {
        if (!hasSharedSpinWindow || playerNames.length === 0) {
            return undefined;
        }
        if (serverTimestamp > 0 && !clockReady) {
            return undefined;
        }

        let timeoutId = null;
        let isCancelled = false;
        const names = JSON.parse(rosterKey);
        const selectedIndex = names.findIndex(name => name.toLowerCase() === selectedPlayer.toLowerCase());
        const startIndex = Math.trunc(spinStartTimestamp) % names.length;
        const cycleCount = Math.max(names.length * 6, 30);
        const tickLength = 42;

        const updateWalkout = () => {
            if (isCancelled) return;

            const now = Date.now() + clockOffset;
            if (now < spinStartTimestamp) {
                setCurrentTime(now);
                timeoutId = setTimeout(updateWalkout, Math.min(250, spinStartTimestamp - now));
                return;
            }

            const duration = spinEndTimestamp - spinStartTimestamp;
            const progress = Math.min(1, (now - spinStartTimestamp) / duration);
            setCurrentTime(now);

            if (progress >= 1) {
                setRollingIndex(selectedIndex >= 0 ? selectedIndex : 0);
                completeSpin();
                return;
            }

            const rollProgress = Math.min(1, progress / 0.9);
            const easedProgress = 1 - Math.pow(1 - rollProgress, 2.4);
            const step = Math.floor(easedProgress * cycleCount);
            const landingIndex = progress >= 0.9 && selectedIndex >= 0
                ? selectedIndex
                : (startIndex + step) % names.length;
            setRollingIndex(landingIndex);

            const elapsed = now - spinStartTimestamp;
            const nextTick = (Math.floor(elapsed / tickLength) + 1) * tickLength;
            timeoutId = setTimeout(updateWalkout, Math.max(12, nextTick - elapsed));
        };

        timeoutId = setTimeout(updateWalkout, 0);
        return () => {
            isCancelled = true;
            if (timeoutId) clearTimeout(timeoutId);
        };
    }, [hasSharedSpinWindow, spinStartTimestamp, spinEndTimestamp, selectedPlayer, rosterKey, playerNames.length, clockOffset, clockReady, serverTimestamp]);

    const rollingPlayer = availablePlayers[rollingIndex];
    const activePlayer = isWalkingOut
        ? rollingPlayer
        : !isAwaitingReveal && selectedPlayer
            ? matchedPlayer || selectedPlayer
            : null;
    const activeName = getPlayerName(activePlayer);
    const activeSeed = typeof activePlayer === "object" ? activePlayer?.seed || activePlayer?.category : "";
    const isRevealed = Boolean(selectedPlayer) && !isAwaitingReveal;
    const slipProgress = isRevealed ? 1 : Math.max(0, Math.min(1, (walkoutProgress - 0.58) / 0.36));
    const visibleRosterCenter = isWalkingOut
        ? rollingIndex
        : winnerIndex >= 0
            ? winnerIndex
            : 0;
    const visibleRoster = availablePlayers.length > 3
        ? [-1, 0, 1].map(offset => {
            const index = (visibleRosterCenter + offset + availablePlayers.length) % availablePlayers.length;
            return { player: availablePlayers[index], index };
        })
        : availablePlayers.map((player, index) => ({ player, index }));
    const walkoutStyle = {
        "--walkout-progress": walkoutProgress,
        "--walkout-progress-width": `${walkoutProgress * 100}%`,
        "--walkout-slip-offset": `${120 - slipProgress * 120}px`,
        "--walkout-slip-opacity": isRevealed ? 1 : Math.min(1, slipProgress * 2),
        "--walkout-bottle-tilt": `${Math.sin(walkoutProgress * Math.PI * 12) * (1 - walkoutProgress) * 7}deg`,
        "--walkout-cork-lift": `${-walkoutProgress * 42}px`,
    };
    const status = isWalkingOut
        ? isClimax ? "NAME EMERGING" : "BOTTLE TURNING"
        : isAwaitingReveal
            ? "BOTTLE SEALED"
            : isRevealed
                ? "PLAYER REVEALED"
                : "READY FOR THE DRAW";

    return (
        <section
            className={`player-walkout ${isWalkingOut ? "is-walking-out" : ""} ${isClimax ? "is-climax" : ""} ${isRevealed ? "is-revealed" : ""} ${className}`.trim()}
            aria-label="Player bottle draw"
        >
            <header className="walkout-header">
                <div>
                    <span className="walkout-kicker">{activeSeasonName ? `${activeSeasonName} · AUCTION NIGHT` : "AUCTION NIGHT"}</span>
                    <h2>THE PLAYER DRAW</h2>
                </div>
                <span className={`walkout-status ${isWalkingOut ? "is-live" : ""}`}>
                    <i />{status}
                </span>
            </header>

            <div
                className={`walkout-stage bottle-draw-stage ${isWalkingOut ? "is-scanning" : ""} ${isClimax ? "is-climax" : ""} ${isRevealed ? "has-winner" : ""}`}
                data-testid="player-walkout-stage"
                data-active-player={activeName || ""}
                style={walkoutStyle}
            >
                <Suspense fallback={null}>
                    <BottleDrawScene progress={walkoutProgress} drawing={isWalkingOut} revealed={isRevealed} />
                </Suspense>
                <div className="walkout-bottle-scene" aria-hidden="true">
                    <div className="walkout-bottle-shadow" />
                    <div className="walkout-bottle">
                        <i className="walkout-bottle-cork" />
                        <i className="walkout-bottle-neck" />
                        <i className="walkout-bottle-shoulder" />
                        <div className="walkout-bottle-body">
                            <i className="walkout-bottle-highlight" />
                            <i className="walkout-bottle-refraction" />
                            <span>SEALED DRAW</span>
                        </div>
                        <i className="walkout-bottle-neck-ring" />
                    </div>
                </div>

                <div className="walkout-name-slip" aria-live={isRevealed ? "polite" : "off"}>
                    <span className="walkout-slip-kicker">
                        {isClimax ? "THE NAME IS COMING OUT" : isWalkingOut ? "DRAWING A NAME" : isRevealed ? "SELECTED PLAYER" : "SEALED INSIDE"}
                    </span>
                    <strong className="walkout-player-name">
                        {activeName || (isAwaitingReveal ? "A name is sealed" : "Waiting for the draw")}
                    </strong>
                    <span className="walkout-player-seed">{activeSeed ? `SEED ${activeSeed}` : isRevealed ? "NOMINEE" : "BTC AUCTION"}</span>
                </div>

                <div className="walkout-roster" aria-label="Players in the draw">
                    {visibleRoster.map(({ player, index }) => {
                        const name = getPlayerName(player);
                        const isActive = name.toLowerCase() === activeName?.toLowerCase();
                        return (
                            <div className={`walkout-roster-player ${isActive ? "is-active" : ""}`} key={`${index}-${name}`}>
                                <span>{String(index + 1).padStart(2, "0")}</span>
                                <strong>{name}</strong>
                            </div>
                        );
                    })}
                    {availablePlayers.length === 0 && (
                        <p className="walkout-empty">Players will appear here once added to the auction.</p>
                    )}
                </div>
                <div className="walkout-progress-track" aria-hidden="true"><i /></div>
            </div>
        </section>
    );
}

export default PlayerWalkout;