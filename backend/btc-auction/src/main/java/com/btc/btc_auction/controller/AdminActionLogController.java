package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AdminActionLogEntity;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.AdminActionLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

}, allowCredentials = "true")
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    public AdminActionLogController(
            AdminActionLogService adminActionLogService) {

        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping("/api/admin/logs")
    public ResponseEntity<List<AdminActionLogEntity>> getLogs(HttpSession session) {
        HttpStatus authorizationStatus = getAuthorizationStatus(session);
        if (authorizationStatus != HttpStatus.OK) {
            return ResponseEntity.status(authorizationStatus).build();
        }

        return ResponseEntity.ok(adminActionLogService.getLogs());
    }

    @PostMapping("/api/admin/logs/clear")
    public ResponseEntity<String> clearLogs(HttpSession session) {
        HttpStatus authorizationStatus = getAuthorizationStatus(session);
        if (authorizationStatus != HttpStatus.OK) {
            return ResponseEntity.status(authorizationStatus).body("Admin access is required.");
        }

        adminActionLogService.clearLogs();
        return ResponseEntity.ok("Admin logs cleared.");
    }

    private @NonNull HttpStatus getAuthorizationStatus(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        if (!(username instanceof String authenticatedUsername) || authenticatedUsername.isBlank()) {
            return HttpStatus.UNAUTHORIZED;
        }

        Object role = session.getAttribute(SessionAttributes.ROLE);
        if (!(role instanceof String userRole) || !"ADMIN".equalsIgnoreCase(userRole)) {
            return HttpStatus.FORBIDDEN;
        }

        return HttpStatus.OK;
    }
}