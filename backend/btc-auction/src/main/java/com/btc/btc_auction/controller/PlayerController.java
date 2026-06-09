package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.model.AddPlayerRequest;
import com.btc.btc_auction.service.PlayerService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(
            PlayerService playerService) {

        this.playerService = playerService;
    }

    @GetMapping("/api/players")
    public List<PlayerEntity> getPlayers() {

        return playerService.getAllPlayers();
    }

    @GetMapping("/api/players/available")
    public List<PlayerEntity> getAvailablePlayers() {

        return playerService.getUnsoldPlayers();
    }

    @PostMapping("/api/players")
    public String addPlayer(
            @RequestBody AddPlayerRequest request) {

        playerService.addPlayer(
                request.getName(),
                request.getSeed());

        return "Player Added Successfully";
    }
}