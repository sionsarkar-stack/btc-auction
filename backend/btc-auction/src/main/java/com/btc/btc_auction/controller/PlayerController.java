package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.service.PlayerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}