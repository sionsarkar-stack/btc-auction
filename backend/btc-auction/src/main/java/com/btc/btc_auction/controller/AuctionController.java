package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.ManualSaleRequest;
import com.btc.btc_auction.model.SellPlayerRequest;
import com.btc.btc_auction.model.UpdateAuctionRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.AuctionSocketService;
import com.btc.btc_auction.service.ClubbedPlayerPairService;
import com.btc.btc_auction.service.TeamService;
import com.btc.btc_auction.service.PlayerService;
import com.btc.btc_auction.service.ProtectionPlayerService;
import com.btc.btc_auction.security.SessionAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

}, allowCredentials = "true")
public class AuctionController {

    private final AuctionService auctionService;

    private final AuctionConfigService auctionConfigService;

    private final AuctionSocketService auctionSocketService;

    private final TeamService teamService;

    private final PlayerService playerService;

    private final ClubbedPlayerPairService clubbedPlayerPairService;

    private final ProtectionPlayerService protectionPlayerService;

    public AuctionController(AuctionService auctionService, AuctionConfigService auctionConfigService,
            AuctionSocketService auctionSocketService, TeamService teamService,
            ClubbedPlayerPairService clubbedPlayerPairService,
            PlayerService playerService, ProtectionPlayerService protectionPlayerService) {
        this.auctionService = auctionService;
        this.auctionConfigService = auctionConfigService;
        this.auctionSocketService = auctionSocketService;
        this.teamService = teamService;
        this.playerService = playerService;
        this.protectionPlayerService = protectionPlayerService;
        this.clubbedPlayerPairService = clubbedPlayerPairService;
    }

    @GetMapping("/api/auction/current")
    public Auction getCurrentAuction() {
        return auctionService.getCurrentAuction();
    }

    @PostMapping("/api/auction/spin-wheel")
    public String spinWheel() {
        return auctionService.spinWheel();
    }

    @PostMapping("/api/admin/protection-selection/enable")
    public String enableProtectionSelection() {
        return auctionService.enableProtectionSelection();
    }

    @PostMapping("/api/auction/re-auction/start")
    public String startReAuction() {
        return auctionService.startReAuction();
    }

    @PostMapping("/api/auction/wild-pick")
    public ResponseEntity<String> useWildPick(HttpSession session) {
        Object username = session.getAttribute(SessionAttributes.USERNAME);
        Object role = session.getAttribute(SessionAttributes.ROLE);
        if (!(username instanceof String captainName) || captainName.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sign in as a captain to activate Wildcard.");
        }
        if (!(role instanceof String userRole) || !"CAPTAIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only captains can activate Wildcard.");
        }
        return ResponseEntity.ok(auctionService.useWildPick(captainName));
    }

    @PostMapping("/api/auction/blind-opening-bid/start")
    public String startBlindOpeningBid(@RequestBody java.util.Map<String, String> request) {
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.startBlindOpeningBid(playerName);
    }

    @GetMapping("/api/auction/blind-opening-bid/{playerName}/{captainName}")
    public Object getBlindOpeningBid(@PathVariable String playerName, @PathVariable String captainName) {
        return auctionService.getBlindOpeningBid(playerName, captainName);
    }

    @PostMapping("/api/auction/blind-opening-bid/submit")
    public String submitBlindOpeningBid(@RequestBody java.util.Map<String, Object> request) {
        String playerName = (String) request.get("playerName");
        String captainName = (String) request.get("captainName");
        Object rawBid = request.get("bidAmount");

        if (playerName == null || playerName.isBlank() || captainName == null || captainName.isBlank()) {
            return "Player and captain names are required.";
        }

        if (rawBid == null) {
            return "Bid amount is required.";
        }

        int bidAmount;
        try {
            bidAmount = Integer.parseInt(rawBid.toString());
        } catch (NumberFormatException ex) {
            return "Bid amount must be a valid number.";
        }

        return auctionService.submitBlindOpeningBid(playerName, captainName, bidAmount);
    }

    @PostMapping("/api/auction/blind-opening-bid/reveal")
    public String revealBlindOpeningBid(@RequestBody java.util.Map<String, String> request) {
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.revealBlindOpeningBid(playerName);
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

    @PostMapping("/api/auction/manual-sale")
    public String manualSale(
            @RequestBody ManualSaleRequest request) {

        return auctionService.manualSale(
                request.getPlayerName(),
                request.getNewCaptain(),
                request.getNewPrice(), request.getReason());
    }

    @PostMapping("/api/auction/cancel-sale")
    public String cancelSale(@RequestBody java.util.Map<String, String> request) {
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.cancelSale(playerName);
    }

    @PostMapping("/api/auction/reset")
    public String resetAuction() {

        return auctionService.resetAuction();
    }

    @PostMapping("/api/auction/start")
    public String startAuction() {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.isAuctionStarted()) {

            return "Auction already started.";

        }

        int playerCount = playerService.getAllPlayers().size();
        if (playerCount == 0) {
            return "Cannot start auction. No players found.";
        }

        List<com.btc.btc_auction.entity.TeamEntity> configuredTeams = teamService.getAllTeams();
        if (configuredTeams.isEmpty()) {
            return "Cannot start auction. Configure captain teams first.";
        }

        List<String> missingProtection = configuredTeams.stream()
                .filter(team -> protectionPlayerService.getByCaptain(team.getCaptainName()).size() != 2)
                .map(team -> team.getCaptainName() + " (needs 2 protection players)")
                .toList();
        if (!missingProtection.isEmpty()) {
            return "Cannot start auction. Missing protection players: " + String.join(", ", missingProtection);
        }

        config.setAuctionStarted(true);

        auctionConfigService.save(config);

        auctionSocketService.broadcastRefresh();

        return "Auction Started Successfully.";
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

    @PostMapping("/api/auction/random-event")
    public String triggerRandomEvent() {
        return auctionService.triggerRandomEventNow();
    }

    @GetMapping("/api/auction/clubbed-pair")
    public Object getClubbedPair() {
        return clubbedPlayerPairService.getActivePairs();
    }

    @PostMapping("/api/auction/clubbed-pair")
    public String createClubbedPair(@RequestBody java.util.Map<String, String> request) {
        String playerOne = request.get("playerOne");
        String playerTwo = request.get("playerTwo");

        if (playerOne == null || playerTwo == null || playerOne.isBlank() || playerTwo.isBlank()) {
            return "Both players are required to create an A + E pair.";
        }

        if (playerOne.equalsIgnoreCase(playerTwo)) {
            return "A player cannot be paired with itself.";
        }

        clubbedPlayerPairService.createPair(playerOne, playerTwo);
        return "A + E pair added: " + playerOne + " + " + playerTwo;
    }

    @DeleteMapping("/api/auction/clubbed-pair")
    public String clearClubbedPair() {
        clubbedPlayerPairService.clearPair();
        return "A + E pair cleared.";
    }

    @DeleteMapping("/api/auction/clubbed-pair/{id}")
    public String deleteClubbedPair(@PathVariable Long id) {
        clubbedPlayerPairService.deletePair(id);
        return "A + E pair deleted.";
    }
}
