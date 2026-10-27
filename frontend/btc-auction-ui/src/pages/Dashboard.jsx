import { useEffect, useState, useCallback, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { API_URL } from "../config";
import LiveActivity from "../components/LiveActivity";
import PlayerWalkout from "../components/PlayerWalkout";
import { showToast } from "../services/toast";
import ActionFeedback from "../services/ActionFeedback";
import { useActionRequest } from "../services/useActionRequest";
import { apiRequest } from "../services/apiClient";

const RANDOM_EVENT_BANTER = {
    MARKET_BOOM: "The price elevator has skipped the safety briefing.",
    MARKET_CRASH: "The price has found a banana peel. Brace for the landing.",
    RTM_LOCKDOWN: "NO MERCY: RTM and Wild Pick are banned for this player.",
    SILENT_AUCTION: "Shhh. Even the calculator is whispering.",
};

function Dashboard() {

    const [dashboard, setDashboard] =
        useState(null);

    const [events, setEvents] =
        useState([]);

    const [rtm, setRtm] =

        useState(null);

    const [rtmBid, setRtmBid] =
        useState("");

    const [silentBidActive, setSilentBidActive] =
        useState(false);

    const [silentRound, setSilentRound] =
        useState(null);

    const [allSilentBids, setAllSilentBids] =
        useState([]);

    const [silentBid, setSilentBid] =
        useState("");

    const [openingBid, setOpeningBid] =
        useState("");

    const [openingBidStatus, setOpeningBidStatus] =
        useState(null);

    const [liveBid, setLiveBid] =
        useState("");

    const [dashboardView, setDashboardView] =
        useState("overview");

    const [auctionEnded, setAuctionEnded] =
        useState(false);

    const [isDashboardLoading, setIsDashboardLoading] = useState(true);
    const [isDashboardRefreshing, setIsDashboardRefreshing] = useState(false);
    const [dashboardError, setDashboardError] = useState(null);
    const [lastSuccessfulRefresh, setLastSuccessfulRefresh] = useState(null);
    const [outcomeRefreshStatus, setOutcomeRefreshStatus] = useState("needed");
    const refreshGeneration = useRef(0);
    const refreshController = useRef(null);

    const {
        acknowledgeOutcome,
        feedback: actionFeedback,
        outcomeUnknown,
        pendingAction,
        runAction,
    } = useActionRequest();
    const actionPending = Boolean(pendingAction);
    const actionsDisabled = actionPending || outcomeUnknown || isDashboardRefreshing || Boolean(dashboardError);

    const postAuctionAction = async (label, path, body, isSuccessful) => {
        const action = await runAction(
            label,
            `${API_URL}${path}`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
            },
            isSuccessful
        );
        if (action.started) {
            loadDashboard();
        }
        return action;
    };

    const role =
        localStorage.getItem("role");

    const username =
        localStorage.getItem("username");

    const isAdmin =
        role === "ADMIN";

    const isCaptain =
        role === "CAPTAIN";

    const isViewer =
        role === "VIEWER";


    const isRtmCaptain =
        rtm &&
        rtm.captainName === username;

    const isOriginalCaptain =
        rtm &&
        rtm.originalCaptain === username;

    const currentTeam =
        dashboard?.teams?.find(
            team => team.captainName === username
        );

    const rtmAlreadyUsed =
        currentTeam ? !currentTeam.rtmAvailable : false;

    const isCurrentHighestBidder =
        dashboard?.currentAuction?.leader?.toLowerCase() ===
        username?.toLowerCase();

    const [auctionStatus, setAuctionStatus] =
        useState(null);

    const [players, setPlayers] =
        useState([]);

    const [curseSelections, setCurseSelections] =
        useState([]);

    const [newCursePlayer, setNewCursePlayer] =
        useState("");

    const [clubbedPairs, setClubbedPairs] =
        useState([]);

    const [, setCaptains] =
        useState([]);

    const [teamQuery, setTeamQuery] =
        useState("");

    const [playerQuery, setPlayerQuery] =
        useState("");

    const [playerFilter, setPlayerFilter] =
        useState("ALL");

    const [playerCategory, setPlayerCategory] =
        useState("ALL");

    const loadDashboard = useCallback(async () => {
        const requestGeneration = ++refreshGeneration.current;
        refreshController.current?.abort();
        const controller = new AbortController();
        refreshController.current = controller;
        setIsDashboardRefreshing(true);

        try {
            const [dashboardData, eventsData, statusData, playersData, clubbedPairData, captainsData] = await Promise.all([
                apiRequest("/api/dashboard", { signal: controller.signal }),
                apiRequest("/api/events", { signal: controller.signal }),
                apiRequest("/api/auction/status", { signal: controller.signal }),
                apiRequest("/api/players", { signal: controller.signal }),
                apiRequest("/api/auction/clubbed-pair", { signal: controller.signal }),
                apiRequest("/api/teams", { signal: controller.signal }),
            ]);

            const [curseData, rtmData, silentActive] = await Promise.all([
                isCaptain
                    ? apiRequest("/api/curse-players/mine", { signal: controller.signal })
                    : Promise.resolve([]),
                apiRequest("/api/rtm/current", { signal: controller.signal }),
                apiRequest("/api/silent-bid/active", { signal: controller.signal }),
            ]);

            const [allSilentBidData, captainSilentData, openingBidData] = await Promise.all([
                silentActive
                    ? apiRequest("/api/silent-bid/all", { signal: controller.signal })
                    : Promise.resolve([]),
                isCaptain && silentActive
                    ? apiRequest(`/api/silent-bid/${encodeURIComponent(username)}`, { signal: controller.signal })
                    : Promise.resolve(null),
                isCaptain && statusData.auctionPhase === "BLIND_OPENING_BID"
                    ? apiRequest("/api/auction/current", { signal: controller.signal }).then(current =>
                        current?.currentPlayer
                            ? apiRequest(
                                `/api/auction/blind-opening-bid/${encodeURIComponent(current.currentPlayer)}/${encodeURIComponent(username)}`,
                                { signal: controller.signal }
                            )
                            : null
                    )
                    : Promise.resolve(null),
            ]);

            if (requestGeneration !== refreshGeneration.current) {
                return false;
            }

            setDashboard(dashboardData);
            setEvents(Array.isArray(eventsData) ? eventsData.slice().reverse().slice(0, 20) : []);
            setAuctionStatus(statusData);
            setPlayers(Array.isArray(playersData) ? playersData : []);
            setClubbedPairs(Array.isArray(clubbedPairData) ? clubbedPairData : []);
            setCaptains(Array.isArray(captainsData) ? captainsData : []);
            setCurseSelections(Array.isArray(curseData) ? curseData : []);
            setRtm(rtmData || null);
            setSilentBidActive(Boolean(silentActive));
            setAllSilentBids(Array.isArray(allSilentBidData) ? allSilentBidData : []);
            setSilentRound(captainSilentData || null);
            setOpeningBidStatus(openingBidData || null);
            setDashboardError(null);
            setLastSuccessfulRefresh(Date.now());
            return true;
        } catch (error) {
            if (error.name === "AbortError" || requestGeneration !== refreshGeneration.current) {
                return false;
            }
            setDashboardError(error.message || "Unable to refresh auction data. Retry to get the latest state.");
            return false;
        } finally {
            if (requestGeneration === refreshGeneration.current) {
                setIsDashboardLoading(false);
                setIsDashboardRefreshing(false);
            }
        }
    }, [isCaptain, username]);

    useEffect(() => {
        loadDashboard();

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(`${API_URL}/ws`),
            reconnectDelay: 5000,
        });

        client.onConnect = () => {
            client.subscribe(
                "/topic/auction",
                () => {
                    loadDashboard();
                }
            );
        };

        client.activate();

        return () => {
            refreshController.current?.abort();
            client.deactivate();
        };
    }, [loadDashboard]);

    const submitCursePlayer = async () => {
        const response = await fetch(`${API_URL}/api/curse-players`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ playerName: newCursePlayer })
        });
        showToast(await response.text());
        setNewCursePlayer("");
        loadDashboard();
    };

    const claimRtm = async () => {
        const action = await postAuctionAction(
            "Claiming RTM",
            "/api/rtm/claim",
            { captainName: username },
            result => result === "RTM claimed."
        );
        if (action.success) {
            if (navigator.vibrate) navigator.vibrate(300);
            new Audio("/sounds/veto.mp3").play().catch(() => { });
        }

    };

    const submitRtmBid = async () => {
        await postAuctionAction(
            "Submitting RTM bid",
            "/api/rtm/bid",
            { bidAmount: Number(rtmBid) },
            result => result === "RTM bid submitted."
        );
    };

    const submitLiveBid = async () => {
        if (!liveBid || Number(liveBid) <= 0) return;
        const action = await postAuctionAction(
            "Placing bid",
            "/api/auction/update-current",
            { captainName: username, currentBid: Number(liveBid) },
            result => result === "Updated"
        );
        if (action.success) setLiveBid("");
    };

    const acceptRtm = async () => {
        const playerName = currentAuction?.currentPlayer || "Player";
        await postAuctionAction(
            "Accepting RTM",
            "/api/rtm/accept",
            { captainName: username },
            result => result.startsWith(`${playerName} sold to `)
        );
    };

    const declineRtm = async () => {
        const playerName = currentAuction?.currentPlayer || "Player";
        await postAuctionAction(
            "Declining RTM",
            "/api/rtm/decline",
            { captainName: username },
            result => result.startsWith(`${playerName} sold to `)
        );
    };

    const submitSilentBid = async () => {
        if (!silentRound || silentBid === "" || Number(silentBid) < 0) return;
        const action = await postAuctionAction(
            "Submitting silent bid",
            "/api/silent-bid/submit",
            {
                playerName: silentRound.playerName,
                captainName: username,
                bidAmount: Number(silentBid)
            },
            result => result === "Bid submitted."
        );
        if (action.success) {
            setSilentBid("");
        }
    };

    const submitOpeningBid = async () => {
        if (!openingBid || Number(openingBid) <= 0 || !dashboard.currentAuction.currentPlayer) return;
        const action = await postAuctionAction(
            "Submitting opening bid",
            "/api/auction/blind-opening-bid/submit",
            {
                playerName: dashboard.currentAuction.currentPlayer,
                captainName: username,
                bidAmount: Number(openingBid)
            },
            result => result === "Blind opening bid submitted."
        );
        if (action.success) setOpeningBid("");
    };

    const passOpeningBid = async () => {
        if (!dashboard.currentAuction.currentPlayer) return;
        await postAuctionAction(
            "Passing on opening bid",
            "/api/auction/blind-opening-bid/pass",
            {
                playerName: dashboard.currentAuction.currentPlayer,
                captainName: username
            },
            result => result === "Blind opening bid passed."
        );
    };

    const endAuction = async () => {
        if (!window.confirm("End the auction and prepare the final team report?")) {
            return;
        }

        const action = await runAction(
            "Ending auction",
            `${API_URL}/api/auction/end`,
            { method: "POST" },
            result => result === "Auction ended and secret targets settled."
        );
        if (action.success) {
            setAuctionEnded(true);
            loadDashboard();
        }
    };

    const exportFinalTeamsPdf = () => {
        window.print();
    };

    const activateWildcard = async () => {
        if (!window.confirm("Activate Wildcard? This cancels every submitted blind opening bid and returns the player to the nomination lot.")) {
            return;
        }

        const action = await runAction(
            "Activating Wildcard",
            `${API_URL}/api/auction/wild-pick`,
            { method: "POST", credentials: "include" },
            result => result.includes("activated Wildcard")
        );
        if (action.success) {
            setOpeningBid("");
            loadDashboard();
        }
    };

    const refreshBeforeRetry = async () => {
        setOutcomeRefreshStatus("refreshing");
        const refreshed = await loadDashboard();
        setOutcomeRefreshStatus(refreshed ? "refreshed" : "needed");
    };

    const acknowledgeReviewedState = () => {
        acknowledgeOutcome();
        setOutcomeRefreshStatus("needed");
    };

    if (!dashboard) {
        return (
            <div className="app-container">
                {isDashboardLoading && <p role="status">Loading auction data...</p>}
                {dashboardError && (
                    <section className="message-error" role="alert">
                        <p>{dashboardError}</p>
                        <button className="button-secondary" type="button" onClick={loadDashboard} disabled={isDashboardRefreshing}>
                            {isDashboardRefreshing ? "Retrying..." : "Retry refresh"}
                        </button>
                    </section>
                )}
            </div>
        );
    }

    const specialEvents =
        events.filter(event =>
            [
                "CAPTAIN_TRIBUNAL",
                "RTM_TRIGGERED",
                "RTM_BID_SUBMITTED",
                "RTM_ACCEPTED",
                "RTM_DECLINED",
                "STARTING_BID_WINNER",
                "WILDCARD_TRIGGERED",
                "CURSE_REVEALED",
                "CURSE_CANCELLED",
                "BLIND_BID_BONUS",
                "BLIND_BONANZA",
                "BLIND_BONANZA_REWARDED"
            ].includes(event.eventType)
        );

    const categories = [
        "ALL",
        ...new Set(
            players
                .map(player => player.category || player.seed || "UNCATEGORIZED")
        )
    ];

    const filteredTeams = dashboard.teams?.filter(team =>
        team.captainName.toLowerCase().includes(teamQuery.toLowerCase())
    ) || [];

    const filteredPlayers = players.filter(player => {
        const matchesQuery = player.name.toLowerCase().includes(playerQuery.toLowerCase());
        const matchesStatus = playerFilter === "ALL"
            || (playerFilter === "AVAILABLE" && !player.sold)
            || (playerFilter === "SOLD" && player.sold);
        const category = player.category || player.seed || "UNCATEGORIZED";
        const matchesCategory = playerCategory === "ALL"
            || category === playerCategory;

        return matchesQuery && matchesStatus && matchesCategory;
    });

    const currentAuction = dashboard.currentAuction;
    const currentPlayerPair = clubbedPairs.find(pair =>
        pair.playerOne?.toLowerCase() === currentAuction.currentPlayer?.toLowerCase()
    );
    const currentPlayerLabel = currentPlayerPair
        ? `${currentPlayerPair.playerOne} + ${currentPlayerPair.playerTwo}`
        : currentAuction.currentPlayer;
    const effectiveSaleValue = player => {
        return player.finalPrice !== 0 ? player.finalPrice : player.soldPrice;
    };
    const squadValueForTeam = captainName => players
        .filter(player => player.team === captainName)
        .filter(player => !clubbedPairs.some(pair =>
            pair.playerTwo?.toLowerCase() === player.name?.toLowerCase()
        ))
        .reduce((total, player) => total + effectiveSaleValue(player), 0);
    const squadPlayerLabel = playerName => {
        const player = players.find(item => item.name === playerName);
        if (player?.seed?.toUpperCase() === "C") {
            return playerName;
        }
        const saleValue = player ? effectiveSaleValue(player) : null;
        return saleValue === null || saleValue === undefined
            ? playerName
            : `${playerName} (₹${saleValue})`;
    };
    const rtmLocked = auctionStatus?.rtmLockdownPlayer?.toLowerCase()
        === currentAuction.currentPlayer?.toLowerCase();
    const isReAuction = auctionStatus?.auctionRound === 2;
    const isBidding = ["OPENING_BID", "BIDDING"].includes(auctionStatus?.auctionPhase);
    const activeRandomEventType = auctionStatus?.pendingRandomEventType;
    const activeRandomEventDescription = auctionStatus?.pendingRandomEventDescription
        || "A surprise auction rule is in play.";
    const showPublicRandomEvent = activeRandomEventType
        && (activeRandomEventType !== "MARKET_CRASH"
            || auctionStatus?.auctionPhase === "SOLD");

    return (

        <div>

            <section
                className={`dashboard-freshness${dashboardError ? " is-stale" : isDashboardRefreshing ? " is-refreshing" : " is-live"}`}
                aria-label="Dashboard data freshness"
            >
                <span className="dashboard-freshness-indicator" aria-hidden="true" />
                <span className="dashboard-freshness-copy" role={dashboardError ? "alert" : "status"} aria-live={dashboardError ? "assertive" : "polite"}>
                    <strong>{dashboardError ? "Data may be stale" : isDashboardRefreshing ? "Refreshing auction" : "Auction data current"}</strong>
                    {lastSuccessfulRefresh && (
                        <span>Last updated {new Date(lastSuccessfulRefresh).toLocaleTimeString()}.</span>
                    )}
                    {dashboardError && <span>{dashboardError}</span>}
                </span>
                {dashboardError && (
                    <button className="button-secondary" type="button" onClick={loadDashboard} disabled={isDashboardRefreshing}>
                        {isDashboardRefreshing ? "Retrying..." : "Retry refresh"}
                    </button>
                )}
            </section>

            {dashboardError && (
                <p className="dashboard-refresh-detail">The displayed auction snapshot is retained, but actions are disabled until refresh succeeds.</p>
            )}

            {isAdmin && (
                <div className="section-card admin-auction-controls">
                    <button className="button" type="button" onClick={endAuction} disabled={auctionEnded || actionsDisabled}>
                        {auctionEnded ? "Auction Ended" : pendingAction === "Ending auction" ? "Ending auction..." : "End Auction"}
                    </button>
                    {auctionEnded && (
                        <button className="button-secondary" type="button" onClick={exportFinalTeamsPdf}>
                            Export Final Teams PDF
                        </button>
                    )}
                    <ActionFeedback
                        feedback={actionFeedback}
                        onRefresh={refreshBeforeRetry}
                        recoveryStatus={outcomeRefreshStatus}
                        onAcknowledge={acknowledgeReviewedState}
                        acknowledgementLabel="I reviewed the refreshed state"
                    />
                </div>
            )}

            {isCaptain && (
                <div className="button-group captain-tabs" style={{ marginBottom: "18px" }}>
                    <button className={dashboardView === "overview" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("overview")}>Current Auction</button>
                    <button className={dashboardView === "standings" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("standings")}>Team Standing</button>
                    <button className={dashboardView === "players" ? "button button-active" : "button-secondary"}
                        onClick={() => setDashboardView("players")}>Player Intelligence</button>
                </div>
            )}

            {showPublicRandomEvent && (
                <section className="live-random-event-banner" role="status" aria-live="polite">
                    <span className="live-random-event-live">LIVE</span>
                    <div className="live-random-event-copy">
                        <p className="live-random-event-kicker">Random event in play</p>
                        <h2>{activeRandomEventType.replaceAll("_", " ")} is live</h2>
                        <p className="live-random-event-description">{activeRandomEventDescription}</p>
                        <p className="live-random-event-banter">
                            Event desk: {RANDOM_EVENT_BANTER[activeRandomEventType]
                                || "The auction rulebook has started improvising."}
                        </p>
                        {auctionStatus?.pendingRandomEventPlayer && (
                            <p className="live-random-event-player">
                                Affecting: <strong>{auctionStatus.pendingRandomEventPlayer}</strong>
                            </p>
                        )}
                    </div>
                </section>
            )}

            {isCaptain && dashboardView === "overview" && (
                <div className="section-card captain-actions" style={{ marginBottom: "18px" }}>
                    <h2>Auction Actions</h2>
                    <ActionFeedback
                        feedback={actionFeedback}
                        onRefresh={refreshBeforeRetry}
                        onAcknowledge={acknowledgeReviewedState}
                        recoveryStatus={outcomeRefreshStatus}
                        acknowledgementLabel="I reviewed the refreshed state"
                    />
                    {silentBidActive && silentRound && !silentRound.submitted && (
                        <div className="captain-action-row silent-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            <input className="input" type="number" min="1"
                                step={Number(silentBid) > 1000 ? 100 : 50}
                                placeholder={pendingAction === "Submitting silent bid" ? "Submitting..." : "Secret silent bid"}
                                value={silentBid} onChange={event => setSilentBid(event.target.value)} />
                            <button className="button" onClick={submitSilentBid} disabled={actionsDisabled}>
                                {pendingAction === "Submitting silent bid" ? "Submitting..." : "Submit Silent Bid"}
                            </button>
                        </div>
                    )}
                    {silentBidActive && silentRound?.submitted && (
                        <p className="message-success">Silent bid submitted. Waiting for the other captains.</p>
                    )}
                    {isBidding && auctionStatus?.auctionPhase !== "SOLD" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            <input className="input" type="number" min="1"
                                step={Number(liveBid) > 1000 ? 100 : 50}
                                placeholder="Enter bid"
                                value={liveBid} onChange={event => setLiveBid(event.target.value)} />
                            <button className="button" onClick={submitLiveBid} disabled={actionsDisabled}>
                                {pendingAction === "Placing bid" ? "Placing bid..." : "Place Bid"}
                            </button>
                        </div>
                    )}
                    {auctionStatus?.auctionPhase === "BLIND_OPENING_BID" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px", marginBottom: "12px" }}>
                            {openingBidStatus && !openingBidStatus.submitted && (
                                <>
                                    <input className="input" type="number" min="1"
                                        data-testid="blind-opening-bid"
                                        step={Number(openingBid) > 1000 ? 100 : 50}
                                        placeholder="Opening bid"
                                        value={openingBid}
                                        onChange={event => setOpeningBid(event.target.value)} />
                                    <button className="button" data-testid="place-blind-opening-bid" onClick={submitOpeningBid} disabled={actionsDisabled}>
                                        {pendingAction === "Submitting opening bid" ? "Submitting..." : "Place Blind Opening Bid"}
                                    </button>
                                    <button className="button-secondary" data-testid="pass-blind-opening-bid" onClick={passOpeningBid} disabled={actionsDisabled}>
                                        {pendingAction === "Passing on opening bid" ? "Passing..." : "Pass"}
                                    </button>
                                </>
                            )}
                            {currentTeam && (
                                <button className="button-secondary" type="button" data-testid="activate-wildcard" onClick={activateWildcard}
                                    disabled={actionsDisabled || currentTeam.wildPickUsed || rtmLocked}>
                                    {pendingAction === "Activating Wildcard" ? "Activating..." : rtmLocked ? "Wild Pick banned by NO MERCY" : currentTeam.wildPickUsed ? "Wild Pick Used" : "Activate Wildcard"}
                                </button>
                            )}
                        </div>
                    )}
                    {auctionStatus?.auctionPhase === "BLIND_OPENING_BID"
                        && openingBidStatus?.submitted && (
                            <p className="message-success">
                                {openingBidStatus.passed
                                    ? "Passed on the opening bid. Waiting for all captains."
                                    : "Opening bid submitted. Waiting for all captains."}
                            </p>
                        )}
                    {auctionStatus?.auctionPhase === "SOLD" && !isCurrentHighestBidder && !rtmLocked && !rtm && !rtmAlreadyUsed && (
                        <button className="button" onClick={claimRtm} disabled={actionsDisabled}>
                            {pendingAction === "Claiming RTM" ? "Claiming..." : "Claim RTM"}
                        </button>
                    )}
                    {rtmLocked && <p>NO MERCY bans RTM and Wild Pick for this player.</p>}
                    {isRtmCaptain && rtm?.status === "CLAIMED" && (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px" }}>
                            <input className="input" type="number" min={currentAuction.currentBid + 100}
                                value={rtmBid}
                                onChange={event => setRtmBid(event.target.value)} placeholder="RTM price" />
                            <button className="button" onClick={submitRtmBid} disabled={actionsDisabled}>
                                {pendingAction === "Submitting RTM bid" ? "Submitting..." : "Submit RTM Price"}
                            </button>
                        </div>
                    )}
                    {isOriginalCaptain && rtm?.status === "BID_SUBMITTED" && (
                        <div>
                            <p>RTM price: <strong>₹{rtm.bidAmount}</strong></p>
                            <div className="captain-action-row rtm-decision-row">
                                <button className="button" onClick={acceptRtm} disabled={actionsDisabled}>
                                    {pendingAction === "Accepting RTM" ? "Accepting..." : "Accept"}
                                </button>
                                <button className="button-secondary" onClick={declineRtm} disabled={actionsDisabled}>
                                    {pendingAction === "Declining RTM" ? "Rejecting..." : "Reject"}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {silentBidActive && allSilentBids.length > 0 && (
                <div className="section-card silent-incoming-card" style={{ marginBottom: "18px" }}>
                    <h2>🔒 Silent Bid Incoming Bids — {allSilentBids[0]?.playerName || currentAuction.currentPlayer}</h2>
                    <div className="silent-bid-matrix-wrap">
                        <table className="standings-table silent-bid-matrix">
                            <thead>
                                <tr>
                                    <th scope="col">Player</th>
                                    {allSilentBids.map(bid => (
                                        <th key={bid.id || bid.captainName} scope="col">{bid.captainName}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td className="silent-bid-player">
                                        {allSilentBids[0]?.playerName || currentAuction.currentPlayer}
                                    </td>
                                    {allSilentBids.map(bid => (
                                        <td key={bid.id || bid.captainName} className="silent-bid-cell">
                                            <span className={bid.submitted ? "silent-bid-status submitted" : "silent-bid-status"}>
                                                {bid.submitted ? "Submitted" : "Waiting"}
                                            </span>
                                        </td>
                                    ))}
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {(dashboardView === "overview" || (!isCaptain && !isAdmin)) && <div className="section-card current-auction">

                <h2>
                    🎤 Current Auction
                </h2>

                <PlayerWalkout
                    players={players.filter(player => !player.sold
                        && (isReAuction || !player.deferredToReAuction)
                        && !clubbedPairs.some(pair =>
                            pair.playerTwo?.toLowerCase() === player.name?.toLowerCase()
                        ))}
                    walkoutPlayers={auctionStatus?.walkoutPlayerNames}
                    selectedPlayer={currentAuction.currentPlayer}
                    spinStartedAt={auctionStatus?.wheelSpinStartedAt}
                    spinEndsAt={auctionStatus?.wheelSpinEndsAt}
                    serverTime={auctionStatus?.serverTime}
                    className="captain-player-walkout"
                    seasonName={auctionStatus?.seasonName}
                />

                {currentPlayerLabel && (
                    <p className="current-player-name" style={{ marginTop: "16px" }}>
                        {currentPlayerLabel}
                    </p>
                )}

                <p style={{ marginTop: "16px" }}>
                    <strong>Current Bid:</strong>{" "}
                    ₹{dashboard.currentAuction.currentBid}
                </p>

            </div>}

            {(dashboardView === "standings" || !isCaptain) && <div className="section-card">

                <h2>
                    🏆 Team Standings
                </h2>

                <div className="intelligence-toolbar">
                    <input
                        className="input"
                        placeholder="Search teams"
                        value={teamQuery}
                        onChange={event => setTeamQuery(event.target.value)}
                    />
                </div>

                <div className="team-grid">

                    {filteredTeams.map(team => (

                        <div
                            key={team.captainName}
                            className="team-card"
                        >

                            <h2>
                                {team.captainName}
                            </h2>

                            <p>
                                💰 Purse: ₹{team.purse}
                            </p>
                            <p>
                                🏆 Tournament Points: {team.tournamentPoints ?? 0}
                            </p>
                            <p>
                                🔄 RTM:
                                {team.rtmAvailable
                                    ? " 🟢 Available"
                                    : " 🔴 Used"}
                            </p>
                            <p>
                                👥 Players Bought: {team.playersBought}
                            </p>
                            <p>
                                📊 Squad Value: ₹{squadValueForTeam(team.captainName)}
                            </p>





                            <div className="squad-title">
                                Squad ({team.squad?.length || 0})
                            </div>

                            {team.squad &&
                                team.squad.length > 0 ? (

                                <ul>

                                    {team.squad.map(player => (

                                        <li key={player}>
                                            {squadPlayerLabel(player)}
                                        </li>

                                    ))}

                                </ul>

                            ) : (

                                <div className="empty-squad">
                                    No players bought yet
                                </div>

                            )}

                        </div>

                    ))}

                </div>

                <div className="comparison-table-wrap">
                    <h3>Team Comparison</h3>
                    <table className="standings-table">
                        <thead>
                            <tr>
                                <th>Team</th>
                                <th>Purse</th>
                                <th>Tournament points</th>
                                <th>Slots left</th>
                                <th>Squad value</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredTeams.map(team => (
                                <tr key={`comparison-${team.captainName}`}>
                                    <td>{team.captainName}</td>
                                    <td>₹{team.purse}</td>
                                    <td>{team.tournamentPoints ?? 0}</td>
                                    <td>{team.playersLeft}</td>
                                    <td>₹{squadValueForTeam(team.captainName)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

            </div>}

            {(dashboardView === "players" || (!isCaptain && !isViewer)) && <div className="section-card">
                <h2>🧭 Player Intelligence</h2>
                <div className="intelligence-toolbar player-toolbar">
                    <input
                        className="input"
                        placeholder="Search players"
                        value={playerQuery}
                        onChange={event => setPlayerQuery(event.target.value)}
                    />
                    <select className="select" value={playerFilter} onChange={event => setPlayerFilter(event.target.value)}>
                        <option value="ALL">All statuses</option>
                        <option value="AVAILABLE">Available</option>
                        <option value="SOLD">Sold</option>
                    </select>
                    <select className="select" value={playerCategory} onChange={event => setPlayerCategory(event.target.value)}>
                        {categories.map(category => (
                            <option key={category} value={category}>
                                {category === "ALL" ? "All categories" : category}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="player-directory-wrap">
                    <table className="standings-table player-directory-table">
                        <thead>
                            <tr>
                                <th>Player</th>
                                <th>Category</th>
                                <th>Status</th>
                                <th>Price</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredPlayers.slice(0, 24).map(player => (
                                <tr key={player.id || player.name}>
                                    <td><strong>{player.name}</strong></td>
                                    <td>
                                        <span className="category-badge">
                                            {player.category || player.seed || "Uncategorized"}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={player.sold ? "status-badge sold" : "status-badge available"}>
                                            {player.sold
                                                ? "Sold"
                                                : player.deferredToReAuction
                                                    ? "Held for re-auction"
                                                    : "Available"}
                                        </span>
                                    </td>
                                    <td>{player.sold
                                        ? `Final ₹${effectiveSaleValue(player)}`
                                        : `Base ₹${player.basePrice}`}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    {filteredPlayers.length === 0 && <p className="table-empty">No players match these filters.</p>}
                    {filteredPlayers.length > 24 && <p className="table-caption">Showing 24 of {filteredPlayers.length} players</p>}
                </div>
            </div>}
            {auctionStatus?.auctionRound === 3 && (
                <div className="section-card">
                    <h2>🔄 Unsold Players After Re-auction</h2>
                    <p>These players remain unsold. Admin can sell them manually.</p>
                    {players.filter(player => !player.sold).length > 0 ? (
                        <div className="player-directory-wrap">
                            <table className="standings-table player-directory-table">
                                <thead>
                                    <tr>
                                        <th>Player</th>
                                        <th>Category</th>
                                        <th>Original Base Price</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {players.filter(player => !player.sold).map(player => (
                                        <tr key={`unsold-${player.id || player.name}`}>
                                            <td><strong>{player.name}</strong></td>
                                            <td>{player.category || player.seed || "Uncategorized"}</td>
                                            <td>₹{player.basePrice}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <p className="table-empty">No players remain unsold.</p>
                    )}
                </div>
            )}
            {isCaptain && auctionStatus?.curseSelectionEnabled && !auctionStatus?.auctionStarted && (
                <div className="section-card">
                    <h2>Secret Curse Picks</h2>
                    <p>Choose two distinct players. Other captains will not see your picks until a sale is finalized.</p>
                    {curseSelections.map(curse => (
                        <div key={curse.id}>{curse.playerName}</div>
                    ))}
                    {curseSelections.length < 2 ? (
                        <div className="captain-action-row" style={{ display: "flex", gap: "8px" }}>
                            <select
                                className="input"
                                aria-label="Select a player to curse"
                                value={newCursePlayer}
                                onChange={event => setNewCursePlayer(event.target.value)}
                            >
                                <option value="">Select a player</option>
                                {players.filter(player => !player.sold
                                    && !curseSelections.some(curse => curse.playerName === player.name))
                                    .map(player => <option key={player.id || player.name} value={player.name}>{player.name}</option>)}
                            </select>
                            <button
                                className="button"
                                type="button"
                                onClick={submitCursePlayer}
                                disabled={!newCursePlayer}
                            >
                                Place Curse
                            </button>
                        </div>
                    ) : (
                        <p className="message-success">Both Curse picks are locked until their players are sold.</p>
                    )}
                </div>
            )}

            {(dashboardView === "overview" || !isCaptain) && <div className="event-columns">
                <div className="section-card event-panel">

                    <h2>
                        📢 Special Events
                    </h2>

                    {specialEvents.length === 0 && (

                        <p>
                            No special events yet
                        </p>

                    )}

                    {specialEvents.map(event => (

                        <div
                            key={event.id}
                            className="activity-item"
                        >

                            <strong>

                                {event.eventType ===
                                    "BOUNTY" &&
                                    "🎁 BOUNTY"}

                                {event.eventType ===
                                    "GOLDEN_BOUNTY" &&
                                    "🏆 GOLDEN BOUNTY"}

                                {event.eventType ===
                                    "CAPTAIN_TRIBUNAL" &&
                                    "⚖️ CAPTAIN TRIBUNAL"}

                                {event.eventType === "RTM_TRIGGERED" &&
                                    "🔄 RTM ACTIVATED"}

                                {event.eventType === "RTM_BID_SUBMITTED" &&
                                    "📝 RTM BID SUBMITTED"}

                                {event.eventType === "RTM_ACCEPTED" &&
                                    "✅ RTM ACCEPTED"}

                                {event.eventType === "RTM_DECLINED" &&
                                    "❌ RTM DECLINED"}

                                {event.eventType === "WILDCARD_TRIGGERED" &&
                                    "🃏 WILDCARD TRIGGERED"}

                                {event.eventType === "CURSE_REVEALED" &&
                                    "🎯 CURSE TRIGGERED"}

                                {event.eventType === "CURSE_CANCELLED" &&
                                    "🎯 CURSE CANCELLED"}

                                {event.eventType === "BLIND_BID_BONUS" &&
                                    "🏆 BLIND BID BONUS"}

                                {event.eventType === "BLIND_BONANZA" &&
                                    "🎊 BLIND BONANZA IN PLAY"}

                                {event.eventType === "BLIND_BONANZA_REWARDED" &&
                                    "🎊 BLIND BONANZA AWARDED"}

                            </strong>

                            <>

                                <div>

                                    {event.playerName}
                                    {event.captainName && ` → ${event.captainName}`}

                                </div>

                                <div>
                                    {event.details}
                                </div>

                                {event.amount !== 0 && (
                                    <div>
                                        {["CURSE_REVEALED", "CURSE_CANCELLED", "BLIND_BID_BONUS", "BLIND_BONANZA", "BLIND_BONANZA_REWARDED"].includes(event.eventType)
                                            ? `${event.amount > 0 ? "+" : ""}${event.amount} tournament points`
                                            : `₹${event.amount > 0 ? "+" : ""}${event.amount}`}
                                    </div>
                                )}

                            </>

                        </div>

                    ))}

                </div>

                <div className="event-panel">
                    <LiveActivity />
                </div>
            </div>}

            <section className="pdf-export-report" aria-label="Final teams report">
                <h1>BTC Auction Final Teams</h1>
                <p>Final team standings</p>
                {dashboard.teams?.map(team => (
                    <article key={`pdf-${team.captainName}`}>
                        <h2>{team.captainName}</h2>
                        <p>Purse: ₹{team.purse} | Tournament points: {team.tournamentPoints ?? 0} | Players bought: {team.playersBought} | Slots left: {team.playersLeft}</p>
                        <p>Squad value: ₹{squadValueForTeam(team.captainName)}</p>
                        <p>Squad: {team.squad?.length
                            ? team.squad.map(squadPlayerLabel).join(", ")
                            : "No players bought"}</p>
                    </article>
                ))}
            </section>

        </div>

    );
}

export default Dashboard;