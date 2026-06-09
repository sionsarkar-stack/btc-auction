package com.btc.btc_auction.controller;

import com.btc.btc_auction.model.Player;
import com.btc.btc_auction.service.PlayerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/api/players")
    public List<Player> getPlayers() {
        return playerService.getPlayers();
    }

    @GetMapping("/api/players/available")
    public List<Player> getAvailablePlayers() {
        return playerService.getUnsoldPlayers();
    }
}