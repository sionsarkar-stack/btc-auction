package com.btc.btc_auction.controller;

import com.btc.btc_auction.dto.CurseSelectionDto;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.CursePlayerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/curse-players")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:8080" }, allowCredentials = "true")
public class CursePlayerController {

    private final CursePlayerService cursePlayerService;

    public CursePlayerController(CursePlayerService cursePlayerService) {
        this.cursePlayerService = cursePlayerService;
    }

    @PostMapping("/selection/enable")
    public ResponseEntity<String> enableSelection(HttpSession session) {
        if (!hasRole(session, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
        }
        return ResponseEntity.ok(cursePlayerService.enableSelection());
    }

    @PostMapping("/auto-select")
    public ResponseEntity<String> autoSelect(HttpSession session) {
        if (!hasRole(session, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
        }
        return ResponseEntity.ok(cursePlayerService.autoSelectForTesting());
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Integer>> getReadiness(HttpSession session) {
        if (!hasRole(session, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(cursePlayerService.getSelectionCounts());
    }

    @GetMapping("/admin-selections")
    public ResponseEntity<List<CurseSelectionDto>> getAdminSelections(HttpSession session) {
        if (!hasRole(session, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(cursePlayerService.getAllSelections());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CursePlayerEntity>> getMine(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        if (!(username instanceof String captainName) || !hasRole(session, "CAPTAIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(cursePlayerService.getByCaptain(captainName));
    }

    @PostMapping
    public ResponseEntity<String> save(@RequestBody Map<String, String> request, HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        if (!(username instanceof String captainName) || !hasRole(session, "CAPTAIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Captain access required.");
        }
        return ResponseEntity.ok(cursePlayerService.saveCurse(captainName, request.get("playerName")));
    }

    private boolean hasRole(HttpSession session, String expectedRole) {
        Object role = session.getAttribute(SessionAttributes.ROLE);
        return role instanceof String userRole && expectedRole.equalsIgnoreCase(userRole);
    }
}