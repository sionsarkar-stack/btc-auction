package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BlindOpeningBidServiceTest {

    @Mock
    private BlindOpeningBidRepository repository;

    @Mock
    private TeamRepository teamRepository;

    private TeamService teamService;

    @Mock
    private AuctionEventService auctionEventService;

    @Mock
    private AuctionSocketService auctionSocketService;
    @Mock
    private AuctionConfigService auctionConfigService;

    private CurrentAuctionService currentAuctionService;
    private BlindOpeningBidService service;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository);
        currentAuctionService = new CurrentAuctionService();
        currentAuctionService.setCurrentAuction(new Auction("Player X", "A", 0, "None", 250));
        service = new BlindOpeningBidService(repository, teamService, currentAuctionService, auctionSocketService,
                auctionEventService, auctionConfigService);
    }

    @Test
    void revealWinnerUsesHighestSecretBid() {
        BlindOpeningBidEntity first = new BlindOpeningBidEntity();
        first.setCaptainName("Sen");
        first.setPlayerName("Player X");
        first.setBidAmount(300);
        first.setSubmitted(true);

        BlindOpeningBidEntity second = new BlindOpeningBidEntity();
        second.setCaptainName("Gappu");
        second.setPlayerName("Player X");
        second.setBidAmount(450);
        second.setSubmitted(true);

        BlindOpeningBidEntity third = new BlindOpeningBidEntity();
        third.setCaptainName("Joy");
        third.setPlayerName("Player X");
        third.setBidAmount(400);
        third.setSubmitted(true);

        when(repository.findByPlayerName("Player X")).thenReturn(List.of(first, second, third));

        BlindOpeningBidEntity winner = service.revealWinner("Player X");

        assertNotNull(winner);
        assertEquals("Gappu", winner.getCaptainName());
        assertEquals(450, winner.getBidAmount());
        assertEquals("Gappu", currentAuctionService.getCurrentAuction().getLeader());
        assertEquals(450, currentAuctionService.getCurrentAuction().getCurrentBid());
    }

    @Test
    void tiedHighestBidsResetOnlyTheTiedCaptainsForAnotherRound() {
        BlindOpeningBidEntity first = new BlindOpeningBidEntity();
        first.setCaptainName("Sen");
        first.setPlayerName("Player X");
        first.setBidAmount(450);
        first.setSubmitted(true);

        BlindOpeningBidEntity second = new BlindOpeningBidEntity();
        second.setCaptainName("Gappu");
        second.setPlayerName("Player X");
        second.setBidAmount(450);
        second.setSubmitted(true);

        BlindOpeningBidEntity third = new BlindOpeningBidEntity();
        third.setCaptainName("Joy");
        third.setPlayerName("Player X");
        third.setBidAmount(400);
        third.setSubmitted(true);

        when(repository.findByPlayerName("Player X")).thenReturn(List.of(first, second, third));

        BlindOpeningBidEntity winner = service.revealWinner("Player X");

        assertNull(winner);
        assertFalse(first.isSubmitted());
        assertFalse(second.isSubmitted());
        assertTrue(third.isSubmitted());
        assertEquals(0, first.getBidAmount());
        assertEquals(0, second.getBidAmount());
        assertFalse(first.isBonusEligible());
        assertFalse(second.isBonusEligible());
        verify(auctionEventService).logEvent(
                eq("STARTING_BID_TIE"),
                eq("Player X"),
                eq("Sen, Gappu"),
                eq(450),
                eq("Tie declared at ₹450. Tied captains must submit again or pass."));
    }

    @Test
    void finalUniqueTieBreakBidderIsTheOnlyBonusEligibleCaptain() {
        BlindOpeningBidEntity senBid = submittedBid("Sen", 450);
        BlindOpeningBidEntity gappuBid = submittedBid("Gappu", 450);
        BlindOpeningBidEntity joyBid = submittedBid("Joy", 400);
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(senBid, gappuBid, joyBid));

        assertNull(service.revealWinner("Player X"));

        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        TeamEntity gappu = new TeamEntity();
        gappu.setCaptainName("Gappu");
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(sen));
        when(teamRepository.findByCaptainName("Gappu")).thenReturn(java.util.Optional.of(gappu));
        when(repository.findByPlayerNameAndCaptainName("Player X", "Sen")).thenReturn(java.util.Optional.of(senBid));
        when(repository.findByPlayerNameAndCaptainName("Player X", "Gappu"))
                .thenReturn(java.util.Optional.of(gappuBid));

        assertEquals("Blind opening bid submitted.", service.submitBid("Player X", "Sen", 500));
        assertEquals("Blind opening bid passed.", service.passBid("Player X", "Gappu"));
        BlindOpeningBidEntity winner = service.revealWinner("Player X");

        assertEquals("Sen", winner.getCaptainName());
        assertTrue(senBid.isBonusEligible());
        assertFalse(gappuBid.isBonusEligible());
        assertFalse(joyBid.isBonusEligible());
    }

    @Test
    void normalBlindBidBonusIsGrantedOnceOnlyWhenFinalBuyerIsEligible() {
        BlindOpeningBidEntity eligibleBid = submittedBid("Sen", 500);
        eligibleBid.setBonusEligible(true);
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setTournamentPoints(75);
        sen.setPurse(2_000);
        AuctionConfigEntity config = new AuctionConfigEntity();
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(eligibleBid));
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(sen));
        when(auctionConfigService.getConfig()).thenReturn(config);

        assertEquals(300, service.awardBonusIfEligible("Player X", "Sen"));
        assertEquals(0, service.awardBonusIfEligible("Player X", "Sen"));
        assertEquals(75, sen.getTournamentPoints());
        assertEquals(2_000, sen.getPurse());
        assertEquals(0, config.getBlindBonanzaAwardsUsed());
        verify(auctionEventService).logEvent(
                "BLIND_BID_BONUS",
                "Player X",
                "Sen",
                300,
                "Highest valid blind bidder won the player; effective price reduced by ₹300.");
    }

    @Test
    void pairedSaleAwardsOnlyOneBonusWhenBuyerIsEligibleForBothPlayers() {
        BlindOpeningBidEntity firstEligibleBid = submittedBid("Sen", 500);
        firstEligibleBid.setBonusEligible(true);
        firstEligibleBid.setPlayerName("Player A");
        BlindOpeningBidEntity secondEligibleBid = submittedBid("Sen", 400);
        secondEligibleBid.setBonusEligible(true);
        secondEligibleBid.setPlayerName("Player C");
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setTournamentPoints(100);
        AuctionConfigEntity config = new AuctionConfigEntity();
        when(repository.findByPlayerName("Player A")).thenReturn(List.of(firstEligibleBid));
        when(repository.findByPlayerName("Player C")).thenReturn(List.of(secondEligibleBid));
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(sen));
        when(auctionConfigService.getConfig()).thenReturn(config);

        assertEquals(300, service.awardBonusIfEligibleForSale(List.of("Player A", "Player C"), "Sen"));
        assertEquals(0, service.awardBonusIfEligibleForSale(List.of("Player A", "Player C"), "Sen"));
        assertEquals(100, sen.getTournamentPoints());
        assertTrue(firstEligibleBid.isBonusAwarded());
        assertFalse(secondEligibleBid.isBonusAwarded());
        verify(auctionEventService, org.mockito.Mockito.times(1)).logEvent(
                eq("BLIND_BID_BONUS"),
                any(String.class),
                eq("Sen"),
                eq(300),
                any(String.class));
    }

    @Test
    void blindBonanzaUpgradesTheNormalBonusAndCountsOnlySuccessfulAwards() {
        BlindOpeningBidEntity eligibleBid = submittedBid("Sen", 500);
        eligibleBid.setBonusEligible(true);
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setPendingRandomEventType(com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA);
        config.setPendingRandomEventPlayer("Player X");
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(eligibleBid));
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(sen));
        when(auctionConfigService.getConfig()).thenReturn(config);

        assertEquals(500, service.awardBonusIfEligible("Player X", "Sen"));
        assertEquals(0, sen.getTournamentPoints());
        assertEquals(1, config.getBlindBonanzaAwardsUsed());
        verify(auctionEventService).logEvent(
                "BLIND_BONANZA_REWARDED",
                "Player X",
                "Sen",
                500,
                "Blind Bonanza reduced the effective price by ₹500.");
    }

    @Test
    void blindBonanzaStopsAfterFourSuccessfulAwardsButNormalBonusContinues() {
        BlindOpeningBidEntity eligibleBid = submittedBid("Sen", 500);
        eligibleBid.setBonusEligible(true);
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setBlindBonanzaAwardsUsed(4);
        config.setPendingRandomEventType(com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA);
        config.setPendingRandomEventPlayer("Player X");
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(eligibleBid));
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(sen));
        when(auctionConfigService.getConfig()).thenReturn(config);

        assertEquals(300, service.awardBonusIfEligible("Player X", "Sen"));
        assertEquals(0, sen.getTournamentPoints());
        assertEquals(4, config.getBlindBonanzaAwardsUsed());
    }

    @Test
    void reversingAnUndoneSaleRestoresTheBonusSlot() {
        BlindOpeningBidEntity eligibleBid = submittedBid("Sen", 500);
        eligibleBid.setBonusEligible(true);
        eligibleBid.setBonusAwarded(true);
        eligibleBid.setBonusAwardedAmount(500);
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setTournamentPoints(500);
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setBlindBonanzaAwardsUsed(1);
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(eligibleBid));
        when(auctionConfigService.getConfig()).thenReturn(config);

        service.reverseBonusIfAwarded("Player X");

        assertEquals(500, sen.getTournamentPoints());
        assertEquals(0, config.getBlindBonanzaAwardsUsed());
        assertFalse(eligibleBid.isBonusAwarded());
    }

    @Test
    void noBonusIfAnotherCaptainIsTheFinalBuyer() {
        BlindOpeningBidEntity eligibleBid = submittedBid("Sen", 500);
        eligibleBid.setBonusEligible(true);
        when(auctionConfigService.getConfig()).thenReturn(new AuctionConfigEntity());
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(eligibleBid));

        assertEquals(0, service.awardBonusIfEligible("Player X", "Gappu"));
        verify(teamRepository, never()).findByCaptainName("Gappu");
    }

    @Test
    @SuppressWarnings("null")
    void startRoundCreatesEntriesForEveryTeam() {
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        TeamEntity gappu = new TeamEntity();
        gappu.setCaptainName("Gappu");
        TeamEntity anirban = new TeamEntity();
        anirban.setCaptainName("Anirban");
        TeamEntity joy = new TeamEntity();
        joy.setCaptainName("Joy");

        List<BlindOpeningBidEntity> saved = new ArrayList<>();
        when(teamRepository.findAll()).thenReturn(List.of(sen, gappu, anirban, joy));
        doNothing().when(repository).deleteByPlayerName("Player X");
        doAnswer(invocation -> {
            BlindOpeningBidEntity bid = invocation.getArgument(0);
            saved.add(bid);
            return bid;
        }).when(repository).save(any(BlindOpeningBidEntity.class));
        when(repository.findByPlayerName("Player X")).thenAnswer(invocation -> saved);

        service.startRound("Player X");

        assertEquals(4, saved.size());
        assertEquals(4, service.getAllBids("Player X").size());
    }

    private BlindOpeningBidEntity submittedBid(String captainName, int amount) {
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setPlayerName("Player X");
        bid.setCaptainName(captainName);
        bid.setBidAmount(amount);
        bid.setSubmitted(true);
        return bid;
    }

    @Test
    void blindOpeningBidsRejectAmountsAboveTheSituationalMaximum() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(500);
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setPlayerName("Player X");
        bid.setCaptainName("Sen");
        bid.setSubmitted(false);

        BidAffordabilityService affordability = org.mockito.Mockito.mock(BidAffordabilityService.class);
        service = new BlindOpeningBidService(repository, teamService, currentAuctionService, auctionSocketService,
                auctionEventService, auctionConfigService, affordability);
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(team));
        when(affordability.validate("Player X", "Sen", 5000, true)).thenReturn("Maximum affordable bid is ₹800.");

        assertEquals("Maximum affordable bid is ₹800.", service.submitBid("Player X", "Sen", 5000));
        assertFalse(bid.isSubmitted());
        verify(repository, org.mockito.Mockito.never()).save(bid);
    }

    @Test
    void passMarksCaptainSubmittedWithoutABid() {
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setPlayerName("Player X");
        bid.setCaptainName("Sen");
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(team));
        when(repository.findByPlayerNameAndCaptainName("Player X", "Sen"))
                .thenReturn(java.util.Optional.of(bid));

        assertEquals("Blind opening bid passed.", service.passBid("Player X", "Sen"));
        assertTrue(bid.isSubmitted());
        assertTrue(bid.isPassed());
        assertEquals(0, bid.getBidAmount());
    }

    @Test
    void allCaptainsPassedDoesNotProduceAWinner() {
        BlindOpeningBidEntity first = new BlindOpeningBidEntity();
        first.setPlayerName("Player X");
        first.setSubmitted(true);
        first.setPassed(true);
        first.setBidAmount(0);
        BlindOpeningBidEntity second = new BlindOpeningBidEntity();
        second.setPlayerName("Player X");
        second.setSubmitted(true);
        second.setPassed(true);
        second.setBidAmount(0);
        when(repository.findByPlayerName("Player X")).thenReturn(List.of(first, second));

        assertNull(service.revealWinner("Player X"));
        assertTrue(service.allCaptainsPassed("Player X"));
    }
}
