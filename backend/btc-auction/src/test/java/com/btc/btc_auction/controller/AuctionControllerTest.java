package com.btc.btc_auction.controller;

import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.ClubbedPlayerPairService;
import com.btc.btc_auction.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import com.btc.btc_auction.model.UpdateAuctionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private ClubbedPlayerPairService clubbedPlayerPairService;
    @Mock
    private PlayerService playerService;
    @Mock
    private HttpSession session;

    private AuctionController controller;

    @BeforeEach
    void setUp() {
        controller = new AuctionController(
                auctionService,
                auctionConfigService,
                clubbedPlayerPairService,
                playerService);
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
    void startsAuctionWithoutCurseSelection() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("admin");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("ADMIN");
        when(auctionService.startNominationAuction()).thenReturn("Auction started. Sen nominates first.");

        assertEquals("Auction started. Sen nominates first.",
                controller.startAuction(session));
        verify(auctionService).startNominationAuction();
    }

    @Test
    void rejectsAuctionControlWithoutAnAdminSession() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.spinWheel(session));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(auctionService, never()).spinWheel();
    }

    @Test
    void rejectsAuctionControlForCaptainSession() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.spinWheel(session));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(auctionService, never()).spinWheel();
    }

    @Test
    void liveBidUsesTheAuthenticatedCaptainInsteadOfTheRequestName() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");
        UpdateAuctionRequest request = new UpdateAuctionRequest();
        request.setCaptainName("Joy");
        request.setCurrentBid(500);
        when(auctionService.updateCurrentAuction("Sen", 500)).thenReturn("Bid updated.");

        assertEquals("Bid updated.", controller.updateCurrentAuction(request, session));

        verify(auctionService).updateCurrentAuction("Sen", 500);
    }

    @Test
    void customLiveBidUsesTheAuthenticatedCaptainInsteadOfTheRequestName() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");
        Map<String, Object> request = Map.of("captainName", "Joy", "bidAmount", 500);
        when(auctionService.placeCustomLiveBid("Sen", 500)).thenReturn("Bid increased to ₹500.");

        assertEquals("Bid increased to ₹500.", controller.placeCustomLiveBid(request, session));

        verify(auctionService).placeCustomLiveBid("Sen", 500);
    }

    @Test
    void auctionTimeoutUsesTheAuthenticatedCaptain() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");
        when(auctionService.callAuctionTimeout("Sen"))
                .thenReturn("Timeout called. The auction is paused for 45 seconds. 2 timeouts remaining.");

        assertEquals("Timeout called. The auction is paused for 45 seconds. 2 timeouts remaining.",
                controller.callAuctionTimeout(session));

        verify(auctionService).callAuctionTimeout("Sen");
    }

    @Test
    void blindOpeningBidUsesTheAuthenticatedCaptainInsteadOfTheRequestName() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");
        Map<String, Object> request = Map.of(
                "playerName", "Player X",
                "captainName", "Joy",
                "bidAmount", 500);
        when(auctionService.submitBlindOpeningBid("Player X", "Sen", 500)).thenReturn("Bid submitted.");

        assertEquals("Bid submitted.", controller.submitBlindOpeningBid(request, session));

        verify(auctionService).submitBlindOpeningBid("Player X", "Sen", 500);
    }

    @Test
    void statusProvidesServerTimeAndTheSortedEligibleWalkoutRoster() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("viewer");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("VIEWER");
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionPhase(AuctionPhase.SPINNING);
        config.setAuctionRound(1);
        PlayerEntity zulu = player("Zulu");
        PlayerEntity alpha = player("Alpha");
        PlayerEntity deferred = player("Deferred");
        deferred.setDeferredToReAuction(true);
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(playerService.getUnsoldPlayers()).thenReturn(List.of(zulu, deferred, alpha));

        AuctionConfigEntity status = controller.getStatus(session);

        verify(auctionService).completeWheelSpinWhenDue();
        assertNotNull(status.getServerTime());
        assertEquals(List.of("Alpha", "Zulu"), status.getWalkoutPlayerNames());
    }

    @Test
    void statusPollingCannotAdvanceAuctionWithoutAnAuthenticatedSession() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getStatus(session));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(auctionService, never()).completeWheelSpinWhenDue();
    }

    private PlayerEntity player(String name) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        return player;
    }
}