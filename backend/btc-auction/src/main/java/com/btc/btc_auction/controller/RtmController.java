package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.RtmBidRequest;
import com.btc.btc_auction.security.SessionAuthorization;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.RtmService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rtm")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
}, allowCredentials = "true")
public class RtmController {

    private final RtmService rtmService;
    private final AuctionConfigService auctionConfigService;
    private final AuctionService auctionService;

    public RtmController(
            RtmService rtmService,
            AuctionConfigService auctionConfigService,
            AuctionService auctionService) {
        this.rtmService = rtmService;
        this.auctionConfigService = auctionConfigService;
        this.auctionService = auctionService;
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claimRtm(HttpSession session) {
        ResponseEntity<String> authorization = requireCaptain(session);
        if (authorization != null) {
            return authorization;
        }

        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (config.getAuctionPhase() != AuctionPhase.SOLD) {
            return ResponseEntity.ok("RTM is available only after SOLD.");
        }

        return ResponseEntity.ok(rtmService.claimRtm(SessionAuthorization.getUsername(session)));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrent(HttpSession session) {
        ResponseEntity<String> authorization = SessionAuthorization.requireAuthenticated(session);
        if (authorization != null) {
            return authorization;
        }
        return ResponseEntity.ok(rtmService.getCurrentRtm());
    }

    @PostMapping("/reset")
    public ResponseEntity<String> reset(HttpSession session) {
        ResponseEntity<String> authorization = requireAdmin(session);
        if (authorization != null) {
            return authorization;
        }
        rtmService.clear();
        return ResponseEntity.ok("RTM reset.");
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(HttpSession session) {
        ResponseEntity<String> authorization = requireAdmin(session);
        if (authorization != null) {
            return authorization;
        }
        return ResponseEntity.ok(rtmService.confirmRtm());
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(HttpSession session) {
        ResponseEntity<String> authorization = requireAdmin(session);
        if (authorization != null) {
            return authorization;
        }
        return ResponseEntity.ok(rtmService.cancelRtm());
    }

    @PostMapping("/bid")
    public ResponseEntity<String> submitBid(@RequestBody RtmBidRequest request, HttpSession session) {
        ResponseEntity<String> authorization = requireCaptain(session);
        if (authorization != null) {
            return authorization;
        }

        RtmEntity claim = rtmService.getCurrentRtm();
        if (claim != null && !SessionAuthorization.getUsername(session).equalsIgnoreCase(claim.getCaptainName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the RTM captain can submit this bid.");
        }
        return ResponseEntity.ok(rtmService.submitBid(request.getBidAmount()));
    }

    @PostMapping("/accept")
    public ResponseEntity<String> accept(HttpSession session) {
        ResponseEntity<String> authorization = requireCaptain(session);
        if (authorization != null) {
            return authorization;
        }
        return ResponseEntity.ok(auctionService.acceptRtm(SessionAuthorization.getUsername(session)));
    }

    @PostMapping("/decline")
    public ResponseEntity<String> decline(HttpSession session) {
        ResponseEntity<String> authorization = requireCaptain(session);
        if (authorization != null) {
            return authorization;
        }
        return ResponseEntity.ok(auctionService.declineRtm(SessionAuthorization.getUsername(session)));
    }

    private ResponseEntity<String> requireAdmin(HttpSession session) {
        return SessionAuthorization.requireRole(session, "ADMIN", "Admin access required.");
    }

    private ResponseEntity<String> requireCaptain(HttpSession session) {
        return SessionAuthorization.requireRole(session, "CAPTAIN", "Captain access required.");
    }
}