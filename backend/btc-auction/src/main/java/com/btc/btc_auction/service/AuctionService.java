package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.RandomEventEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.enums.RandomEventType;
import com.btc.btc_auction.model.Auction;

import java.util.List;
import java.util.function.DoubleSupplier;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuctionService {

        private static final double RANDOM_EVENT_PROBABILITY = 0.75;
        private static final long WHEEL_SPIN_LEAD_TIME_MILLIS = 400;
        private static final long WHEEL_SPIN_MIN_DURATION_MILLIS = 5000;
        private static final long WHEEL_SPIN_MAX_DURATION_MILLIS = 7000;
        private static final int WHEEL_SPIN_MAX_PLAYERS = 35;
        @Value("${btc.auction.random-event-probability:0.75}")
        private double randomEventProbability = RANDOM_EVENT_PROBABILITY;

        private String pendingSilentAuctionPlayer;
        private String currentPlayerRandomEventPlayer;
        private RandomEventEntity currentPlayerRandomEvent;
        private boolean marketCrashApplied;

        private final CurrentAuctionService currentAuctionService;
        private final TeamService teamService;
        private final PlayerService playerService;
        private final AuctionLogService auctionLogService;
        private final AdminActionLogService adminActionLogService;
        private final AuctionEventService auctionEventService;
        private final AuctionConfigService auctionConfigService;
        private final RtmService rtmService;
        private final AuctionSocketService auctionSocketService;
        private final ClubbedPlayerPairService clubbedPlayerPairService;
        private final RandomEventService randomEventService;
        private final BlindOpeningBidService blindOpeningBidService;
        private final SilentBidService silentBidService;
        private final ReAuctionSnapshotService reAuctionSnapshotService;
        private final CursePlayerService cursePlayerService;
        private EffectivePurseService effectivePurseService;
        private BidAffordabilityService bidAffordabilityService;
        private final DoubleSupplier randomValueSupplier;

        public AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService,
                        ClubbedPlayerPairService clubbedPlayerPairService) {
                this(teamService, playerService, auctionLogService, adminActionLogService,
                                auctionEventService, auctionConfigService,
                                rtmService, auctionSocketService,
                                currentAuctionService, clubbedPlayerPairService,
                                null, null, null, null, Math::random);
        }

        @org.springframework.beans.factory.annotation.Autowired
        public AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService,
                        ClubbedPlayerPairService clubbedPlayerPairService,
                        RandomEventService randomEventService,
                        BlindOpeningBidService blindOpeningBidService,
                        @Lazy SilentBidService silentBidService,
                        ReAuctionSnapshotService reAuctionSnapshotService,
                        CursePlayerService cursePlayerService,
                        EffectivePurseService effectivePurseService,
                        BidAffordabilityService bidAffordabilityService) {

                this(teamService, playerService, auctionLogService, adminActionLogService,
                                auctionEventService, auctionConfigService,
                                rtmService, auctionSocketService,
                                currentAuctionService, clubbedPlayerPairService,
                                randomEventService, blindOpeningBidService,
                                silentBidService, reAuctionSnapshotService,
                                Math::random, cursePlayerService);
                this.effectivePurseService = effectivePurseService;
                this.bidAffordabilityService = bidAffordabilityService;
        }

        AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService,
                        ClubbedPlayerPairService clubbedPlayerPairService,
                        RandomEventService randomEventService,
                        BlindOpeningBidService blindOpeningBidService,
                        SilentBidService silentBidService,
                        DoubleSupplier randomValueSupplier) {

                this(teamService, playerService, auctionLogService, adminActionLogService,
                                auctionEventService, auctionConfigService,
                                rtmService, auctionSocketService,
                                currentAuctionService, clubbedPlayerPairService,
                                randomEventService, blindOpeningBidService,
                                silentBidService, null, randomValueSupplier);
        }

        AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService,
                        ClubbedPlayerPairService clubbedPlayerPairService,
                        RandomEventService randomEventService,
                        BlindOpeningBidService blindOpeningBidService,
                        SilentBidService silentBidService,
                        ReAuctionSnapshotService reAuctionSnapshotService,
                        DoubleSupplier randomValueSupplier) {
                this(teamService, playerService, auctionLogService, adminActionLogService, auctionEventService,
                                auctionConfigService, rtmService, auctionSocketService, currentAuctionService,
                                clubbedPlayerPairService, randomEventService, blindOpeningBidService,
                                silentBidService, reAuctionSnapshotService, randomValueSupplier,
                                null);
        }

        AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService,
                        ClubbedPlayerPairService clubbedPlayerPairService,
                        RandomEventService randomEventService,
                        BlindOpeningBidService blindOpeningBidService,
                        SilentBidService silentBidService,
                        ReAuctionSnapshotService reAuctionSnapshotService,
                        DoubleSupplier randomValueSupplier,
                        CursePlayerService cursePlayerService) {

                this.teamService = teamService;
                this.playerService = playerService;
                this.auctionLogService = auctionLogService;
                this.adminActionLogService = adminActionLogService;
                this.auctionEventService = auctionEventService;
                this.auctionConfigService = auctionConfigService;
                this.rtmService = rtmService;
                this.currentAuctionService = currentAuctionService;
                this.auctionSocketService = auctionSocketService;
                this.clubbedPlayerPairService = clubbedPlayerPairService;
                this.randomEventService = randomEventService;
                this.blindOpeningBidService = blindOpeningBidService;
                this.silentBidService = silentBidService;
                this.reAuctionSnapshotService = reAuctionSnapshotService;
                this.cursePlayerService = cursePlayerService;
                this.randomValueSupplier = randomValueSupplier;
        }

        @EventListener(ApplicationReadyEvent.class)
        public synchronized void restorePersistedRuntimeState() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config == null) {
                        return;
                }

                pendingSilentAuctionPlayer = config.getPendingSilentAuctionPlayer();
                marketCrashApplied = config.isMarketCrashApplied();

                RandomEventType eventType = config.getPendingRandomEventType();
                String eventPlayer = config.getPendingRandomEventPlayer();
                if (eventType == null || eventPlayer == null || eventPlayer.isBlank()) {
                        currentPlayerRandomEvent = null;
                        currentPlayerRandomEventPlayer = null;
                        return;
                }

                RandomEventEntity event = new RandomEventEntity();
                event.setType(eventType);
                event.setTitle(config.getPendingRandomEventTitle());
                event.setDescription(config.getPendingRandomEventDescription());
                event.setAmount(config.getPendingRandomEventAmount());
                currentPlayerRandomEvent = event;
                currentPlayerRandomEventPlayer = eventPlayer;
        }

        public Auction getCurrentAuction() {

                Auction auction = currentAuctionService.getCurrentAuction();
                if (auction != null && !auction.getCurrentPlayer().isBlank()) {
                        PlayerEntity player = playerService.getPlayer(auction.getCurrentPlayer());
                        if (player != null && player.isSold()) {
                                currentAuctionService.setCurrentAuction(
                                                new Auction("", "", 0, "None", 0));
                                return currentAuctionService.getCurrentAuction();
                        }
                }
                return auction;

        }

        public synchronized String spinWheel() {
                List<PlayerEntity> availablePlayers = playerService.getUnsoldPlayers();
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (!config.isAuctionStarted()) {
                        return "Start the auction before spinning the wheel.";
                }
                Auction activeAuction = currentAuctionService.getCurrentAuction();
                if (activeAuction != null && activeAuction.getCurrentPlayer() != null
                                && !activeAuction.getCurrentPlayer().isBlank()) {
                        return "Complete or return the current player before spinning the wheel again.";
                }
                if (availablePlayers != null && config.getAuctionRound() == 2) {
                        availablePlayers = availablePlayers.stream()
                                        .filter(player -> !player.isReAuctioned())
                                        .toList();
                }
                if (availablePlayers != null && config.getAuctionRound() == 1) {
                        availablePlayers = availablePlayers.stream()
                                        .filter(player -> !player.isDeferredToReAuction())
                                        .toList();
                }
                if (availablePlayers != null) {
                        availablePlayers = availablePlayers.stream()
                                        .filter(player -> !"C".equalsIgnoreCase(player.getSeed()))
                                        .filter(player -> !clubbedPlayerPairService.isPlayerTwo(player.getName()))
                                        .toList();
                }
                if (availablePlayers == null || availablePlayers.isEmpty()) {
                        if (config.getAuctionRound() == 2) {
                                if (cursePlayerService != null) {
                                        cursePlayerService.expireForUnsoldPlayers();
                                }
                                config.setAuctionRound(3);
                                config.setAuctionStarted(false);
                                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                                auctionConfigService.save(config);
                                if (reAuctionSnapshotService != null) {
                                        reAuctionSnapshotService.clear();
                                }
                                auctionSocketService.broadcastRefresh();
                                return "Re-auction completed. Remaining unsold players are displayed for admin manual sale.";
                        }
                        return "No available players left to spin.";
                }

                RandomEventEntity scheduledEvent = selectRandomEventForNextPlayer(config);
                PlayerEntity selected = availablePlayers.get(
                                (int) (randomValueSupplier.getAsDouble() * availablePlayers.size()));

                int basePrice = config.getAuctionRound() == 2
                                ? Math.max(1, selected.getBasePrice() / 2)
                                : selected.getBasePrice();
                if (config.getAuctionRound() == 2) {
                        selected.setReAuctioned(true);
                        playerService.savePlayer(selected);
                }

                Auction auction = new Auction(
                                selected.getName(),
                                selected.getSeed(),
                                basePrice,
                                "None",
                                basePrice);

                currentAuctionService.setCurrentAuction(auction);
                long wheelSpinStartedAt = System.currentTimeMillis() + WHEEL_SPIN_LEAD_TIME_MILLIS;
                config.setWheelSpinStartedAt(wheelSpinStartedAt);
                config.setWheelSpinEndsAt(
                                wheelSpinStartedAt + calculateWheelSpinDuration(availablePlayers.size()));
                config.setAuctionPhase(AuctionPhase.SPINNING);
                assignRandomEventToPlayer(scheduledEvent, selected.getName(), config);
                auctionSocketService.broadcastRefresh();

                return "Wheel spinning. Nominee will be revealed shortly.";
        }

        @Transactional
        public synchronized String markCurrentPlayerUnsold() {
                Auction auction = currentAuctionService.getCurrentAuction();
                AuctionConfigEntity config = auctionConfigService.getConfig();

                if (auction == null || auction.getCurrentPlayer() == null || auction.getCurrentPlayer().isBlank()) {
                        return "No active auction.";
                }

                if (config.getAuctionPhase() == AuctionPhase.SPINNING) {
                        return "Wait for the wheel spin to finish.";
                }

                if (config.getAuctionPhase() == AuctionPhase.SOLD) {
                        return "Resolve the SOLD and RTM window before marking this player unsold.";
                }

                PlayerEntity player = playerService.getPlayer(auction.getCurrentPlayer());
                if (player == null) {
                        return "Player not found.";
                }

                if (player.isSold()) {
                        return "A sold player cannot be marked unsold from the active auction.";
                }

                if (config.getAuctionRound() == 1) {
                        player.setDeferredToReAuction(true);
                } else if (config.getAuctionRound() == 2) {
                        player.setReAuctioned(true);
                }
                playerService.savePlayer(player);

                clearRandomEventForPlayer(player.getName(), config);
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.clearRound(player.getName());
                }
                if (silentBidService != null) {
                        silentBidService.clearRound();
                }
                currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0, ""));
                clearWheelSpinState(config);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);

                auctionEventService.logEvent(
                                "PLAYER_MARKED_UNSOLD",
                                player.getName(),
                                null,
                                0,
                                config.getAuctionRound() == 1
                                                ? "Player deferred to the re-auction pool."
                                                : "Player remains unsold after re-auction.");
                auctionSocketService.broadcastRefresh();

                return config.getAuctionRound() == 1
                                ? player.getName() + " marked unsold and moved to the re-auction pool."
                                : player.getName() + " marked unsold after re-auction.";
        }

        @Scheduled(fixedDelay = 100)
        public synchronized void completeWheelSpinWhenDue() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config == null || config.getAuctionPhase() != AuctionPhase.SPINNING) {
                        return;
                }

                Long wheelSpinEndsAt = config.getWheelSpinEndsAt();
                if (wheelSpinEndsAt == null || System.currentTimeMillis() < wheelSpinEndsAt) {
                        return;
                }

                Auction auction = currentAuctionService.getCurrentAuction();
                clearWheelSpinState(config);
                if (auction == null || auction.getCurrentPlayer() == null || auction.getCurrentPlayer().isBlank()) {
                        config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                        auctionConfigService.save(config);
                        auctionSocketService.broadcastRefresh();
                        return;
                }

                String playerName = auction.getCurrentPlayer();
                if (config.getPendingRandomEventType() == RandomEventType.SILENT_AUCTION) {
                        config.setAuctionPhase(AuctionPhase.SILENT_BID);
                        auctionConfigService.save(config);
                        if (silentBidService != null) {
                                silentBidService.startRound(playerName);
                        }
                } else if (blindOpeningBidService != null) {
                        config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
                        auctionConfigService.save(config);
                        blindOpeningBidService.startRound(playerName);
                } else {
                        config.setAuctionPhase(AuctionPhase.OPENING_BID);
                        auctionConfigService.save(config);
                }

                logScheduledRandomEvent(config, playerName);
                auctionSocketService.broadcastRefresh();
        }

        private long calculateWheelSpinDuration(int playerCount) {
                if (playerCount <= 1) {
                        return WHEEL_SPIN_MIN_DURATION_MILLIS;
                }
                if (playerCount >= WHEEL_SPIN_MAX_PLAYERS) {
                        return WHEEL_SPIN_MAX_DURATION_MILLIS;
                }

                return Math.round(WHEEL_SPIN_MIN_DURATION_MILLIS
                                + ((double) (playerCount - 1) / (WHEEL_SPIN_MAX_PLAYERS - 1))
                                                * (WHEEL_SPIN_MAX_DURATION_MILLIS - WHEEL_SPIN_MIN_DURATION_MILLIS));
        }

        @Transactional
        public synchronized String startReAuction() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionRound() != 1) {
                        return "The one allowed re-auction round has already been used.";
                }
                if (config.isAuctionStarted()) {
                        return "End the main auction before starting the re-auction.";
                }

                Auction activeAuction = currentAuctionService.getCurrentAuction();
                if (activeAuction != null && activeAuction.getCurrentPlayer() != null
                                && !activeAuction.getCurrentPlayer().isBlank()) {
                        return "Complete or mark the current player unsold before restarting the auction.";
                }

                if (reAuctionSnapshotService != null) {
                        reAuctionSnapshotService.capture(config.getBlindBonanzaAwardsUsed());
                }

                playerService.getUnsoldPlayers().forEach(player -> {
                        player.setDeferredToReAuction(false);
                        player.setReAuctioned(false);
                        playerService.savePlayer(player);
                });

                config.setAuctionRound(2);
                config.setAuctionStarted(true);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);
                auctionSocketService.broadcastRefresh();
                return "Re-auction started. Unsold players will return at 50% of their original base price.";
        }

        @Transactional
        public synchronized String cancelReAuction() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionRound() != 2) {
                        return "No active re-auction to cancel.";
                }
                if (reAuctionSnapshotService == null || !reAuctionSnapshotService.hasSnapshot()) {
                        return "Cannot cancel re-auction because its starting snapshot is unavailable.";
                }

                Auction activeAuction = currentAuctionService.getCurrentAuction();
                if (activeAuction != null && activeAuction.getCurrentPlayer() != null
                                && !activeAuction.getCurrentPlayer().isBlank()
                                && blindOpeningBidService != null) {
                        blindOpeningBidService.clearRound(activeAuction.getCurrentPlayer());
                }
                if (silentBidService != null) {
                        silentBidService.clearRound();
                }

                int restoredBlindBonanzaAwardsUsed = reAuctionSnapshotService.restore();
                reconcileEffectivePurses();
                playerService.getAllPlayers().stream()
                                .filter(player -> !player.isSold())
                                .forEach(player -> {
                                        player.setDeferredToReAuction(false);
                                        player.setReAuctioned(false);
                                        playerService.savePlayer(player);
                                });

                pendingSilentAuctionPlayer = null;
                currentPlayerRandomEventPlayer = null;
                currentPlayerRandomEvent = null;
                marketCrashApplied = false;
                persistRuntimeState(config);
                config.setBlindBonanzaAwardsUsed(restoredBlindBonanzaAwardsUsed);
                config.setRtmLockdownPlayer(null);
                config.setMarketAdjustmentPlayer(null);
                config.setMarketAdjustment(0);
                clearWheelSpinState(config);
                config.setAuctionRound(1);
                config.setAuctionStarted(true);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);

                currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0, ""));
                auctionEventService.logEvent(
                                "RE_AUCTION_CANCELLED",
                                null,
                                null,
                                0,
                                "Re-auction sales were rolled back and unsold round-one players returned to the wheel.");
                auctionSocketService.broadcastRefresh();

                return "Re-auction cancelled. Re-auction sales were rolled back and unsold players returned to the wheel.";
        }

        public synchronized String startBlindOpeningBid(String playerName) {
                if (blindOpeningBidService == null) {
                        return "Blind opening bid system unavailable.";
                }
                if (playerName == null || playerName.isBlank()) {
                        return "Player name is required.";
                }
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionPhase() == AuctionPhase.SPINNING) {
                        return "Wait for the wheel spin to finish.";
                }
                if (config.getAuctionPhase() == AuctionPhase.SILENT_BID) {
                        return "Silent bidding is active for the current player.";
                }
                blindOpeningBidService.startRound(playerName);
                config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
                auctionConfigService.save(config);
                return "Blind opening bids started for " + playerName + ".";
        }

        @Transactional
        public synchronized String useWildPick(String captainName) {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionPhase() != AuctionPhase.OPENING_BID
                                && config.getAuctionPhase() != AuctionPhase.BLIND_OPENING_BID) {
                        return "Wildcard can only be activated before bidding starts.";
                }

                Auction auction = currentAuctionService.getCurrentAuction();
                if (auction == null || auction.getCurrentPlayer() == null
                                || auction.getCurrentPlayer().isBlank()) {
                        return "No player is currently selected.";
                }

                if (auction.getCurrentPlayer().equalsIgnoreCase(config.getRtmLockdownPlayer())) {
                        return "NO MERCY bans RTM and Wild Pick for this player.";
                }

                if (config.getAuctionPhase() == AuctionPhase.BLIND_OPENING_BID
                                && blindOpeningBidService != null) {
                        List<BlindOpeningBidEntity> blindOpeningBids = blindOpeningBidService
                                        .getAllBids(auction.getCurrentPlayer());
                        boolean allBlindOpeningBidsSubmitted = !blindOpeningBids.isEmpty();
                        for (BlindOpeningBidEntity bid : blindOpeningBids) {
                                if (!bid.isSubmitted()) {
                                        allBlindOpeningBidsSubmitted = false;
                                        break;
                                }
                        }
                        if (allBlindOpeningBidsSubmitted) {
                                return "Wildcard must be used before all Blind Opening Bids are placed.";
                        }
                }

                TeamEntity team = teamService.getTeam(captainName);
                if (team == null) {
                        return "Captain not found.";
                }
                if (team.isWildPickUsed()) {
                        return "Wild Pick has already been used.";
                }

                team.setWildPickUsed(true);
                teamService.saveTeam(team);
                clearRandomEventForPlayer(auction.getCurrentPlayer(), config);
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.clearRound(auction.getCurrentPlayer());
                }
                currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0));
                clearWheelSpinState(config);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);
                auctionEventService.logEvent(
                                "WILDCARD_TRIGGERED",
                                auction.getCurrentPlayer(),
                                captainName,
                                0,
                                captainName + " activated Wildcard. The player was returned to the nomination lot. "
                                                + "Admin, spin the wheel again.");
                auctionSocketService.broadcastRefresh();
                return captainName + " activated Wildcard. The player was returned to the wheel. "
                                + "Admin, spin the wheel again.";
        }

        public synchronized String submitBlindOpeningBid(String playerName, String captainName, int bidAmount) {
                if (blindOpeningBidService == null) {
                        return "Blind opening bid system unavailable.";
                }
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionPhase() != AuctionPhase.BLIND_OPENING_BID) {
                        return "Blind opening bids are not active.";
                }
                String result = blindOpeningBidService.submitBid(playerName, captainName, bidAmount);
                if ("Blind opening bid submitted.".equals(result)
                                && blindOpeningBidService.getAllBids(playerName).stream()
                                                .allMatch(bid -> bid != null && bid.isSubmitted())) {
                        BlindOpeningBidEntity winner = blindOpeningBidService.revealWinner(playerName);
                        if (winner != null) {
                                config.setAuctionPhase(AuctionPhase.BIDDING);
                                auctionConfigService.save(config);
                        }
                }
                return result;
        }

        public synchronized String passBlindOpeningBid(String playerName, String captainName) {
                if (blindOpeningBidService == null) {
                        return "Blind opening bid system unavailable.";
                }
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionPhase() != AuctionPhase.BLIND_OPENING_BID) {
                        return "Blind opening bids are not active.";
                }
                String result = blindOpeningBidService.passBid(playerName, captainName);
                if ("Blind opening bid passed.".equals(result)
                                && blindOpeningBidService.getAllBids(playerName).stream()
                                                .allMatch(bid -> bid != null && bid.isSubmitted())) {
                        blindOpeningBidService.revealWinner(playerName);
                }
                return result;
        }

        public boolean allBlindOpeningBidsPassed(String playerName) {
                return blindOpeningBidService != null && blindOpeningBidService.allCaptainsPassed(playerName);
        }

        public BlindOpeningBidEntity getBlindOpeningBid(String playerName, String captainName) {
                if (blindOpeningBidService == null) {
                        return null;
                }
                return blindOpeningBidService.getAllBids(playerName).stream()
                                .filter(bid -> bid.getCaptainName().equalsIgnoreCase(captainName))
                                .findFirst()
                                .orElse(null);
        }

        public List<BlindOpeningBidEntity> getAllBlindOpeningBids(String playerName) {
                if (blindOpeningBidService == null) {
                        return List.of();
                }
                return blindOpeningBidService.getAllBids(playerName);
        }

        public synchronized String revealBlindOpeningBid(String playerName) {
                if (blindOpeningBidService == null) {
                        return "Blind opening bid system unavailable.";
                }
                BlindOpeningBidEntity winner = blindOpeningBidService.revealWinner(playerName);
                if (winner == null) {
                        return "Blind opening bids are not ready to reveal yet.";
                }

                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setAuctionPhase(AuctionPhase.BIDDING);
                auctionConfigService.save(config);
                return "Blind opening bid winner: " + winner.getCaptainName() + " at ₹" + winner.getBidAmount();
        }

        public synchronized void activateSilentAuctionLock(String playerName) {
                if (playerName == null || playerName.isBlank()) {
                        return;
                }
                this.pendingSilentAuctionPlayer = playerName;
                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setAuctionPhase(AuctionPhase.SILENT_BID);
                persistRuntimeState(config);
                if (config != null) {
                        auctionConfigService.save(config);
                }
        }

        public synchronized String triggerRandomEventNow() {
                return "Random events are assigned automatically before each wheel spin.";
        }

        private RandomEventEntity selectRandomEventForNextPlayer(AuctionConfigEntity config) {
                if (randomEventService == null
                                || randomValueSupplier.getAsDouble() >= randomEventProbability) {
                        return null;
                }

                List<RandomEventEntity> activeEvents = randomEventService.getActiveEvents();
                if (activeEvents == null || activeEvents.isEmpty()) {
                        return null;
                }

                List<RandomEventEntity> eligibleEvents = activeEvents.stream()
                                .filter(event -> event.getType() != RandomEventType.BLIND_BONANZA
                                                || config.getBlindBonanzaAwardsUsed() < 4)
                                .toList();
                if (eligibleEvents.isEmpty()) {
                        return null;
                }

                int selectedIndex = Math.min(
                                eligibleEvents.size() - 1,
                                (int) (randomValueSupplier.getAsDouble() * eligibleEvents.size()));
                return eligibleEvents.get(selectedIndex);
        }

        private void assignRandomEventToPlayer(
                        RandomEventEntity event, String playerName, AuctionConfigEntity config) {
                currentPlayerRandomEvent = event;
                currentPlayerRandomEventPlayer = event == null ? null : playerName;
                marketCrashApplied = false;

                if (config == null) {
                        return;
                }

                if (event == null || event.getType() == RandomEventType.MARKET_CRASH) {
                        persistRuntimeState(config);
                        auctionConfigService.save(config);
                        return;
                }

                switch (event.getType()) {
                        case MARKET_BOOM:
                                config.setMarketAdjustmentPlayer(playerName);
                                config.setMarketAdjustment(event.getAmount() == null ? 100 : event.getAmount());
                                break;
                        case RTM_LOCKDOWN:
                                config.setRtmLockdownPlayer(playerName);
                                break;
                        case SILENT_AUCTION:
                                pendingSilentAuctionPlayer = playerName;
                                break;
                        case BLIND_BONANZA:
                        case MARKET_CRASH:
                                break;
                }

                persistRuntimeState(config);
                auctionConfigService.save(config);
        }

        private void logScheduledRandomEvent(AuctionConfigEntity config, String playerName) {
                RandomEventType eventType = config.getPendingRandomEventType();
                if (eventType == null || eventType == RandomEventType.MARKET_CRASH) {
                        return;
                }

                auctionEventService.logEvent(
                                eventType.name(),
                                playerName,
                                null,
                                config.getPendingRandomEventAmount(),
                                config.getPendingRandomEventDescription());
        }

        private void clearWheelSpinState(AuctionConfigEntity config) {
                config.setWheelSpinStartedAt(null);
                config.setWheelSpinEndsAt(null);
        }

        private RandomEventEntity triggerMarketCrashForCurrentPlayer(
                        Auction auction, AuctionConfigEntity config) {
                if (marketCrashApplied
                                || currentPlayerRandomEvent == null
                                || currentPlayerRandomEvent.getType() != RandomEventType.MARKET_CRASH
                                || currentPlayerRandomEventPlayer == null
                                || config == null
                                || !currentPlayerRandomEventPlayer.equalsIgnoreCase(auction.getCurrentPlayer())) {
                        return null;
                }

                int amount = currentPlayerRandomEvent.getAmount() == null
                                ? 100
                                : Math.abs(currentPlayerRandomEvent.getAmount());
                config.setMarketAdjustmentPlayer(auction.getCurrentPlayer());
                config.setMarketAdjustment(-amount);
                marketCrashApplied = true;
                persistRuntimeState(config);
                auctionConfigService.save(config);
                return currentPlayerRandomEvent;
        }

        private void logRandomEvent(RandomEventEntity event, String playerName) {
                auctionEventService.logEvent(
                                event.getType().name(),
                                playerName,
                                null,
                                event.getAmount(),
                                event.getDescription());
        }

        private void clearRandomEventForPlayer(String playerName, AuctionConfigEntity config) {
                if (currentPlayerRandomEventPlayer == null
                                || !currentPlayerRandomEventPlayer.equalsIgnoreCase(playerName)) {
                        return;
                }

                if (playerName.equalsIgnoreCase(config.getMarketAdjustmentPlayer())) {
                        config.setMarketAdjustmentPlayer(null);
                        config.setMarketAdjustment(0);
                }
                if (playerName.equalsIgnoreCase(config.getRtmLockdownPlayer())) {
                        config.setRtmLockdownPlayer(null);
                }
                if (playerName.equalsIgnoreCase(pendingSilentAuctionPlayer)) {
                        pendingSilentAuctionPlayer = null;
                }

                currentPlayerRandomEventPlayer = null;
                currentPlayerRandomEvent = null;
                marketCrashApplied = false;
                persistRuntimeState(config);
        }

        private void persistRuntimeState(AuctionConfigEntity config) {
                if (config == null) {
                        return;
                }

                config.setPendingSilentAuctionPlayer(pendingSilentAuctionPlayer);
                config.setMarketCrashApplied(marketCrashApplied);
                config.setPendingRandomEventPlayer(currentPlayerRandomEventPlayer);
                if (currentPlayerRandomEvent == null) {
                        config.setPendingRandomEventType(null);
                        config.setPendingRandomEventTitle(null);
                        config.setPendingRandomEventDescription(null);
                        config.setPendingRandomEventAmount(null);
                        return;
                }

                config.setPendingRandomEventType(currentPlayerRandomEvent.getType());
                config.setPendingRandomEventTitle(currentPlayerRandomEvent.getTitle());
                config.setPendingRandomEventDescription(currentPlayerRandomEvent.getDescription());
                config.setPendingRandomEventAmount(currentPlayerRandomEvent.getAmount());
        }

        @Transactional
        public synchronized String sellPlayer(
                        String playerName,
                        String captainName,
                        int soldPrice) {

                Auction auction = currentAuctionService.getCurrentAuction();
                AuctionConfigEntity config = auctionConfigService.getConfig();

                if (auction == null || auction.getCurrentPlayer().isBlank()) {
                        return "No active auction";
                }

                if (config.getAuctionPhase() != AuctionPhase.SOLD) {
                        return "Call SOLD before confirming the sale";
                }

                if (!auction.getCurrentPlayer().equalsIgnoreCase(playerName)
                                || !auction.getLeader().equalsIgnoreCase(captainName)
                                || auction.getCurrentBid() != soldPrice) {
                        return "Sale must match the current SOLD player, leader, and bid";
                }

                TeamEntity team = teamService.getTeam(captainName);
                int penalty = 0;

                if (team == null) {
                        return "Team not found";
                }

                int purseBeforeSale = team.getPurse();
                ClubbedPlayerPairEntity activePair = clubbedPlayerPairService.getPairForPlayer(playerName).orElse(null);
                String pairedPlayerName = null;
                if (activePair != null) {
                        if (activePair.getPlayerOne().equalsIgnoreCase(playerName)) {
                                pairedPlayerName = activePair.getPlayerTwo();
                        } else if (activePair.getPlayerTwo().equalsIgnoreCase(playerName)) {
                                pairedPlayerName = activePair.getPlayerOne();
                        }
                }
                int packageSize = pairedPlayerName == null ? 1 : 2;
                if (team.getPlayersLeft() < packageSize) {
                        return "Squad is already complete";
                }

                PlayerEntity player = playerService.getPlayer(playerName);

                if (player == null) {
                        return "Player not found";
                }

                int marketAdjustment = getMarketAdjustment(config, playerName);
                String affordabilityError = validateAffordability(playerName, captainName, soldPrice);
                if (affordabilityError != null) {
                        return affordabilityError;
                }
                int finalSalePrice = Math.max(0, soldPrice + marketAdjustment);
                if (marketAdjustment != 0) {
                        config.setMarketAdjustmentPlayer(null);
                        config.setMarketAdjustment(0);
                }

                player.setSold(true);
                player.setSoldPrice(finalSalePrice);
                player.setFinalPrice(finalSalePrice);
                player.setTeam(captainName);

                playerService.savePlayer(player);

                if (pairedPlayerName != null) {
                        PlayerEntity pairedPlayer = playerService.getPlayer(pairedPlayerName);
                        if (pairedPlayer != null && !pairedPlayer.isSold()) {
                                pairedPlayer.setSold(true);
                                pairedPlayer.setSoldPrice(finalSalePrice);
                                pairedPlayer.setFinalPrice(finalSalePrice);
                                pairedPlayer.setTeam(captainName);
                                playerService.savePlayer(pairedPlayer);
                        }
                        auctionEventService.logEvent(
                                        "AE_PARTNERSHIP_REVEALED",
                                        pairedPlayerName,
                                        captainName,
                                        finalSalePrice,
                                        "A + E partnership revealed after sale of " + playerName);
                }

                team.setPurse(
                                team.getPurse()
                                                - finalSalePrice
                                                - penalty);

                team.setPlayersBought(
                                team.getPlayersBought() + packageSize);

                team.setPlayersLeft(
                                team.getPlayersLeft() - packageSize);

                teamService.saveTeam(team);
                AuctionLogEntity log = new AuctionLogEntity();

                log.setPlayerName(playerName);
                log.setCaptainName(captainName);
                log.setSoldPrice(finalSalePrice);

                auctionLogService.addLog(log);
                player.setFinalPrice(finalSalePrice);
                playerService.savePlayer(player);
                auctionEventService.logEvent(
                                "PLAYER_SOLD",
                                playerName,
                                captainName,
                                finalSalePrice,
                                "Bid ₹" + soldPrice
                                                + (marketAdjustment == 0 ? ""
                                                                : " | Market adjustment "
                                                                                + formatAdjustment(marketAdjustment))
                                                + " | Recorded sale price ₹"
                                                + finalSalePrice
                                                + " | Spendable purse settled using effective player price");
                if (cursePlayerService != null) {
                        if (pairedPlayerName != null) {
                                cursePlayerService.settleSaleForPlayers(
                                                List.of(playerName, pairedPlayerName),
                                                captainName,
                                                finalSalePrice);
                        } else {
                                cursePlayerService.settleSale(playerName, captainName, finalSalePrice);
                        }
                }
                if (blindOpeningBidService != null) {
                        if (pairedPlayerName != null) {
                                blindOpeningBidService.awardBonusIfEligibleForSale(
                                                List.of(playerName, pairedPlayerName),
                                                captainName);
                        } else {
                                blindOpeningBidService.awardBonusIfEligible(playerName, captainName);
                        }
                }
                reconcileEffectivePurses();
                int netPurseImpact = purseBeforeSale - team.getPurse();
                boolean purseFlushTriggered = flushAuctionPursesIfExhausted();
                clearRandomEventForPlayer(playerName, config);
                rtmService.clearCurrentAuctionClaim();
                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));

                clearWheelSpinState(config);
                config.setAuctionPhase(
                                AuctionPhase.NO_AUCTION);

                auctionConfigService.save(config);
                auctionSocketService.broadcastRefresh();

                return playerName
                                + " sold to "
                                + captainName
                                + " for bid ₹"
                                + soldPrice
                                + " (market-adjusted sale price ₹"
                                + finalSalePrice
                                + "; final purse impact ₹"
                                + netPurseImpact
                                + ")"
                                + (purseFlushTriggered ? "; every captain's purse reset to ₹3000" : "");
        }

        private boolean flushAuctionPursesIfExhausted() {
                List<TeamEntity> teams = teamService.getAllTeams();
                if (teams.isEmpty() || teams.stream().anyMatch(team -> team.getPurse() > 0)) {
                        return false;
                }

                for (TeamEntity team : teams) {
                        team.setPurse(3000);
                        teamService.saveTeam(team);
                }
                auctionEventService.logEvent(
                                "PURSE_FLUSH",
                                null,
                                null,
                                3000,
                                "Every captain's auction purse was reset to ₹3000 after all purses reached zero or below.");
                return true;
        }

        private String formatAdjustment(int amount) {
                return amount >= 0 ? "+₹" + amount : "-₹" + Math.abs(amount);
        }

        private int getMarketAdjustment(AuctionConfigEntity config, String playerName) {
                return config.getMarketAdjustmentPlayer() != null
                                && config.getMarketAdjustmentPlayer().equalsIgnoreCase(playerName)
                                                ? config.getMarketAdjustment()
                                                : 0;
        }

        private synchronized String prepareSilentBidWinner(
                        String playerName,
                        String captainName,
                        int bidAmount) {

                PlayerEntity player = playerService.getPlayer(playerName);

                if (player == null || player.isSold()) {
                        return "Player is not available for silent bidding";
                }

                String affordabilityError = validateAffordability(playerName, captainName, bidAmount);
                if (affordabilityError != null) {
                        return affordabilityError;
                }

                if (pendingSilentAuctionPlayer != null && !pendingSilentAuctionPlayer.isBlank()
                                && pendingSilentAuctionPlayer.equalsIgnoreCase(playerName)) {
                        pendingSilentAuctionPlayer = null;
                        AuctionConfigEntity config = auctionConfigService.getConfig();
                        persistRuntimeState(config);
                        if (config != null) {
                                auctionConfigService.save(config);
                        }
                }

                currentAuctionService.setCurrentAuction(new Auction(
                                playerName,
                                player.getSeed(),
                                bidAmount,
                                captainName,
                                player.getBasePrice(),
                                "Silent Bid"));

                return "Silent bid winner prepared.";
        }

        @Transactional
        public synchronized String callSilentBidWinner(
                        String playerName,
                        String captainName,
                        int bidAmount) {

                String prepared = prepareSilentBidWinner(
                                playerName,
                                captainName,
                                bidAmount);

                if (!prepared.equals("Silent bid winner prepared.")) {
                        return prepared;
                }

                return callSold(captainName, bidAmount);
        }

        @Transactional
        public synchronized String finalizeSilentBidWinner() {

                Auction auction = currentAuctionService.getCurrentAuction();

                if (auction == null || auction.getCurrentPlayer().isBlank()) {
                        return "No active silent bid auction.";
                }

                if (rtmService.getCurrentRtm() != null) {
                        return "Resolve RTM before confirming the sale.";
                }

                return sellPlayer(
                                auction.getCurrentPlayer(),
                                auction.getLeader(),
                                auction.getCurrentBid());
        }

        @Transactional
        public String undoLastSale() {

                AuctionLogEntity lastLog = auctionLogService.getLastLog();

                if (lastLog == null) {
                        return "No sale to undo";
                }

                PlayerEntity player = playerService.getPlayer(
                                lastLog.getPlayerName());

                ClubbedPlayerPairEntity pair = clubbedPlayerPairService.getPairForPlayer(lastLog.getPlayerName())
                                .orElse(null);
                PlayerEntity pairedPlayer = null;
                if (pair != null) {
                        String pairedName = pair.getPlayerOne().equalsIgnoreCase(lastLog.getPlayerName())
                                        ? pair.getPlayerTwo()
                                        : pair.getPlayerOne();
                        pairedPlayer = playerService.getPlayer(pairedName);
                        if (pairedPlayer != null && pairedPlayer.isSold()
                                        && pairedPlayer.getTeam().equalsIgnoreCase(lastLog.getCaptainName())) {
                                pairedPlayer.setSold(false);
                                pairedPlayer.setSoldPrice(0);
                                pairedPlayer.setFinalPrice(0);
                                pairedPlayer.setTeam("");
                                playerService.savePlayer(pairedPlayer);
                        } else {
                                pairedPlayer = null;
                        }
                }

                TeamEntity team = teamService.getTeam(
                                lastLog.getCaptainName());

                if (player != null) {

                        player.setSold(false);
                        player.setSoldPrice(0);
                        player.setFinalPrice(0);
                        player.setTeam("");

                        playerService.savePlayer(player);
                }

                if (team != null) {
                        int packageSize = pairedPlayer == null ? 1 : 2;

                        team.setPurse(
                                        team.getPurse()
                                                        + lastLog.getSoldPrice());

                        team.setPlayersBought(
                                        Math.max(0, team.getPlayersBought() - packageSize));

                        team.setPlayersLeft(
                                        team.getPlayersLeft() + packageSize);

                        teamService.saveTeam(team);
                }

                if (cursePlayerService != null) {
                        if (pairedPlayer != null) {
                                cursePlayerService.undoSettlementForPlayers(
                                                List.of(lastLog.getPlayerName(), pairedPlayer.getName()));
                        } else {
                                cursePlayerService.undoSettlement(lastLog.getPlayerName());
                        }
                }
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.reverseBonusIfAwarded(lastLog.getPlayerName());
                        if (pairedPlayer != null) {
                                blindOpeningBidService.reverseBonusIfAwarded(pairedPlayer.getName());
                        }
                }

                reconcileEffectivePurses();
                auctionLogService.removeLastLog();
                auctionEventService.logEvent(
                                "SALE_UNDONE",
                                lastLog.getPlayerName(),
                                lastLog.getCaptainName(),
                                lastLog.getSoldPrice(),
                                "Undo last sale");

                auctionSocketService.broadcastRefresh();

                return "Last sale undone";
        }

        @Transactional
        public synchronized String cancelSale(String playerName) {
                PlayerEntity player = playerService.getPlayer(playerName);
                if (player == null) {
                        return "Player not found";
                }
                if (!player.isSold()) {
                        return "Player is not currently sold";
                }

                String captainName = player.getTeam();
                TeamEntity team = teamService.getTeam(captainName);
                if (team == null) {
                        return "Team not found";
                }

                ClubbedPlayerPairEntity pair = clubbedPlayerPairService.getPairForPlayer(playerName).orElse(null);
                PlayerEntity pairedPlayer = null;
                if (pair != null) {
                        String pairedName = pair.getPlayerOne().equalsIgnoreCase(playerName)
                                        ? pair.getPlayerTwo()
                                        : pair.getPlayerOne();
                        pairedPlayer = playerService.getPlayer(pairedName);
                        if (pairedPlayer != null && pairedPlayer.isSold()
                                        && captainName.equalsIgnoreCase(pairedPlayer.getTeam())) {
                                pairedPlayer.setSold(false);
                                pairedPlayer.setSoldPrice(0);
                                pairedPlayer.setFinalPrice(0);
                                pairedPlayer.setTeam("");
                                playerService.savePlayer(pairedPlayer);
                        } else {
                                pairedPlayer = null;
                        }
                }

                int packageSize = pairedPlayer == null ? 1 : 2;
                if (cursePlayerService != null) {
                        if (pairedPlayer != null) {
                                cursePlayerService.undoSettlementForPlayers(
                                                List.of(playerName, pairedPlayer.getName()));
                        } else {
                                cursePlayerService.undoSettlement(playerName);
                        }
                }
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.reverseBonusIfAwarded(playerName);
                        if (pairedPlayer != null) {
                                blindOpeningBidService.reverseBonusIfAwarded(pairedPlayer.getName());
                        }
                }
                team.setPurse(team.getPurse() + player.getSoldPrice());
                team.setPlayersBought(Math.max(0, team.getPlayersBought() - packageSize));
                team.setPlayersLeft(team.getPlayersLeft() + packageSize);
                teamService.saveTeam(team);

                player.setSold(false);
                player.setSoldPrice(0);
                player.setFinalPrice(0);
                player.setTeam("");
                playerService.savePlayer(player);
                auctionLogService.removeLogForPlayer(playerName);
                reconcileEffectivePurses();
                auctionEventService.logEvent(
                                "SALE_CANCELLED",
                                playerName,
                                captainName,
                                0,
                                "Sale cancelled by admin");
                auctionSocketService.broadcastRefresh();
                return "Sale cancelled for " + playerName;
        }

        @Transactional
        public synchronized String returnPlayerToPool(String playerName) {
                PlayerEntity player = playerService.getPlayer(playerName);
                if (player == null) {
                        return "Player not found";
                }
                if (player.isSold()) {
                        String result = cancelSale(playerName);
                        if (!result.equals("Sale cancelled for " + playerName)) {
                                return result;
                        }
                }
                String partner = clubbedPlayerPairService.getPairedPlayer(playerName);
                List<String> names = partner == null ? List.of(playerName) : List.of(playerName, partner);
                Auction auction = currentAuctionService.getCurrentAuction();
                if (auction != null && names.stream().anyMatch(name -> name.equalsIgnoreCase(auction.getCurrentPlayer()))) {
                        AuctionConfigEntity config = auctionConfigService.getConfig();
                        clearRandomEventForPlayer(auction.getCurrentPlayer(), config);
                        rtmService.cancelRtm();
                        if (silentBidService != null) {
                                silentBidService.clearRound();
                        }
                        currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0, ""));
                        clearWheelSpinState(config);
                        config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                        auctionConfigService.save(config);
                }
                for (String name : names) {
                        PlayerEntity returned = playerService.getPlayer(name);
                        if (returned != null && !returned.isSold()) {
                                returned.setDeferredToReAuction(false);
                                returned.setReAuctioned(false);
                                playerService.savePlayer(returned);
                                if (blindOpeningBidService != null) {
                                        blindOpeningBidService.clearRound(name);
                                }
                        }
                }
                auctionEventService.logEvent("PLAYER_RETURNED_TO_POOL", playerName, null, 0,
                                "Admin cancelled bidding and returned the player/package to the current auction pool.");
                auctionSocketService.broadcastRefresh();
                return playerName + " returned to the auction pool.";
        }

        @Transactional
        public String manualSale(
                        String playerName,
                        String newCaptain,
                        int newPrice,
                        String reason) {

                PlayerEntity player = playerService.getPlayer(playerName);

                if (player == null) {
                        return "Player not found";
                }

                String oldCaptain = player.getTeam();

                int oldPrice = player.getSoldPrice();

                TeamEntity oldTeam = teamService.getTeam(oldCaptain);

                TeamEntity newTeam = teamService.getTeam(newCaptain);

                if (newTeam == null) {
                        return "New team not found";
                }

                if (newPrice < 0) {
                        return "New price cannot be negative";
                }

                if (newTeam.getPlayersLeft() <= 0 && !newCaptain.equalsIgnoreCase(oldCaptain)) {
                        return "New team squad is already complete";
                }

                if (cursePlayerService != null) {
                        cursePlayerService.undoSettlement(playerName);
                }
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.reverseBonusIfAwarded(playerName);
                }

                if (oldTeam != null) {

                        oldTeam.setPurse(
                                        oldTeam.getPurse() + oldPrice);

                        oldTeam.setPlayersBought(
                                        oldTeam.getPlayersBought() - 1);

                        oldTeam.setPlayersLeft(
                                        oldTeam.getPlayersLeft() + 1);

                        teamService.saveTeam(oldTeam);
                }

                newTeam.setPurse(
                                newTeam.getPurse()
                                                - newPrice);

                newTeam.setPlayersBought(
                                newTeam.getPlayersBought() + 1);

                newTeam.setPlayersLeft(
                                newTeam.getPlayersLeft() - 1);

                teamService.saveTeam(newTeam);

                adminActionLogService.addLog(
                                "MANUAL_SALE",
                                playerName,
                                oldCaptain,
                                newCaptain,
                                oldPrice,
                                newPrice,
                                reason);
                auctionEventService.logEvent(
                                "MANUAL_SALE",
                                playerName,
                                newCaptain,
                                newPrice,
                                reason);

                player.setTeam(newCaptain);
                player.setSold(true);
                player.setSoldPrice(newPrice);
                player.setFinalPrice(
                                newPrice);

                playerService.savePlayer(player);

                if (cursePlayerService != null) {
                        cursePlayerService.settleSale(playerName, newCaptain, newPrice);
                }
                if (blindOpeningBidService != null) {
                        blindOpeningBidService.awardBonusIfEligible(playerName, newCaptain);
                }

                reconcileEffectivePurses();
                auctionSocketService.broadcastRefresh();

                return "Manual Sale Updated";
        }

        private void reconcileEffectivePurses() {
                if (effectivePurseService != null) {
                        effectivePurseService.reconcilePurses();
                }
        }

        private String validateAffordability(String playerName, String captainName, int bid) {
                return bidAffordabilityService == null ? null
                                : bidAffordabilityService.validate(playerName, captainName, bid, false);
        }

        public int getMaxAffordableBid(String captainName, boolean openingBid) {
                Auction auction = currentAuctionService.getCurrentAuction();
                if (auction == null || auction.getCurrentPlayer().isBlank()) {
                        return 0;
                }
                return bidAffordabilityService.maxBid(auction.getCurrentPlayer(), captainName, openingBid);
        }

        @Transactional
        public String resetAuction() {

                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setAuctionStarted(false);
                config.setAuctionRound(1);
                config.setCurseSelectionEnabled(false);
                config.setBlindBonanzaAwardsUsed(0);
                config.setRtmLockdownPlayer(null);
                config.setSquadSize(10);
                config.setTargetBonus(150);
                config.setTargetCompletionBonus(250);
                config.setTargetMissPenalty(100);
                config.setStealPenalty(200);
                config.setMarketAdjustmentPlayer(null);
                config.setMarketAdjustment(0);
                config.setAuctionPhase(
                                AuctionPhase.NO_AUCTION);

                pendingSilentAuctionPlayer = null;
                currentPlayerRandomEventPlayer = null;
                currentPlayerRandomEvent = null;
                marketCrashApplied = false;
                persistRuntimeState(config);
                auctionConfigService.save(config);

                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));

                playerService.getAllPlayers()
                                .forEach(player -> {

                                        player.setSold(false);
                                        player.setSoldPrice(0);
                                        player.setFinalPrice(0);
                                        player.setTeam("");
                                        player.setReAuctioned(false);
                                        player.setDeferredToReAuction(false);

                                        playerService.savePlayer(player);
                                });

                auctionLogService.clearLogs();

                adminActionLogService.clearLogs();

                auctionEventService.clearEvents();

                if (cursePlayerService != null) {
                        cursePlayerService.clearAll();
                }

                if (blindOpeningBidService != null) {
                        blindOpeningBidService.clearAll();
                }

                reconcileEffectivePurses();
                if (silentBidService != null) {
                        silentBidService.clearRound();
                }

                if (reAuctionSnapshotService != null) {
                        reAuctionSnapshotService.clear();
                }

                rtmService.clear();

                auctionSocketService.broadcastRefresh();

                return "Auction Reset Successfully";
        }

        public String endAuction() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (config.getAuctionRound() == 2 && cursePlayerService != null) {
                        cursePlayerService.expireForUnsoldPlayers();
                }
                config.setAuctionStarted(false);
                clearWheelSpinState(config);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);
                auctionSocketService.broadcastRefresh();
                return "Auction ended and secret targets settled.";
        }

        public String callSold() {
                return callSold(null, null);
        }

        public String callSold(
                        String captainName,
                        Integer soldPrice) {

                Auction auction = currentAuctionService.getCurrentAuction();
                AuctionConfigEntity config = auctionConfigService.getConfig();

                if (auction == null || auction.getCurrentPlayer() == null || auction.getCurrentPlayer().isBlank()) {
                        return "No active auction.";
                }

                if (config.getAuctionPhase() == AuctionPhase.SPINNING) {
                        return "Wait for the wheel spin to finish.";
                }

                if (pendingSilentAuctionPlayer != null && !pendingSilentAuctionPlayer.isBlank()
                                && pendingSilentAuctionPlayer.equalsIgnoreCase(auction.getCurrentPlayer())) {
                        return "Silent auction is active for " + auction.getCurrentPlayer()
                                        + ". Use the silent-bid flow instead of live bidding.";
                }

                if (captainName != null && !captainName.isBlank() && soldPrice != null && soldPrice > 0) {
                        TeamEntity team = teamService.getTeam(captainName);

                        if (team == null) {
                                return "Captain not found.";
                        }

                        if (team.getPlayersLeft() <= 0) {
                                return "Squad is already complete.";
                        }

                        if (soldPrice < auction.getBasePrice()) {
                                return "Sold price cannot be lower than base price (₹" + auction.getBasePrice() + ").";
                        }

                        String affordabilityError = validateAffordability(
                                        auction.getCurrentPlayer(), captainName, soldPrice);
                        if (affordabilityError != null) {
                                return affordabilityError;
                        }
                        auction.setLeader(captainName);
                        auction.setCurrentBid(soldPrice);
                        currentAuctionService.saveCurrentAuction();
                }

                if (auction.getLeader().isBlank()
                                || "None".equalsIgnoreCase(auction.getLeader())) {
                        return "No bids placed yet.";
                }

                String affordabilityError = validateAffordability(
                                auction.getCurrentPlayer(), auction.getLeader(), auction.getCurrentBid());
                if (affordabilityError != null) {
                        return affordabilityError;
                }
                RandomEventEntity marketCrash = triggerMarketCrashForCurrentPlayer(auction, config);

                config.setAuctionPhase(AuctionPhase.SOLD);

                auctionConfigService.save(config);

                auctionEventService.logEvent(
                                "SOLD",
                                auction.getCurrentPlayer(),
                                auction.getLeader(),
                                auction.getCurrentBid(),
                                "Waiting for RTM");
                if (marketCrash != null) {
                        logRandomEvent(marketCrash, auction.getCurrentPlayer());
                }
                auctionSocketService.broadcastRefresh();

                return marketCrash == null
                                ? "Waiting for RTM."
                                : "Market Crash triggered. Waiting for RTM.";
        }

        @Transactional
        public String acceptRtm(
                        String captainName) {

                RtmEntity claim = rtmService.getCurrentRtm();

                if (claim == null) {

                        return "No active RTM.";

                }

                if (!"BID_SUBMITTED".equals(
                                claim.getStatus())) {

                        return "RTM bid not submitted.";

                }

                if (!claim.getOriginalCaptain().equals(
                                captainName)) {

                        return "Only the original winning captain can accept.";

                }
                String player = claim.getPlayerName();
                String winner = claim.getOriginalCaptain();
                String rtmCaptain = claim.getCaptainName();
                int bid = claim.getBidAmount();

                Auction auction = currentAuctionService.getCurrentAuction();
                String affordabilityError = validateAffordability(player, winner, bid);
                if (affordabilityError != null) {
                        return affordabilityError;
                }
                auction.setCurrentBid(bid);
                currentAuctionService.saveCurrentAuction();

                String result = sellPlayer(
                                player,
                                winner,
                                bid);

                if (result.startsWith(player + " sold to ")) {
                        claim.setUsed(true);
                        rtmService.save(claim);

                        auctionEventService.logEvent(
                                        "RTM_ACCEPTED",
                                        player,
                                        winner,
                                        bid,
                                        "Matched RTM bid from " + rtmCaptain);
                        auctionSocketService.broadcastRefresh();
                }

                return result;

        }

        @Transactional
        public String declineRtm(
                        String captainName) {

                RtmEntity claim = rtmService.getCurrentRtm();

                if (claim == null) {

                        return "No active RTM.";

                }

                if (!"BID_SUBMITTED".equals(
                                claim.getStatus())) {

                        return "RTM bid not submitted.";

                }

                if (!claim.getOriginalCaptain().equals(
                                captainName)) {

                        return "Only the original winning captain can decline.";

                }
                Auction auction = currentAuctionService.getCurrentAuction();
                String affordabilityError = validateAffordability(
                                claim.getPlayerName(), claim.getCaptainName(), claim.getBidAmount());
                if (affordabilityError != null) {
                        return affordabilityError;
                }
                auction.setLeader(claim.getCaptainName());
                auction.setCurrentBid(claim.getBidAmount());
                currentAuctionService.saveCurrentAuction();

                String result = sellPlayer(
                                claim.getPlayerName(),
                                claim.getCaptainName(), // RTM captain ✅
                                claim.getBidAmount());

                if (result.startsWith(claim.getPlayerName() + " sold to ")) {
                        claim.setUsed(true);
                        rtmService.save(claim);

                        auctionEventService.logEvent(
                                        "RTM_DECLINED",
                                        claim.getPlayerName(),
                                        claim.getCaptainName(),
                                        claim.getBidAmount(),
                                        "Original captain declined to match");
                        auctionSocketService.broadcastRefresh();
                }

                return result;

        }

        public synchronized String updateCurrentAuction(
                        String captainName,
                        Integer currentBid) {

                Auction auction = currentAuctionService.getCurrentAuction();

                if (auction == null ||
                                auction.getCurrentPlayer() == null || auction.getCurrentPlayer().isBlank()) {

                        return "No active auction.";
                }

                if (pendingSilentAuctionPlayer != null && !pendingSilentAuctionPlayer.isBlank()
                                && pendingSilentAuctionPlayer.equalsIgnoreCase(auction.getCurrentPlayer())) {
                        return "Silent auction is active for " + auction.getCurrentPlayer()
                                        + ". Submit bids through the silent-bid flow instead.";
                }

                if (currentBid == null || currentBid <= 0) {
                        return "A positive bid is required.";
                }

                if (!teamService.isValidBidIncrement(currentBid)) {
                        return "Bid must be a multiple of 50 up to ₹1000, then a multiple of 100.";
                }

                TeamEntity team = teamService.getTeam(captainName);
                if (team == null) {
                        return "Captain not found.";
                }

                if (team.getPlayersLeft() <= 0) {
                        return "Squad is already complete.";
                }

                if ("None".equals(auction.getLeader())) {
                        if (currentBid < auction.getBasePrice()) {
                                return "Opening bid cannot be below the base price (₹"
                                                + auction.getBasePrice() + ").";
                        }
                } else {
                        int minimumIncrement = auction.getCurrentBid() <= 1000 ? 50 : 100;
                        if (currentBid < auction.getCurrentBid() + minimumIncrement) {
                                return "Minimum bid is ₹" + (auction.getCurrentBid() + minimumIncrement) + ".";
                        }
                }

                String affordabilityError = validateAffordability(
                                auction.getCurrentPlayer(), captainName, currentBid);
                if (affordabilityError != null) {
                        return affordabilityError;
                }
                auction.setLeader(captainName);
                auction.setCurrentBid(currentBid);
                currentAuctionService.saveCurrentAuction();

                auctionSocketService.broadcastRefresh();

                return "Updated";
        }

}
