package com.btc.btc_auction.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class SessionAuthorization {

    private SessionAuthorization() {
    }

    public static ResponseEntity<String> requireAuthenticated(HttpSession session) {
        if (!hasIdentity(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sign in to continue.");
        }
        return null;
    }

    public static ResponseEntity<String> requireRole(HttpSession session, String expectedRole, String message) {
        if (!hasIdentity(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sign in to continue.");
        }
        if (!hasRole(session, expectedRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
        }
        return null;
    }

    public static String getUsername(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        return username instanceof String value ? value : null;
    }

    public static boolean hasRole(HttpSession session, String expectedRole) {
        Object role = session.getAttribute(SessionAttributes.ROLE);
        return role instanceof String value && expectedRole.equalsIgnoreCase(value);
    }

    private static boolean hasIdentity(HttpSession session) {
        String username = getUsername(session);
        Object role = session.getAttribute(SessionAttributes.ROLE);
        return username != null && !username.isBlank() && role instanceof String value && !value.isBlank();
    }
}