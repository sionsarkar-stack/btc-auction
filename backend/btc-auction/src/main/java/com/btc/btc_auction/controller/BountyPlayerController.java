package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.BountyPlayerEntity;
import com.btc.btc_auction.model.BountyConfigRequest;
import com.btc.btc_auction.service.BountyPlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.Arrays;

@RestController
@CrossOrigin(origins = {

                "http://localhost:5173",

                "http://localhost:8080"

})
public class BountyPlayerController {

        private final BountyPlayerService service;

        public BountyPlayerController(
                        BountyPlayerService service) {

                this.service = service;
        }

        @GetMapping("/api/bounty")
        public List<BountyPlayerEntity> getBounties() {

                return service.getAll();
        }

        @PostMapping("/api/bounty")
        public String saveBounties(
                        @RequestBody BountyConfigRequest request) {

                List<String> selectedPlayers = Arrays.asList(
                                request.getPlayerOne(), request.getPlayerTwo(),
                                request.getPlayerThree(), request.getPlayerFour(),
                                request.getGoldenPlayerOne(), request.getGoldenPlayerTwo());

                if (selectedPlayers.stream().anyMatch(player -> player == null || player.isBlank())
                                || Set.copyOf(selectedPlayers).size() != 6) {
                        return "Select six different players: four normal and two golden bounties.";
                }

                service.deleteAll();

                savePlayer(request.getPlayerOne(), false);
                savePlayer(request.getPlayerTwo(), false);
                savePlayer(request.getPlayerThree(), false);
                savePlayer(request.getPlayerFour(), false);
                savePlayer(request.getGoldenPlayerOne(), true);
                savePlayer(request.getGoldenPlayerTwo(), true);

                return "Bounty Players Saved";
        }

        private void savePlayer(
                        String playerName,
                        boolean golden) {

                if (playerName == null ||
                                playerName.isBlank()) {
                        return;
                }

                BountyPlayerEntity bounty = new BountyPlayerEntity();

                bounty.setPlayerName(
                                playerName);

                bounty.setGolden(golden);

                bounty.setRevealed(false);

                service.save(bounty);
        }
}
