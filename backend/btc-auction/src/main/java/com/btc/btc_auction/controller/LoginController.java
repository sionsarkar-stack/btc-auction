package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.model.LoginRequest;
import com.btc.btc_auction.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class LoginController {

    private final UserService userService;

    public LoginController(
            UserService userService) {

        this.userService = userService;
    }

    @PostMapping("/api/login")
    public UserEntity login(
            @RequestBody LoginRequest request) {

        return userService.login(
                request.getUsername(),
                request.getPassword());
    }
}