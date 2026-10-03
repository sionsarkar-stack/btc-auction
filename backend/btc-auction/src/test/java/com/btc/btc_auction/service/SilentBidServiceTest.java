package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.SilentBidEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.SilentBidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SilentBidServiceTest {

    @Mock
    private SilentBidRepository repository;
    @Mock
    private TeamService teamService;
    @Mock
    private AuctionService auctionService;
    @Mock
    private AuctionEventService auctionEventService;
    @Mock
    private AuctionSocketService auctionSocketService;
    @Mock
    private AuctionConfigService auctionConfigService;

    @Test
    void reAuctionAllowsSilentBidsAboveTheNormalMaximumBid() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionRound(2);
        TeamEntity team = new TeamEntity();
        SilentBidEntity bid = new SilentBidEntity();
        bid.setPlayerName("Player X");
        bid.setCaptainName("Sen");
        bid.setSubmitted(false);
        bid.setEligibleForTieBreak(true);
        SilentBidService service = new SilentBidService(
                repository,
                teamService,
                auctionService,
                auctionEventService,
                auctionSocketService,
                auctionConfigService);

        when(auctionConfigService.getConfig()).thenReturn(config);
        when(repository.findByPlayerNameAndCaptainName("Player X", "Sen")).thenReturn(Optional.of(bid));
        when(teamService.getTeam("Sen")).thenReturn(team);
        when(teamService.isValidBidIncrement(5000)).thenReturn(true);

        assertEquals("Bid submitted.", service.submitBid("Player X", "Sen", 5000));
        assertEquals(5000, bid.getBidAmount());
        verify(teamService, never()).getMaxBid(team);
    }

    @Test
    void allZeroSilentBidsAreReportedAsAllPassedWithoutWinner() {
        SilentBidEntity first = new SilentBidEntity();
        first.setPlayerName("Player X");
        first.setCaptainName("Sen");
        first.setBidAmount(0);
        first.setSubmitted(true);

        SilentBidEntity second = new SilentBidEntity();
        second.setPlayerName("Player X");
        second.setCaptainName("Gappu");
        second.setBidAmount(0);
        second.setSubmitted(true);

        SilentBidService service = new SilentBidService(
                repository,
                teamService,
                auctionService,
                auctionEventService,
                auctionSocketService,
                auctionConfigService);
        when(repository.findAll()).thenReturn(java.util.List.of(first, second));

        var result = service.getResult();

        assertTrue(result.isAllPassed());
        assertFalse(result.isTie());
        org.junit.jupiter.api.Assertions.assertNull(result.getWinner());
    }
}