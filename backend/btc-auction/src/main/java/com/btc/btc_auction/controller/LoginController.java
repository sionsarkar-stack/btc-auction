package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.model.LoginRequest;
import com.btc.btc_auction.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class LoginController {

    private final UserService userService;

    public LoginController(
            UserService userService) {

        this.userService = userService;
    }

    @PostMapping("/api/login")

    public ResponseEntity<UserEntity> login(

            @RequestBody LoginRequest request) {

        UserEntity user =

                userService.login(

                        request.getUsername(),

                        request.getPassword());

        if (user == null) {

            return ResponseEntity

                    .status(HttpStatus.UNAUTHORIZED)

                    .build();

        }

        return ResponseEntity.ok(user);

    }
}