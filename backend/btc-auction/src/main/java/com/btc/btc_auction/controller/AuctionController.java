package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.BountyPlayerEntity;
import com.btc.btc_auction.service.BountyPlayerService;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.ManualSaleRequest;
import com.btc.btc_auction.model.NominationRequest;
import com.btc.btc_auction.model.SellPlayerRequest;
import com.btc.btc_auction.model.TargetsReadyResponse;
import com.btc.btc_auction.model.UpdateAuctionRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.AuctionSocketService;
import com.btc.btc_auction.service.ReverseTargetService;
import com.btc.btc_auction.service.SecretTargetService;
import com.btc.btc_auction.service.TeamService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class AuctionController {

    private final AuctionService auctionService;

    private final AuctionConfigService auctionConfigService;

    private final AuctionSocketService auctionSocketService;

    private final SecretTargetService secretTargetService;

    private final ReverseTargetService reverseTargetService;

    private final TeamService teamService;

    private final BountyPlayerService bountyPlayerService;

    public AuctionController(AuctionService auctionService, AuctionConfigService auctionConfigService,
            AuctionSocketService auctionSocketService, SecretTargetService secretTargetService,
            ReverseTargetService reverseTargetService, TeamService teamService,
            BountyPlayerService bountyPlayerService) {
        this.auctionService = auctionService;
        this.auctionConfigService = auctionConfigService;
        this.auctionSocketService = auctionSocketService;
        this.secretTargetService = secretTargetService;
        this.reverseTargetService = reverseTargetService;
        this.teamService = teamService;
        this.bountyPlayerService = bountyPlayerService;
    }

    @GetMapping("/api/auction/current")
    public Auction getCurrentAuction() {
        return auctionService.getCurrentAuction();
    }

    @PostMapping("/api/auction/nominate")
    public String nominatePlayer(
            @RequestBody NominationRequest request) {

        return auctionService.nominatePlayer(
                request.getPlayerName(),
                request.getSeed(),
                request.getCaptainName());
    }

    @PostMapping("/api/auction/sold")
    public String sellPlayer(
            @RequestBody SellPlayerRequest request) {

        return auctionService.sellPlayer(
                request.getPlayerName(),
                request.getCaptainName(),
                request.getSoldPrice());
    }

    @PostMapping("/api/auction/undo")
    public String undoSale() {

        return auctionService.undoLastSale();
    }

    @PostMapping("/api/auction/veto")
    public String vetoCurrentAuction() {

        return auctionService.vetoCurrentAuction();
    }

    @PostMapping("/api/auction/veto-player")
    public String vetoSelectedPlayer(@RequestParam String playerName) {

        return auctionService.vetoCurrentAuction(playerName);
    }

    @PostMapping("/api/auction/cancel-nomination")
    public String cancelOwnNomination(@RequestParam String captainName) {

        return auctionService.cancelOwnNomination(captainName);
    }

    @PostMapping("/api/auction/manual-sale")
    public String manualSale(
            @RequestBody ManualSaleRequest request) {

        return auctionService.manualSale(
                request.getPlayerName(),
                request.getNewCaptain(),
                request.getNewPrice(), request.getReason());
    }

    @PostMapping("/api/auction/reset")
    public String resetAuction() {

        return auctionService.resetAuction();
    }

    @GetMapping("/api/auction/targets-ready")
    public TargetsReadyResponse checkTargetsReady() {
        List<String> teams = teamService.getAllTeams().stream()
                .map(team -> team.getCaptainName())
                .toList();

        List<String> missingSecret = new ArrayList<>();
        List<String> missingReverse = new ArrayList<>();

        for (String captain : teams) {
            if (secretTargetService.getByCaptain(captain) == null) {
                missingSecret.add(captain);
            }
            if (reverseTargetService.getByCaptain(captain) == null) {
                missingReverse.add(captain);
            }
        }

        int bountyCount = (int) bountyPlayerService.getAll().stream()
                .filter(bounty -> !bounty.isGolden())
                .count();
        int goldenBountyCount = (int) bountyPlayerService.getAll().stream()
                .filter(BountyPlayerEntity::isGolden)
                .count();
        boolean allSubmitted = missingSecret.isEmpty()
                && missingReverse.isEmpty();
        return new TargetsReadyResponse(allSubmitted, missingSecret, missingReverse,
                bountyCount, goldenBountyCount);
    }

    @PostMapping("/api/auction/start")
    public String startAuction() {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.isAuctionStarted()) {

            return "Auction already started.";

        }

        TargetsReadyResponse targetsReady = checkTargetsReady();
        if (!targetsReady.isAllSubmitted()) {
            List<String> missing = new ArrayList<>();
            for (String captain : targetsReady.getMissingSecretTargets()) {
                missing.add(captain + " (missing secret target)\n");
            }
            for (String captain : targetsReady.getMissingReverseTargets()) {
                if (!targetsReady.getMissingSecretTargets().contains(captain)) {
                    missing.add(captain + " (missing reverse target)\n");
                }
            }
            return "Cannot start auction. Missing targets from:\n" + String.join("", missing);
        }

        if (!bountyPlayerService.randomizeBounties()) {
            return "Cannot start auction. At least six unsold players are required for bounties.";
        }

        config.setAuctionStarted(true);

        auctionConfigService.save(config);

        auctionSocketService.broadcastRefresh();

        return "Auction Started Successfully. Four normal and two golden bounties were randomized and saved.";
    }

    @PostMapping("/api/auction/end")
    public String endAuction() {
        return auctionService.endAuction();
    }

    @GetMapping("/api/auction/status")
    public AuctionConfigEntity getStatus() {

        return auctionConfigService.getConfig();

    }

    @PostMapping("/api/auction/call-sold")
    public String callSold(
            @RequestBody(required = false) UpdateAuctionRequest request) {

        if (request == null) {
            return auctionService.callSold();
        }

        return auctionService.callSold(
                request.getCaptainName(),
                request.getCurrentBid());

    }

    @PostMapping("/api/auction/update-current")
    public String updateCurrentAuction(
            @RequestBody UpdateAuctionRequest request) {

        return auctionService.updateCurrentAuction(
                request.getCaptainName(),
                request.getCurrentBid());
    }
}
