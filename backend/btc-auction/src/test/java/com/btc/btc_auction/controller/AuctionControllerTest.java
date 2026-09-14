package com.btc.btc_auction.controller;

import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.AuctionSocketService;
import com.btc.btc_auction.service.ClubbedPlayerPairService;
import com.btc.btc_auction.service.PlayerService;
import com.btc.btc_auction.service.ProtectionPlayerService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ProtectionPlayerService protectionPlayerService;
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
                protectionPlayerService);
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
}