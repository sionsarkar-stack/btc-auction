package com.btc.btc_auction.controller;

import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.CaptainPresenceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
}, allowCredentials = "true")
public class CaptainPresenceController {

    private final CaptainPresenceService captainPresenceService;

    public CaptainPresenceController(CaptainPresenceService captainPresenceService) {
        this.captainPresenceService = captainPresenceService;
    }

    @PostMapping("/api/captain-presence/heartbeat")
    public ResponseEntity<Void> heartbeat(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        Object role = session.getAttribute(SessionAttributes.ROLE);
        if (!(username instanceof String captainName) || captainName.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(role instanceof String userRole) || !"CAPTAIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        captainPresenceService.checkIn(session.getId(), captainName, userRole);
        return ResponseEntity.noContent().build();
    }
}