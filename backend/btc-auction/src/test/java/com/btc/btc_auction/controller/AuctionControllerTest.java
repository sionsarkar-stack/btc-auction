package com.btc.btc_auction.controller;

import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.AuctionSocketService;
import com.btc.btc_auction.service.ClubbedPlayerPairService;
import com.btc.btc_auction.service.CursePlayerService;
import com.btc.btc_auction.service.PlayerService;
import com.btc.btc_auction.service.TeamService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private AuctionSocketService auctionSocketService;
    @Mock
    private TeamService teamService;
    @Mock
    private ClubbedPlayerPairService clubbedPlayerPairService;
    @Mock
    private PlayerService playerService;
    @Mock
    private CursePlayerService cursePlayerService;
    @Mock
    private HttpSession session;

    private AuctionController controller;

    @BeforeEach
    void setUp() {
        controller = new AuctionController(
                auctionService,
                auctionConfigService,
                auctionSocketService,
                teamService,
                clubbedPlayerPairService,
                playerService,
                cursePlayerService);
    }

    @Test
    void activatesWildcardForTheCaptainStoredInTheSession() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");
        when(auctionService.useWildPick("Sen")).thenReturn("Sen activated Wildcard.");

        ResponseEntity<String> response = controller.useWildPick(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Sen activated Wildcard.", response.getBody());
        verify(auctionService).useWildPick("Sen");
    }

    @Test
    void rejectsWildcardWithoutAnAuthenticatedCaptainSession() {
        ResponseEntity<String> response = controller.useWildPick(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void rejectsWildcardForANonCaptainSession() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("viewer");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("VIEWER");

        ResponseEntity<String> response = controller.useWildPick(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void preventsAuctionStartUntilEveryCaptainHasTwoCursePicks() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        PlayerEntity player = new PlayerEntity();
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(playerService.getAllPlayers()).thenReturn(java.util.List.of(player));
        when(teamService.getAllTeams()).thenReturn(java.util.List.of(team));
        when(cursePlayerService.areAllCaptainsReady(java.util.List.of(team))).thenReturn(false);

        assertEquals("Cannot start auction. Every captain must select two curse players first.",
                controller.startAuction());
        verify(auctionSocketService, never()).broadcastRefresh();
    }

    @Test
    void statusProvidesServerTimeAndTheSortedEligibleWalkoutRoster() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionPhase(AuctionPhase.SPINNING);
        config.setAuctionRound(1);
        PlayerEntity zulu = player("Zulu");
        PlayerEntity alpha = player("Alpha");
        PlayerEntity deferred = player("Deferred");
        deferred.setDeferredToReAuction(true);
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(playerService.getUnsoldPlayers()).thenReturn(List.of(zulu, deferred, alpha));

        AuctionConfigEntity status = controller.getStatus();

        verify(auctionService).completeWheelSpinWhenDue();
        assertNotNull(status.getServerTime());
        assertEquals(List.of("Alpha", "Zulu"), status.getWalkoutPlayerNames());
    }

    private PlayerEntity player(String name) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        return player;
    }
}