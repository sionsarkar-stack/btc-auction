package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.RtmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RtmServiceTest {

    @Mock
    private RtmRepository repository;
    @Mock
    private AuctionEventService auctionEventService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private AuctionSocketService auctionSocketService;
    @Mock
    private TeamService teamService;

    private CurrentAuctionService currentAuctionService;
    private RtmService service;

    @BeforeEach
    void setUp() {
        currentAuctionService = new CurrentAuctionService();
        currentAuctionService.setCurrentAuction(new Auction("Player X", "A", 300, "Sen", 300));
        service = new RtmService(
                repository,
                currentAuctionService,
                auctionEventService,
                auctionConfigService,
                auctionSocketService,
                teamService);
    }

    @Test
    void rtmBidsRejectAmountsAboveTheSituationalMaximum() {
        BidAffordabilityService affordability = org.mockito.Mockito.mock(BidAffordabilityService.class);
        service = new RtmService(repository, currentAuctionService, auctionEventService, auctionConfigService,
                auctionSocketService, teamService, affordability);
        TeamEntity team = new TeamEntity();
        RtmEntity claim = new RtmEntity();
        claim.setCaptainName("Sen");
        claim.setOriginalBidAmount(300);

        when(repository.findByPlayerName("Player X")).thenReturn(Optional.of(claim));
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.isValidBidIncrement(5000)).thenReturn(true);
        when(affordability.validate("Player X", "Sen", 5000, false)).thenReturn("Maximum affordable bid is ₹150.");

        assertEquals("Maximum affordable bid is ₹150.", service.submitBid(5000));
        verify(repository, never()).save(claim);
    }

    @Test
    void noMercyPreventsRtmForTheSelectedPlayer() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionPhase(AuctionPhase.SOLD);
        config.setRtmLockdownPlayer("Player X");
        when(auctionConfigService.getConfig()).thenReturn(config);

        assertEquals("NO MERCY bans RTM and Wild Pick for this player.", service.claimRtm("Sen"));
        verify(repository, never()).findByPlayerName("Player X");
    }
}