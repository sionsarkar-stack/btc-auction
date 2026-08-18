package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.model.AddPlayerRequest;
import com.btc.btc_auction.service.PlayerService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
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
                request.getSeed(),
                request.getBasePrice(),
                request.getCategory());

        return "Player Added Successfully";
    }

    @PostMapping("/api/players/import")
    public String importPlayers(
            @RequestParam("file") MultipartFile file)
            throws Exception {

        playerService.importCsv(
                file);

        return "Players Imported Successfully";
    }
}
