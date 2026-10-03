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
                auctionEventService);
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
        verify(auctionEventService).logEvent(
                eq("STARTING_BID_TIE"),
                eq("Player X"),
                eq("Sen, Gappu"),
                eq(450),
                eq("Tie declared at ₹450. Tied captains must submit again or pass."));
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
        doNothing().when(repository).deleteAll();
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

    @Test
    void reAuctionAllowsBlindOpeningBidsAboveTheNormalMaximumBid() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionRound(2);
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(500);
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setPlayerName("Player X");
        bid.setCaptainName("Sen");
        bid.setSubmitted(false);

        service = new BlindOpeningBidService(repository, teamService, currentAuctionService, auctionSocketService,
                auctionEventService, auctionConfigService);
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(teamRepository.findByCaptainName("Sen")).thenReturn(java.util.Optional.of(team));
        when(repository.findByPlayerNameAndCaptainName("Player X", "Sen"))
                .thenReturn(java.util.Optional.of(bid));

        assertEquals("Blind opening bid submitted.", service.submitBid("Player X", "Sen", 5000));
        assertEquals(5000, bid.getBidAmount());
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
