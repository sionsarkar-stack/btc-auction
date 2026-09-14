package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ProtectionPlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.ProtectionPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtectionPlayerServiceTest {

    @Mock
    private ProtectionPlayerRepository repository;

    @Mock
    private TeamService teamService;

    @Mock
    private PlayerService playerService;

    @Mock
    private AuctionEventService auctionEventService;

    private ProtectionPlayerService service;

    @BeforeEach
    void setUp() {
        service = new ProtectionPlayerService(repository, teamService, playerService, auctionEventService);
    }

    @Test
    void buyerGetsBonusWhenTheyBuyTheirOwnProtectedPlayer() {
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setPurse(1000);

        ProtectionPlayerEntity protection = new ProtectionPlayerEntity();
        protection.setCaptainName("Sen");
        protection.setPlayerName("Rohit");

        when(repository.findAll()).thenReturn(List.of(protection));
        when(teamService.getTeam("Sen")).thenReturn(sen);

        int result = service.applyPurchaseReward("Rohit", "Sen");

        assertEquals(300, result);
        assertEquals(1300, sen.getPurse());
    }

    @Test
    void otherCaptainGetsBonusAndProtectorPaysPenalty() {
        TeamEntity sen = new TeamEntity();
        sen.setCaptainName("Sen");
        sen.setPurse(1000);

        TeamEntity gappu = new TeamEntity();
        gappu.setCaptainName("Gappu");
        gappu.setPurse(1200);

        ProtectionPlayerEntity protection = new ProtectionPlayerEntity();
        protection.setCaptainName("Sen");
        protection.setPlayerName("Rohit");

        when(repository.findAll()).thenReturn(List.of(protection));
        when(teamService.getTeam("Sen")).thenReturn(sen);
        when(teamService.getTeam("Gappu")).thenReturn(gappu);

        int result = service.applyPurchaseReward("Rohit", "Gappu");

        assertEquals(300, result);
        assertEquals(800, sen.getPurse());
        assertEquals(1500, gappu.getPurse());
    }
}
