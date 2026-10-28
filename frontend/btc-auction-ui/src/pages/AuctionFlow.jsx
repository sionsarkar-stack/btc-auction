import { useCallback, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import { apiRequest } from "../services/apiClient";

function AuctionFlow({ isAdmin = false }) {
    const [teams, setTeams] = useState([]);
    const [players, setPlayers] = useState([]);
    const [allPlayers, setAllPlayers] = useState([]);
    const [auction, setAuction] = useState(null);
    const [status, setStatus] = useState(null);
    const [latestSale, setLatestSale] = useState(null);
    const [selectedPlayer, setSelectedPlayer] = useState("");
    const [customBid, setCustomBid] = useState("");
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isBusy, setIsBusy] = useState(false);
    const [isConnected, setIsConnected] = useState(false);
    const [clock, setClock] = useState(0);
    const [serverOffset, setServerOffset] = useState(0);

    const username = localStorage.getItem("username") || "";
    const role = localStorage.getItem("role");
    const isCaptain = role === "CAPTAIN";

    const loadData = useCallback(async () => {
        try {
            const [teamData, playerData, allPlayerData, currentAuction, auctionStatus, eventData] = await Promise.all([
                apiRequest("/api/teams"),
                apiRequest("/api/players/available"),
                apiRequest("/api/players"),
                apiRequest("/api/auction/current"),
                apiRequest("/api/auction/status"),
                apiRequest("/api/events"),
            ]);
            setTeams(Array.isArray(teamData) ? teamData : []);
            setPlayers(Array.isArray(playerData) ? playerData.filter(player => !player.sold) : []);
            setAllPlayers(Array.isArray(allPlayerData) ? allPlayerData : []);
            setAuction(currentAuction);
            setStatus(auctionStatus);
            setLatestSale(Array.isArray(eventData)
                ? eventData.filter(event => event.eventType === "PLAYER_SOLD")
                    .reduce((latest, event) => !latest || event.id > latest.id ? event : latest, null)
                : null);
            setServerOffset((auctionStatus?.serverTime || Date.now()) - Date.now());
            setMessage("");
        } catch (error) {
            setMessage(error.message || "Unable to load auction data.");
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const initialLoad = window.setTimeout(() => void loadData(), 0);
        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => {
                setIsConnected(true);
                client.subscribe("/topic/auction", () => void loadData());
            },
            onDisconnect: () => setIsConnected(false),
            onWebSocketClose: () => setIsConnected(false),
        });
        client.activate();
        return () => {
            window.clearTimeout(initialLoad);
            client.deactivate();
        };
    }, [loadData]);

    useEffect(() => {
        const intervalId = window.setInterval(() => setClock(Date.now()), 200);
        return () => window.clearInterval(intervalId);
    }, []);

    const runAction = async (path, body) => {
        setIsBusy(true);
        try {
            const result = await apiRequest(path, {
                method: "POST",
                ...(body ? {
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(body),
                } : {}),
            });
            await loadData();
            setMessage(result);
        } catch (error) {
            setMessage(error.message || "Auction action failed.");
        } finally {
            setIsBusy(false);
        }
    };

    const currentPlayer = auction?.currentPlayer || "";
    const currentTeam = teams.find(team => team.captainName.toLowerCase() === username.toLowerCase());
    const maxBid = currentTeam
        ? Math.max(0, currentTeam.purse - Math.max(0, currentTeam.playersLeft - 1) * 100)
        : 0;
    const nextFixedBid = (auction?.currentBid || 0) + 50;
    const firstAllowedCustomBid = Math.floor((auction?.currentBid || 0) / 50) * 50 + 50;
    const customBidAmount = Number(customBid);
    const isCustomBidValid = Number.isInteger(customBidAmount)
        && customBidAmount > (auction?.currentBid || 0)
        && customBidAmount % 50 === 0
        && customBidAmount <= maxBid;
    const isBidding = status?.auctionPhase === "BIDDING" && Boolean(currentPlayer);
    const canShowNomination = Boolean(status?.auctionStarted && players.length > 0);
    const isMyNominationTurn = status?.nextNominationCaptain?.toLowerCase() === username.toLowerCase();
    const hasPassed = (status?.passedCaptains || []).some(name => name.toLowerCase() === username.toLowerCase());
    const deadline = status?.bidDeadlineAt;
    const secondsLeft = deadline && clock
        ? Math.max(0, Math.ceil((deadline - (clock + serverOffset)) / 1000))
        : null;
    const allOtherCaptainsPassed = Boolean(auction?.leader && auction.leader !== "None")
        && teams.filter(team => team.captainName.toLowerCase() !== auction.leader.toLowerCase())
            .every(team => (status?.passedCaptains || []).some(name => name.toLowerCase() === team.captainName.toLowerCase()));
    const passCount = status?.passedCaptains?.length || 0;
    const timeoutEndsAt = status?.auctionTimeoutEndsAt;
    const timeoutSecondsLeft = timeoutEndsAt && clock
        ? Math.max(0, Math.ceil((timeoutEndsAt - (clock + serverOffset)) / 1000))
        : null;
    const isTimeoutActive = timeoutSecondsLeft !== null && timeoutSecondsLeft > 0;
    const timeoutUsage = Object.entries(status?.timeoutUsage || {})
        .find(([captainName]) => captainName.toLowerCase() === username.toLowerCase())?.[1] || 0;
    const timeoutsLeft = Math.max(0, 3 - timeoutUsage);
    const auctionBreakEndsAt = status?.auctionBreakEndsAt;
    const auctionBreakSecondsLeft = auctionBreakEndsAt && clock
        ? Math.max(0, Math.ceil((auctionBreakEndsAt - (clock + serverOffset)) / 1000))
        : null;
    const isScheduledBreakActive = auctionBreakSecondsLeft !== null && auctionBreakSecondsLeft > 0;
    const auctionsUntilBreak = Math.max(1, 18 - (status?.auctionsSinceLastBreak || 0));
    const breakCountdown = auctionBreakSecondsLeft === null
        ? "3:00"
        : `${Math.floor(auctionBreakSecondsLeft / 60)}:${String(auctionBreakSecondsLeft % 60).padStart(2, "0")}`;

    if (isLoading) {
        return <div className="app-container"><p role="status">Loading auction...</p></div>;
    }

    return (
        <section className="app-container auction-flow">
            <header className="page-header">
                <div>
                    <p className="auction-kicker">{isAdmin ? "Auction Control" : "Live Auction"}</p>
                    <h1 className="page-title">{status?.seasonName || "Auction"}</h1>
                </div>
                <div className={isConnected ? "connection-status connected" : "connection-status"}>
                    <span className="connection-dot" />
                    {isConnected ? "Live updates connected" : "Connecting to live updates..."}
                </div>
            </header>

            {latestSale && (
                <section className="player-sold-banner" role="status" aria-live="polite">
                    <span className="player-sold-banner-label">PLAYER SOLD</span>
                    <div className="player-sold-details">
                        <strong>{latestSale.playerName}</strong>
                        <span>to {latestSale.captainName}</span>
                    </div>
                    <strong className="player-sold-price">₹{latestSale.amount}</strong>
                </section>
            )}

            {status?.auctionStarted && (
                <section className={isScheduledBreakActive ? "auction-break-banner is-active" : "auction-break-banner"}
                    role="status" aria-live="polite">
                    {isScheduledBreakActive ? (
                        <>
                            <strong>Scheduled 3-minute auction break</strong>
                            <span>Resumes in {breakCountdown}</span>
                            <span>Next break after 18 completed player auctions</span>
                        </>
                    ) : (
                        <>
                            <strong>Next 3-minute break</strong>
                            <span>After {auctionsUntilBreak} completed player auction{auctionsUntilBreak === 1 ? "" : "s"}</span>
                        </>
                    )}
                </section>
            )}

            {isAdmin && (
                <section className="section-card auction-flow-admin">
                    <div className="section-heading-row">
                        <div>
                            <h2>Auction status</h2>
                            <p>{status?.auctionStarted ? "Captains nominate one player per turn in captain-list order." : "Ready when captains and players are set up."}</p>
                        </div>
                        <div className="auction-flow-admin-actions">
                            <button
                                className="button"
                                type="button"
                                onClick={() => void runAction("/api/auction/start")}
                                disabled={isBusy || status?.auctionStarted || teams.length === 0 || players.length === 0}
                            >
                                {status?.auctionStarted ? "Auction started" : "Start auction"}
                            </button>
                            <button
                                className="button-secondary"
                                type="button"
                                onClick={() => void runAction("/api/auction/end")}
                                disabled={isBusy || !status?.auctionStarted || Boolean(currentPlayer)}
                            >
                                End auction
                            </button>
                        </div>
                    </div>
                    <p className="form-hint">{teams.length} captains · {players.length} available players</p>
                </section>
            )}

            <section className="current-auction-card auction-flow-current">
                {currentPlayer ? (
                    <>
                        <div className="auction-metrics" aria-label="Current auction metrics">
                            <div><span>Base price</span><strong>₹{auction.basePrice}</strong></div>
                            <div><span>Current bid</span><strong>₹{auction.currentBid}</strong></div>
                            <div><span>Leading captain</span><strong>{auction.leader === "None" ? "No bids" : auction.leader}</strong></div>
                            <div><span>Increase count</span><strong>{Math.max(0, Math.floor((auction.currentBid - auction.basePrice) / 50))}</strong></div>
                        </div>
                        <p className="auction-kicker auction-nominated-label">Nominated player</p>
                        <h2 className="current-player-name">{currentPlayer}</h2>
                        <p className="current-player-seed">Seed {auction.seed || "-"}</p>
                        {isTimeoutActive ? (
                            <div className="auction-timeout-banner" role="status" aria-live="polite">
                                <strong>{status.timeoutCaptainName} called timeout</strong>
                                <span>Auction paused · {timeoutSecondsLeft} seconds</span>
                            </div>
                        ) : (
                            <div className={`auction-countdown${secondsLeft !== null && secondsLeft <= 3 ? " is-urgent" : ""}`} aria-live="polite">
                                {secondsLeft === null ? "Waiting for the first bid" : `${secondsLeft}`}
                                {secondsLeft !== null && <span>seconds</span>}
                            </div>
                        )}
                        {allOtherCaptainsPassed && auction.leader && auction.leader !== "None" && (
                            <p className="message-success" role="status">All other captains passed. The sale closes when the counter reaches zero.</p>
                        )}
                        {isCaptain && isBidding && (
                            <>
                                <div className="auction-budget-summary">
                                    <span>Points remaining <strong>₹{currentTeam?.purse ?? 0}</strong></span>
                                    <span>Maximum bid <strong>₹{maxBid}</strong></span>
                                </div>
                                <div className="auction-bid-actions">
                                    <button
                                        className="button"
                                        type="button"
                                        onClick={() => void runAction("/api/auction/bid/increase")}
                                        disabled={isBusy || isTimeoutActive || auction.leader?.toLowerCase() === username.toLowerCase() || nextFixedBid > maxBid}
                                        title={nextFixedBid > maxBid ? `Next bid ₹${nextFixedBid} exceeds your max bid of ₹${maxBid}.` : undefined}
                                    >
                                        Increase to ₹{nextFixedBid}
                                    </button>
                                    <button
                                        className="button-secondary"
                                        type="button"
                                        onClick={() => void runAction("/api/auction/bid/pass")}
                                        disabled={isBusy || isTimeoutActive}
                                    >
                                        {hasPassed ? "Rejoin bidding" : "Pass"}
                                    </button>
                                </div>
                                <form
                                    className="custom-bid-form"
                                    onSubmit={event => {
                                        event.preventDefault();
                                        if (isCustomBidValid) {
                                            void runAction("/api/auction/bid/custom", { bidAmount: customBidAmount });
                                            setCustomBid("");
                                        }
                                    }}
                                >
                                    <label htmlFor="custom-live-bid">Custom bid</label>
                                    <input
                                        id="custom-live-bid"
                                        className="input"
                                        type="number"
                                        min={firstAllowedCustomBid}
                                        max={maxBid}
                                        step="50"
                                        value={customBid}
                                        onChange={event => setCustomBid(event.target.value)}
                                        disabled={isBusy || isTimeoutActive || auction.leader?.toLowerCase() === username.toLowerCase()}
                                    />
                                    <button
                                        className="button-secondary"
                                        type="submit"
                                        disabled={isBusy || isTimeoutActive || auction.leader?.toLowerCase() === username.toLowerCase() || !isCustomBidValid}
                                    >
                                        Place custom bid
                                    </button>
                                    <span className="form-hint">Must exceed ₹{auction.currentBid}, use ₹50 increments, and not exceed ₹{maxBid}.</span>
                                </form>
                                <button
                                    className="button-secondary auction-timeout-button"
                                    type="button"
                                    onClick={() => void runAction("/api/auction/timeout")}
                                    disabled={isBusy || isTimeoutActive || timeoutsLeft === 0}
                                >
                                    Pause auction · {timeoutsLeft} timeouts left
                                </button>
                            </>
                        )}
                        {isBidding && <p className="form-hint auction-pass-count">{passCount} of {teams.length} captains passed · 3 passes close the lot</p>}
                    </>
                ) : isScheduledBreakActive ? (
                    <div className="auction-flow-nomination auction-break-state">
                        <p className="auction-kicker">Scheduled break</p>
                        <h2>Auction resumes in {breakCountdown}</h2>
                        <p className="form-hint">Nominations resume when the break ends.</p>
                    </div>
                ) : canShowNomination ? (
                    <div className="auction-flow-nomination">
                        <p className="auction-kicker">Next nomination</p>
                        <h2>{status?.nextNominationCaptain || "Waiting for auction to start"}</h2>
                        {isCaptain && status?.auctionStarted && isMyNominationTurn && (
                            <div className="auction-nomination-controls">
                                <select
                                    className="select"
                                    aria-label="Choose a player to nominate"
                                    value={selectedPlayer}
                                    onChange={event => setSelectedPlayer(event.target.value)}
                                    disabled={isBusy}
                                >
                                    <option value="">Select an available player</option>
                                    {players.map(player => (
                                        <option key={player.name} value={player.name}>
                                            {player.name} · ₹{player.basePrice}
                                        </option>
                                    ))}
                                </select>
                                <button
                                    className="button"
                                    type="button"
                                    onClick={() => void runAction("/api/auction/nominate", { playerName: selectedPlayer })}
                                    disabled={isBusy || !selectedPlayer}
                                >
                                    Nominate player
                                </button>
                            </div>
                        )}
                        {status?.auctionStarted && isCaptain && !isMyNominationTurn && (
                            <p className="form-hint">Waiting for {status.nextNominationCaptain} to nominate.</p>
                        )}
                        {role === "VIEWER" && <p className="form-hint">Waiting for the next player nomination.</p>}
                        {!status?.auctionStarted && <p className="form-hint">The admin can start the auction after player import and captain setup.</p>}
                    </div>
                ) : null}
            </section>

            {message && <p className={/unable|cannot|required|not found|failed|already|only|maximum|must|multiple|greater|exceed|invalid/i.test(message) ? "message-error" : "message-success"} role="status">{message}</p>}

            {isAdmin && (
                <section className="section-card auction-flow-roster">
                    <h2>Captains</h2>
                    <ul>
                        {teams.map(team => (
                            <li key={team.captainName}>
                                <span>{team.captainName}</span>
                                <span>{(status?.passedCaptains || []).some(name => name.toLowerCase() === team.captainName.toLowerCase()) ? "Passed" : team.captainName === auction?.leader ? "Leading" : "Ready"}</span>
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            <section className="section-card auction-flow-teams">
                <h2>Team points and players sold</h2>
                <div className="auction-team-table-wrap">
                    <table className="standings-table auction-team-table">
                        <thead>
                            <tr>
                                <th scope="col">Team</th>
                                <th scope="col">Points remaining</th>
                                <th scope="col">Max bid</th>
                                <th scope="col">Players sold</th>
                            </tr>
                        </thead>
                        <tbody>
                            {teams.map(team => {
                                const teamPlayers = allPlayers.filter(player => player.sold
                                    && player.team?.toLowerCase() === team.captainName.toLowerCase());
                                const teamMaxBid = Math.max(0, team.purse - Math.max(0, team.playersLeft - 1) * 100);
                                return (
                                    <tr key={team.captainName}>
                                        <th scope="row">{team.captainName}</th>
                                        <td>₹{team.purse}</td>
                                        <td>₹{teamMaxBid}</td>
                                        <td>
                                            {teamPlayers.length > 0 ? (
                                                <ul className="auction-team-sold-list">
                                                    {teamPlayers.map(player => (
                                                        <li key={player.id || player.name}>
                                                            <span>{player.name}</span>
                                                            <strong>₹{player.soldPrice}</strong>
                                                        </li>
                                                    ))}
                                                </ul>
                                            ) : "-"}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </section>
        </section>
    );
}

export default AuctionFlow;
