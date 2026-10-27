package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.RtmBidRequest;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.RtmService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RtmControllerTest {

    @Mock
    private RtmService rtmService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private AuctionService auctionService;
    @Mock
    private HttpSession session;

    private RtmController controller;

    @BeforeEach
    void setUp() {
        controller = new RtmController(rtmService, auctionConfigService, auctionService);
    }

    @Test
    void rejectsRtmClaimWithoutAnAuthenticatedCaptain() {
        ResponseEntity<String> response = controller.claimRtm(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(auctionConfigService, never()).getConfig();
        verify(rtmService, never()).claimRtm("Sen");
    }

    @Test
    void claimsRtmForTheCaptainStoredInTheSession() {
        authenticateAs("Sen", "CAPTAIN");
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setAuctionPhase(AuctionPhase.SOLD);
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(rtmService.claimRtm("Sen")).thenReturn("RTM claimed.");

        ResponseEntity<String> response = controller.claimRtm(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RTM claimed.", response.getBody());
        verify(rtmService).claimRtm("Sen");
    }

    @Test
    void onlyTheClaimingCaptainCanSubmitTheRtmBid() {
        authenticateAs("Sen", "CAPTAIN");
        RtmEntity claim = new RtmEntity();
        claim.setCaptainName("Joy");
        when(rtmService.getCurrentRtm()).thenReturn(claim);
        RtmBidRequest request = new RtmBidRequest();
        request.setBidAmount(500);

        ResponseEntity<String> response = controller.submitBid(request, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(rtmService, never()).submitBid(500);
    }

    @Test
    void adminCanResetRtm() {
        authenticateAs("admin", "ADMIN");

        ResponseEntity<String> response = controller.reset(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rtmService).clear();
    }

    @Test
    void captainCannotResetRtm() {
        authenticateAs("Sen", "CAPTAIN");

        ResponseEntity<String> response = controller.reset(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(rtmService, never()).clear();
    }

    @Test
    void rtmAcceptanceUsesTheAuthenticatedCaptain() {
        authenticateAs("Sen", "CAPTAIN");
        when(auctionService.acceptRtm("Sen")).thenReturn("RTM accepted.");

        ResponseEntity<String> response = controller.accept(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RTM accepted.", response.getBody());
        verify(auctionService).acceptRtm("Sen");
    }

    private void authenticateAs(String username, String role) {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn(username);
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn(role);
    }
}