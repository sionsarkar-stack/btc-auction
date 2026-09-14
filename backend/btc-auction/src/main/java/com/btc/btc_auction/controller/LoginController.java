package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.model.LoginRequest;
import com.btc.btc_auction.model.LoginResponse;
import com.btc.btc_auction.security.SessionAttributes;
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

    public LoginController(
            UserService userService) {

        this.userService = userService;
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

        session.setAttribute(SessionAttributes.USERNAME, user.getUsername());
        session.setAttribute(SessionAttributes.ROLE, user.getRole());

        return ResponseEntity.ok(new LoginResponse(user.getUsername(), user.getRole()));

    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpSession session) {

        session.invalidate();

        return ResponseEntity.noContent().build();

    }
}