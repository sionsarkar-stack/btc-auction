package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.BountyPlayerEntity;
import com.btc.btc_auction.model.BountyConfigRequest;
import com.btc.btc_auction.service.BountyPlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

                service.deleteAll();

                savePlayer(
                                request.getPlayerOne(),
                                request.getGoldenPlayer());

                savePlayer(
                                request.getPlayerTwo(),
                                request.getGoldenPlayer());

                savePlayer(
                                request.getPlayerThree(),
                                request.getGoldenPlayer());

                savePlayer(
                                request.getPlayerFour(),
                                request.getGoldenPlayer());

                return "Bounty Players Saved";
        }

        private void savePlayer(
                        String playerName,
                        String goldenPlayer) {

                if (playerName == null ||
                                playerName.isBlank()) {
                        return;
                }

                BountyPlayerEntity bounty = new BountyPlayerEntity();

                bounty.setPlayerName(
                                playerName);

                bounty.setGolden(
                                playerName.equalsIgnoreCase(
                                                goldenPlayer));

                bounty.setRevealed(false);

                service.save(bounty);
        }
}