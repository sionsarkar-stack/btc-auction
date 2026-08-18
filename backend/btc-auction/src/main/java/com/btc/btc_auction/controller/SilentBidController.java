package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.SilentBidEntity;
import com.btc.btc_auction.model.SilentBidRequest;
import com.btc.btc_auction.model.SilentBidStartRequest;
import com.btc.btc_auction.service.SilentBidService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.btc.btc_auction.model.SilentBidResult;

import java.util.List;

@RestController
@RequestMapping("/api/silent-bid")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class SilentBidController {

    private final SilentBidService silentBidService;

    public SilentBidController(
            SilentBidService silentBidService) {

        this.silentBidService = silentBidService;

    }

    @PostMapping("/submit")
    public String submitBid(
            @RequestBody SilentBidRequest request) {

        return silentBidService.submitBid(
                request.getPlayerName(),
                request.getCaptainName(),
                request.getBidAmount());

    }

    @GetMapping("/all")
    public List<SilentBidEntity> getAllBids() {

        return silentBidService.getAllBids();

    }

    @GetMapping("/winner")
    public ResponseEntity<?> getWinner() {

        SilentBidResult result = silentBidService.getResult();

        if (result.isTie()) {

            return ResponseEntity.ok(result);

        }

        if (result.getWinner() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Waiting for all captains to submit.");

        }

        return ResponseEntity.ok(result);

    }

    @PostMapping("/clear")
    public String clearRound() {

        silentBidService.clearRound();

        return "Silent bid cleared.";

    }

    @PostMapping("/start")
    public String startRound(
            @RequestBody SilentBidStartRequest request) {

        silentBidService.startRound(
                request.getPlayerName());

        return "Silent bidding started.";

    }

    @GetMapping("/{captainName}")
    public SilentBidEntity getCaptainBid(
            @PathVariable String captainName) {

        return silentBidService.getCaptainBid(
                captainName);

    }

    @GetMapping("/active")
    public boolean isActive() {

        return silentBidService.isRoundActive();

    }

    @PostMapping("/sell")
    public String sellWinner() {

        return silentBidService.sellWinner();

    }

    @PostMapping("/call-sold")
    public String callSoldWinner() {

        return silentBidService.callSoldWinner();

    }

}