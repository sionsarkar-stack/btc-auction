package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.RandomEventEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.enums.RandomEventType;
import com.btc.btc_auction.model.Auction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceWheelSelectionTest {

    @Mock
    private TeamService teamService;
    @Mock
    private PlayerService playerService;
    @Mock
    private AuctionLogService auctionLogService;
    @Mock
    private AdminActionLogService adminActionLogService;
    @Mock
    private AuctionEventService auctionEventService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private RtmService rtmService;
    @Mock
    private AuctionSocketService auctionSocketService;
    private CurrentAuctionService currentAuctionService;
    @Mock
    private ClubbedPlayerPairService clubbedPlayerPairService;
    @Mock
    private RandomEventService randomEventService;
    @Mock
    private BlindOpeningBidService blindOpeningBidService;
    @Mock
    private ProtectionPlayerService protectionPlayerService;
    @Mock
    private ValueBetService valueBetService;
    @Mock
    private SilentBidService silentBidService;
    private AuctionConfigEntity config;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        config = new AuctionConfigEntity();
        config.setAuctionStarted(true);
        when(auctionConfigService.getConfig()).thenReturn(config);

        PlayerEntity first = new PlayerEntity();
        first.setName("Rohit Sharma");
        first.setSeed("A");
        first.setBasePrice(300);
        first.setSold(false);

        PlayerEntity second = new PlayerEntity();
        second.setName("Virat Kohli");
        second.setSeed("B");
        second.setBasePrice(250);
        second.setSold(false);

        lenient().when(playerService.getUnsoldPlayers()).thenReturn(List.of(first, second));
        lenient().when(playerService.getPlayer("Rohit Sharma")).thenReturn(first);
        lenient().when(playerService.getPlayer("Virat Kohli")).thenReturn(second);

        currentAuctionService = new CurrentAuctionService();

        auctionService = new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService);
    }

    @Test
    void spinWheelSelectsAnAvailablePlayer() {
        String result = auctionService.spinWheel();

        assertEquals("Wheel spinning. Nominee will be revealed shortly.", result);
        Auction auction = currentAuctionService.getCurrentAuction();
        assertNotNull(auction);
        assertFalse(auction.getCurrentPlayer().isBlank());
        assertEquals(auction.getBasePrice(), auction.getCurrentBid());
        assertEquals(AuctionPhase.SPINNING, config.getAuctionPhase());
        assertNotNull(config.getWheelSpinStartedAt());
        assertNotNull(config.getWheelSpinEndsAt());
        assertTrue(config.getWheelSpinEndsAt() > config.getWheelSpinStartedAt());
    }

    @Test
    void spinWheelDoesNotReplaceAnActivePlayer() {
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 300, "Sen", 300));

        assertEquals("Complete or return the current player before spinning the wheel again.",
                auctionService.spinWheel());
        assertEquals("Rohit Sharma", currentAuctionService.getCurrentAuction().getCurrentPlayer());
    }

    @Test
    void assignsOnlyMarketBoomBeforeTheWheelSelectsThePlayer() {
        RandomEventEntity marketBoom = randomEvent(RandomEventType.MARKET_BOOM);
        when(randomEventService.getActiveEvents()).thenReturn(List.of(marketBoom));
        auctionService = auctionServiceWithRandomEvents(new SequenceDoubleSupplier(0.0, 0.0, 0.0));
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPlayersLeft(5);
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.getMaxBid(team)).thenReturn(1000);

        auctionService.spinWheel();
        completeCurrentWheelSpin();

        assertEquals("Rohit Sharma", currentAuctionService.getCurrentAuction().getCurrentPlayer());
        assertEquals("Rohit Sharma", config.getMarketAdjustmentPlayer());
        assertEquals(100, config.getMarketAdjustment());
        assertEquals("Waiting for RTM.", auctionService.callSold("Sen", 300));
        assertEquals(100, config.getMarketAdjustment());
    }

    @Test
    void keepsTheNoEventBranchAtTwentyFivePercent() {
        auctionService = auctionServiceWithRandomEvents(new SequenceDoubleSupplier(0.75, 0.0));

        auctionService.spinWheel();

        verifyNoInteractions(randomEventService);
        assertNull(config.getMarketAdjustmentPlayer());
        assertNull(config.getValueBetPlayer());
        assertNull(config.getRtmLockdownPlayer());
    }

    @Test
    void keepsValueBetActivationPrivateUntilThePlayerIsSold() {
        RandomEventEntity valueBet = randomEvent(RandomEventType.VALUE_BET);
        when(randomEventService.getActiveEvents()).thenReturn(List.of(valueBet));
        auctionService = auctionServiceWithRandomEvents(new SequenceDoubleSupplier(0.0, 0.0, 0.0));

        auctionService.spinWheel();
        completeCurrentWheelSpin();

        assertEquals("Rohit Sharma", config.getValueBetPlayer());
        assertEquals(1, config.getValueBetEventsUsed());
        verifyNoInteractions(auctionEventService);
    }

    @Test
    void doesNotScheduleValueBetAfterThreeUses() {
        config.setValueBetEventsUsed(3);
        RandomEventEntity valueBet = randomEvent(RandomEventType.VALUE_BET);
        when(randomEventService.getActiveEvents()).thenReturn(List.of(valueBet));
        auctionService = auctionServiceWithRandomEvents(new SequenceDoubleSupplier(0.0, 0.0, 0.0));

        auctionService.spinWheel();
        completeCurrentWheelSpin();

        assertNull(config.getValueBetPlayer());
        assertEquals(3, config.getValueBetEventsUsed());
        verifyNoInteractions(auctionEventService);
    }

    @Test
    void defersMarketCrashUntilCallSoldForTheAssignedPlayer() {
        RandomEventEntity marketCrash = randomEvent(RandomEventType.MARKET_CRASH);
        when(randomEventService.getActiveEvents()).thenReturn(List.of(marketCrash));
        auctionService = auctionServiceWithRandomEvents(new SequenceDoubleSupplier(0.0, 0.0, 0.0));
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPlayersLeft(5);
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.getMaxBid(team)).thenReturn(1000);

        auctionService.spinWheel();
        completeCurrentWheelSpin();

        assertNull(config.getMarketAdjustmentPlayer());
        assertEquals("Market Crash triggered. Waiting for RTM.", auctionService.callSold("Sen", 300));
        assertEquals("Rohit Sharma", config.getMarketAdjustmentPlayer());
        assertEquals(-100, config.getMarketAdjustment());
    }

    @Test
    void settlesValueBetsAfterLoggingThePlayerSale() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(1000);
        team.setPlayersLeft(5);
        PlayerEntity player = new PlayerEntity();
        player.setName("Rohit Sharma");
        player.setSeed("A");
        player.setBasePrice(300);
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 500, "Sen", 300));
        config.setAuctionPhase(AuctionPhase.SOLD);
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.getMaxBid(team)).thenReturn(1000);
        when(playerService.getPlayer("Rohit Sharma")).thenReturn(player);
        auctionService = new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService,
                null,
                null,
                null,
                valueBetService,
                null,
                () -> 0.75);

        auctionService.sellPlayer("Rohit Sharma", "Sen", 500);

        InOrder eventOrder = inOrder(auctionEventService, valueBetService);
        eventOrder.verify(auctionEventService).logEvent(
                eq("PLAYER_SOLD"),
                eq("Rohit Sharma"),
                eq("Sen"),
                eq(500),
                anyString());
        eventOrder.verify(valueBetService).applyRewards("Rohit Sharma", 500);
    }

    @Test
    void restoresAPendingMarketCrashAfterRestart() {
        config.setPendingRandomEventType(RandomEventType.MARKET_CRASH);
        config.setPendingRandomEventPlayer("Rohit Sharma");
        config.setPendingRandomEventTitle("market crash");
        config.setPendingRandomEventDescription("Final price decreases by 100 points.");
        config.setPendingRandomEventAmount(100);
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 300, "None", 300));

        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPlayersLeft(5);
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.getMaxBid(team)).thenReturn(1000);

        auctionService.restorePersistedRuntimeState();

        assertEquals("Market Crash triggered. Waiting for RTM.", auctionService.callSold("Sen", 300));
        assertEquals(-100, config.getMarketAdjustment());
        assertTrue(config.isMarketCrashApplied());
    }

    @Test
    void silentAuctionLockBlocksLiveBidCalls() {
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 300, "None", 300));
        auctionService.activateSilentAuctionLock("Rohit Sharma");

        String result = auctionService.callSold("Sen", 400);

        assertEquals("Silent auction is active for Rohit Sharma. Use the silent-bid flow instead of live bidding.",
                result);
    }

    @Test
    void silentAuctionStartsSecretBiddingInsteadOfBlindOpeningBidding() {
        RandomEventEntity silentAuction = randomEvent(RandomEventType.SILENT_AUCTION);
        when(randomEventService.getActiveEvents()).thenReturn(List.of(silentAuction));
        auctionService = new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService,
                randomEventService,
                blindOpeningBidService,
                null,
                null,
                silentBidService,
                new SequenceDoubleSupplier(0.0, 0.0, 0.0));

        auctionService.spinWheel();

        assertEquals(AuctionPhase.SPINNING, config.getAuctionPhase());
        verify(silentBidService, never()).startRound(anyString());
        completeCurrentWheelSpin();

        assertEquals(AuctionPhase.SILENT_BID, config.getAuctionPhase());
        verify(silentBidService).startRound("Rohit Sharma");
        verify(blindOpeningBidService, never()).startRound(anyString());
    }

    @Test
    void spinWheelStartsBlindBiddingOnlyAfterTheSharedRevealTime() {
        auctionService = auctionServiceWithBlindOpeningBidService();

        auctionService.spinWheel();

        assertEquals(AuctionPhase.SPINNING, config.getAuctionPhase());
        verify(blindOpeningBidService, never()).startRound(anyString());

        completeCurrentWheelSpin();

        assertEquals(AuctionPhase.BLIND_OPENING_BID, config.getAuctionPhase());
        assertNull(config.getWheelSpinStartedAt());
        assertNull(config.getWheelSpinEndsAt());
        verify(blindOpeningBidService).startRound(
                currentAuctionService.getCurrentAuction().getCurrentPlayer());
    }

    @Test
    void resetAuctionClearsProtectionAssignmentsAndAnyActiveSilentBidRound() {
        auctionService = new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService,
                null,
                null,
                protectionPlayerService,
                null,
                silentBidService,
                () -> 0.75);
        when(playerService.getAllPlayers()).thenReturn(List.of());

        auctionService.resetAuction();

        verify(silentBidService).clearRound();
        verify(protectionPlayerService).clearAll();
        assertFalse(config.isProtectionSelectionEnabled());
    }

    @Test
    void wildPickReturnsSelectedPlayerToWheelAndCanBeUsedOnlyOnce() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        when(teamService.getTeam("Sen")).thenReturn(team);

        auctionService.spinWheel();
        completeCurrentWheelSpin();
        String result = auctionService.useWildPick("Sen");

        assertTrue(result.contains("returned to the wheel"));
        assertTrue(team.isWildPickUsed());
        assertTrue(currentAuctionService.getCurrentAuction().getCurrentPlayer().isBlank());

        auctionService.spinWheel();
        completeCurrentWheelSpin();
        String secondResult = auctionService.useWildPick("Sen");
        assertEquals("Wild Pick has already been used.", secondResult);
    }

    @Test
    void wildPickCancelsAnUnsubmittedBlindOpeningRoundAndBroadcastsEvent() {
        auctionService = auctionServiceWithBlindOpeningBidService();
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        BlindOpeningBidEntity firstBid = blindOpeningBid(true);
        BlindOpeningBidEntity secondBid = blindOpeningBid(true);
        BlindOpeningBidEntity thirdBid = blindOpeningBid(true);
        BlindOpeningBidEntity fourthBid = blindOpeningBid(true);
        BlindOpeningBidEntity finalBid = blindOpeningBid(false);
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 300, "None", 300));
        config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(blindOpeningBidService.getAllBids("Rohit Sharma"))
                .thenReturn(List.of(firstBid, secondBid, thirdBid, fourthBid, finalBid));

        assertTrue(auctionService.useWildPick("Sen").contains("activated Wildcard"));
        assertTrue(team.isWildPickUsed());
        assertTrue(currentAuctionService.getCurrentAuction().getCurrentPlayer().isBlank());
        verify(blindOpeningBidService).clearRound("Rohit Sharma");
        verify(auctionEventService).logEvent(
                eq("WILDCARD_TRIGGERED"),
                eq("Rohit Sharma"),
                eq("Sen"),
                eq(0),
                anyString());
    }

    @Test
    void wildPickIsRejectedAfterAllBlindOpeningBidsArePlaced() {
        auctionService = auctionServiceWithBlindOpeningBidService();
        BlindOpeningBidEntity firstBid = blindOpeningBid(true);
        BlindOpeningBidEntity secondBid = blindOpeningBid(true);
        BlindOpeningBidEntity thirdBid = blindOpeningBid(true);
        BlindOpeningBidEntity fourthBid = blindOpeningBid(true);
        BlindOpeningBidEntity finalBid = blindOpeningBid(true);
        currentAuctionService.setCurrentAuction(new Auction("Rohit Sharma", "A", 300, "None", 300));
        config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
        when(blindOpeningBidService.getAllBids("Rohit Sharma"))
                .thenReturn(List.of(firstBid, secondBid, thirdBid, fourthBid, finalBid));

        assertEquals("Wildcard must be used before all Blind Opening Bids are placed.",
                auctionService.useWildPick("Sen"));
        verify(blindOpeningBidService, never()).clearRound("Rohit Sharma");
    }

    @Test
    void completesBlindOpeningBidAfterAllFiveCaptainsSubmit() {
        auctionService = auctionServiceWithBlindOpeningBidService();
        BlindOpeningBidEntity firstBid = blindOpeningBid(true);
        BlindOpeningBidEntity secondBid = blindOpeningBid(true);
        BlindOpeningBidEntity thirdBid = blindOpeningBid(true);
        BlindOpeningBidEntity fourthBid = blindOpeningBid(true);
        BlindOpeningBidEntity finalBid = blindOpeningBid(true);
        BlindOpeningBidEntity winner = blindOpeningBid(true);
        winner.setCaptainName("Sen");
        winner.setBidAmount(500);
        config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
        when(blindOpeningBidService.submitBid("Rohit Sharma", "Sen", 500))
                .thenReturn("Blind opening bid submitted.");
        when(blindOpeningBidService.getAllBids("Rohit Sharma"))
                .thenReturn(List.of(firstBid, secondBid, thirdBid, fourthBid, finalBid));
        when(blindOpeningBidService.revealWinner("Rohit Sharma")).thenReturn(winner);

        assertEquals("Blind opening bid submitted.",
                auctionService.submitBlindOpeningBid("Rohit Sharma", "Sen", 500));
        assertEquals(AuctionPhase.BIDDING, config.getAuctionPhase());
        verify(blindOpeningBidService).revealWinner("Rohit Sharma");
    }

    @Test
    void reAuctionUsesHalfBasePriceAndEndsAfterEachUnsoldPlayerIsSelectedOnce() {
        config.setAuctionStarted(false);
        PlayerEntity reAuctionPlayer = new PlayerEntity();
        reAuctionPlayer.setName("Re-auction Player");
        reAuctionPlayer.setSeed("A");
        reAuctionPlayer.setBasePrice(300);
        reAuctionPlayer.setSold(false);
        when(playerService.getUnsoldPlayers()).thenReturn(List.of(reAuctionPlayer));

        assertEquals(
                "Re-auction started. Unsold players will return at 50% of their original base price.",
                auctionService.startReAuction());
        auctionService.spinWheel();

        Auction auction = currentAuctionService.getCurrentAuction();
        assertEquals(150, auction.getBasePrice());
        assertTrue(reAuctionPlayer.isReAuctioned());
        currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0));

        assertEquals(
                "Re-auction completed. Remaining unsold players are displayed for admin manual sale.",
                auctionService.spinWheel());
        assertEquals(3, config.getAuctionRound());
        assertFalse(config.isAuctionStarted());
    }

    private AuctionService auctionServiceWithRandomEvents(DoubleSupplier randomValueSupplier) {
        return new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService,
                randomEventService,
                null,
                null,
                null,
                null,
                randomValueSupplier);
    }

    private AuctionService auctionServiceWithBlindOpeningBidService() {
        return new AuctionService(
                teamService,
                playerService,
                auctionLogService,
                adminActionLogService,
                auctionEventService,
                auctionConfigService,
                rtmService,
                auctionSocketService,
                currentAuctionService,
                clubbedPlayerPairService,
                null,
                blindOpeningBidService,
                null,
                null,
                null,
                () -> 0.75);
    }

    private RandomEventEntity randomEvent(RandomEventType type) {
        RandomEventEntity event = new RandomEventEntity();
        event.setType(type);
        event.setAmount(100);
        return event;
    }

    private BlindOpeningBidEntity blindOpeningBid(boolean submitted) {
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setSubmitted(submitted);
        return bid;
    }

    private void completeCurrentWheelSpin() {
        config.setWheelSpinEndsAt(System.currentTimeMillis() - 1);
        auctionService.completeWheelSpinWhenDue();
    }

    private static final class SequenceDoubleSupplier implements DoubleSupplier {
        private final double[] values;
        private int index;

        private SequenceDoubleSupplier(double... values) {
            this.values = values;
        }

        @Override
        public double getAsDouble() {
            double value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }

}
