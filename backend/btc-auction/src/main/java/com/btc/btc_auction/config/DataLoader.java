package com.btc.btc_auction.config;

import com.btc.btc_auction.service.PlayerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final PlayerService playerService;

    public DataLoader(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void run(String... args) {

        if (playerService.getAllPlayers().isEmpty()) {

            playerService.addPlayer("Sawon", "Z");
            playerService.addPlayer("Sunny", "A");
            playerService.addPlayer("Ujjwal", "Z");
            playerService.addPlayer("Arnab", "B");

            System.out.println("Players Loaded");
        }
    }
}