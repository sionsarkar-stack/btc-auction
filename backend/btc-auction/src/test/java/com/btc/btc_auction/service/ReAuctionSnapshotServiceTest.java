package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.ReAuctionSnapshotEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.AuctionLogRepository;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import com.btc.btc_auction.repository.PlayerRepository;
import com.btc.btc_auction.repository.ReAuctionSnapshotRepository;
import com.btc.btc_auction.repository.RtmRepository;
import com.btc.btc_auction.repository.TeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReAuctionSnapshotServiceTest {

    @Mock
    private ReAuctionSnapshotRepository snapshotRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private AuctionLogRepository auctionLogRepository;
    @Mock
    private BlindOpeningBidRepository blindOpeningBidRepository;
    @Mock
    private CursePlayerRepository cursePlayerRepository;
    @Mock
    private RtmRepository rtmRepository;

    @Test
    void restoreRevertsTournamentPointsAndTriggeredCurseState() throws Exception {
        ReAuctionSnapshotService.ReAuctionState state = new ReAuctionSnapshotService.ReAuctionState(
                List.of(new ReAuctionSnapshotService.TeamState("Sen", 3_000, 475, 4, 5, false)),
                List.of(),
                List.of(),
                List.of(new ReAuctionSnapshotService.CurseState("Sen", "Target", true, "Buyer", 250, 250)),
                List.of(new ReAuctionSnapshotService.BlindOpeningBidState("Target", "Sen", 400, true, false,
                        true, true, 500)),
                List.of(),
                2);
        ReAuctionSnapshotEntity snapshot = new ReAuctionSnapshotEntity();
        snapshot.setId(1L);
        snapshot.setStateJson(new ObjectMapper().writeValueAsString(state));
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(1_000);
        team.setTournamentPoints(225);
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        when(teamRepository.findByCaptainName("Sen")).thenReturn(Optional.of(team));

        ReAuctionSnapshotService service = new ReAuctionSnapshotService(
                snapshotRepository,
                teamRepository,
                playerRepository,
                auctionLogRepository,
                blindOpeningBidRepository,
                cursePlayerRepository,
                rtmRepository,
                new ObjectMapper());

        assertEquals(2, service.restore());

        assertEquals(3_000, team.getPurse());
        assertEquals(475, team.getTournamentPoints());
        verify(cursePlayerRepository).deleteAll();
        verify(cursePlayerRepository).saveAll(argThat(containsTriggeredCurseFor("Sen", "Target", "Buyer", 250)));
        verify(blindOpeningBidRepository).saveAll(argThat(containsAwardedBidFor("Target", "Sen", 400)));
    }

    private ArgumentMatcher<Iterable<CursePlayerEntity>> containsTriggeredCurseFor(
            String setter,
            String player,
            String buyer,
            int amount) {
        return curses -> {
            if (curses == null)
                return false;
            for (CursePlayerEntity curse : curses) {
                if (setter.equals(curse.getCaptainName())
                        && player.equals(curse.getPlayerName())
                        && buyer.equals(curse.getSettledBuyer())
                        && amount == curse.getSettledAmount()
                        && curse.isTriggered()) {
                    return true;
                }
            }
            return false;
        };
    }

    private ArgumentMatcher<Iterable<BlindOpeningBidEntity>> containsAwardedBidFor(
            String player,
            String captain,
            int amount) {
        return bids -> {
            if (bids == null)
                return false;
            for (BlindOpeningBidEntity bid : bids) {
                if (player.equals(bid.getPlayerName())
                        && captain.equals(bid.getCaptainName())
                        && amount == bid.getBidAmount()
                        && bid.isBonusEligible()
                        && bid.isBonusAwarded()) {
                    return true;
                }
            }
            return false;
        };
    }
}