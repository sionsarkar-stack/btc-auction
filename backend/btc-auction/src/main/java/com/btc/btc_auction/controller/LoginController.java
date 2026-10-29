package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.model.LoginRequest;
import com.btc.btc_auction.model.LoginResponse;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.CaptainPresenceService;
import com.btc.btc_auction.service.UserService;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

}, allowCredentials = "true")
public class LoginController {

    private final UserService userService;
    private final CaptainPresenceService captainPresenceService;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginController(
            UserService userService,
            CaptainPresenceService captainPresenceService) {

        this.userService = userService;
        this.captainPresenceService = captainPresenceService;
    }

    public LoginController(UserService userService) {
        this(userService, null);
    }

    @PostMapping("/api/login")

    public ResponseEntity<LoginResponse> login(

            @RequestBody LoginRequest request,
            HttpSession session) {

        UserEntity user =

                userService.login(

                        request.getUsername(),

                        request.getPassword());

        if (user == null) {

            return ResponseEntity

                    .status(HttpStatus.UNAUTHORIZED)

                    .build();

        }

        if (captainPresenceService != null) {
            captainPresenceService.signOut(session.getId());
        }
        session.setAttribute(SessionAttributes.USERNAME, user.getUsername());
        session.setAttribute(SessionAttributes.ROLE, user.getRole());
        if (captainPresenceService != null) {
            captainPresenceService.checkIn(session.getId(), user.getUsername(), user.getRole());
        }

        return ResponseEntity.ok(new LoginResponse(user.getUsername(), user.getRole()));

    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpSession session) {

        if (captainPresenceService != null) {
            captainPresenceService.signOut(session.getId());
        }
        session.invalidate();

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/api/session")
    public ResponseEntity<LoginResponse> getSession(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        Object role = session.getAttribute(SessionAttributes.ROLE);
        if (!(username instanceof String authenticatedUsername)
                || authenticatedUsername.isBlank()
                || !(role instanceof String authenticatedRole)
                || authenticatedRole.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new LoginResponse(authenticatedUsername, authenticatedRole));
    }
}