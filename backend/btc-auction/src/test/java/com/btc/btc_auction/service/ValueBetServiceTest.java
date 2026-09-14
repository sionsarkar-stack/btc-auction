package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.entity.ValueBetEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.ValueBetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValueBetServiceTest {

    @Mock
    private ValueBetRepository repository;
    @Mock
    private TeamService teamService;
    @Mock
    private PlayerService playerService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private AuctionEventService auctionEventService;

    private final CurrentAuctionService currentAuctionService = new CurrentAuctionService();
    private ValueBetService valueBetService;
    private AuctionConfigEntity config;

    @BeforeEach
    void setUp() {
        config = new AuctionConfigEntity();
        config.setAuctionPhase(AuctionPhase.OPENING_BID);
        config.setValueBetPlayer("Rohit");
        lenient().when(auctionConfigService.getConfig()).thenReturn(config);
        lenient().when(teamService.isValidBidIncrement(anyInt())).thenReturn(true);
        valueBetService = new ValueBetService(
                repository,
                teamService,
                playerService,
                auctionConfigService,
                currentAuctionService,
                auctionEventService);
    }

    @Test
    void acceptsPredictionBeforeBiddingAndRejectsDuplicate() {
        currentAuctionService.setCurrentAuction(new Auction("Rohit", "A", 300, "None", 300));
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        PlayerEntity player = new PlayerEntity();
        player.setName("Rohit");
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(playerService.getPlayer("Rohit")).thenReturn(player);
        when(repository.findByPlayerNameAndCaptainName("Rohit", "Sen"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ValueBetEntity()));

        assertEquals("Value Bet submitted.", valueBetService.submitPrediction("Rohit", "Sen", 500));
        assertEquals("Value Bet already submitted for this player.",
                valueBetService.submitPrediction("Rohit", "Sen", 600));
    }

    @Test
    void rewardsOnlyTheClosestPrediction() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(1000);
        TeamEntity fartherTeam = new TeamEntity();
        fartherTeam.setCaptainName("Joy");
        fartherTeam.setPurse(1000);
        ValueBetEntity qualifying = new ValueBetEntity();
        qualifying.setPlayerName("Rohit");
        qualifying.setCaptainName("Sen");
        qualifying.setPredictedPrice(500);
        ValueBetEntity fartherPrediction = new ValueBetEntity();
        fartherPrediction.setPlayerName("Rohit");
        fartherPrediction.setCaptainName("Joy");
        fartherPrediction.setPredictedPrice(700);
        when(repository.findByPlayerName("Rohit")).thenReturn(List.of(qualifying, fartherPrediction));
        when(teamService.getTeam("Sen")).thenReturn(team);
        lenient().when(teamService.getTeam("Joy")).thenReturn(fartherTeam);

        assertEquals(1, valueBetService.applyRewards("Rohit", 500));
        assertEquals(1200, team.getPurse());
        assertEquals(1000, fartherTeam.getPurse());
        assertEquals(0, valueBetService.applyRewards("Rohit", 500));
        verify(repository).save(qualifying);
    }

    @Test
    void rewardsEveryTiedClosestPredictionAndRevealsAllBetsAfterSale() {
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setPurse(1000);
        TeamEntity gappu = new TeamEntity();
        gappu.setCaptainName("Gappu");
        gappu.setPurse(1000);
        TeamEntity joy = new TeamEntity();
        joy.setCaptainName("Joy");
        joy.setPurse(1000);

        ValueBetEntity senBet = valueBet("Sen", 500);
        ValueBetEntity gappuBet = valueBet("Gappu", 500);
        ValueBetEntity joyBet = valueBet("Joy", 700);
        when(repository.findByPlayerName("Rohit")).thenReturn(List.of(senBet, gappuBet, joyBet));
        when(teamService.getTeam("Sen")).thenReturn(sen);
        when(teamService.getTeam("Gappu")).thenReturn(gappu);

        assertEquals(2, valueBetService.applyRewards("Rohit", 500));
        assertEquals(1200, sen.getPurse());
        assertEquals(1200, gappu.getPurse());
        assertEquals(1000, joy.getPurse());

        InOrder eventOrder = inOrder(auctionEventService);
        eventOrder.verify(auctionEventService).logEvent(
                eq("VALUE_BET_REVEALED"), eq("Rohit"), eq("Sen"), eq(500),
                eq("Value Bet revealed after sale. Final price ₹500."));
        eventOrder.verify(auctionEventService).logEvent(
                eq("VALUE_BET_REVEALED"), eq("Rohit"), eq("Gappu"), eq(500),
                eq("Value Bet revealed after sale. Final price ₹500."));
        eventOrder.verify(auctionEventService).logEvent(
                eq("VALUE_BET_REVEALED"), eq("Rohit"), eq("Joy"), eq(700),
                eq("Value Bet revealed after sale. Final price ₹500."));
        eventOrder.verify(auctionEventService).logEvent(
                eq("VALUE_BET_REWARD"), eq("Rohit"), eq("Sen, Gappu"), eq(200),
                eq("Closest Value Bet prediction(s) to final price ₹500; ₹200 awarded to each winner."));
    }

    @Test
    void acceptsPredictionDuringBlindOpeningBid() {
        config.setAuctionPhase(AuctionPhase.BLIND_OPENING_BID);
        currentAuctionService.setCurrentAuction(new Auction("Rohit", "A", 300, "None", 300));
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        PlayerEntity player = new PlayerEntity();
        player.setName("Rohit");
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(playerService.getPlayer("Rohit")).thenReturn(player);
        when(repository.findByPlayerNameAndCaptainName("Rohit", "Sen"))
                .thenReturn(Optional.empty());

        assertEquals("Value Bet submitted.", valueBetService.submitPrediction("Rohit", "Sen", 500));
    }

    @Test
    void rejectsPredictionAfterBiddingStarts() {
        config.setAuctionPhase(AuctionPhase.BIDDING);

        assertEquals("Value Bet event is not active for this player.",
                valueBetService.submitPrediction("Rohit", "Sen", 500));
    }

    private ValueBetEntity valueBet(String captainName, int predictedPrice) {
        ValueBetEntity bet = new ValueBetEntity();
        bet.setPlayerName("Rohit");
        bet.setCaptainName(captainName);
        bet.setPredictedPrice(predictedPrice);
        return bet;
    }
}