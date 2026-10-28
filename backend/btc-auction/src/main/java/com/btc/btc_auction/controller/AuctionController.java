package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.ManualSaleRequest;
import com.btc.btc_auction.model.SellPlayerRequest;
import com.btc.btc_auction.model.UpdateAuctionRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.ClubbedPlayerPairService;
import com.btc.btc_auction.service.PlayerService;
import com.btc.btc_auction.security.SessionAuthorization;
import com.btc.btc_auction.security.SessionAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

}, allowCredentials = "true")
public class AuctionController {

    private final AuctionService auctionService;

    private final AuctionConfigService auctionConfigService;

    private final PlayerService playerService;

    private final ClubbedPlayerPairService clubbedPlayerPairService;

    public AuctionController(AuctionService auctionService, AuctionConfigService auctionConfigService,
            ClubbedPlayerPairService clubbedPlayerPairService, PlayerService playerService) {
        this.auctionService = auctionService;
        this.auctionConfigService = auctionConfigService;
        this.playerService = playerService;
        this.clubbedPlayerPairService = clubbedPlayerPairService;
    }

    @GetMapping("/api/auction/current")
    public Auction getCurrentAuction() {
        return auctionService.getCurrentAuction();
    }

    @PostMapping("/api/auction/spin-wheel")
    public String spinWheel(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.spinWheel();
    }

    @PostMapping("/api/auction/re-auction/start")
    public String startReAuction(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.startReAuction();
    }

    @PostMapping("/api/auction/re-auction/cancel")
    public String cancelReAuction(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.cancelReAuction();
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
    public String startBlindOpeningBid(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.startBlindOpeningBid(playerName);
    }

    @GetMapping("/api/auction/blind-opening-bid/{playerName}/{captainName}")
    public Object getBlindOpeningBid(
            @PathVariable String playerName,
            @PathVariable String captainName,
            HttpSession session) {
        requireCaptainOrAdmin(session);
        if (!SessionAuthorization.hasRole(session, "ADMIN")
                && !SessionAuthorization.getUsername(session).equalsIgnoreCase(captainName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Captains can only view their own bid.");
        }
        return auctionService.getBlindOpeningBid(playerName, captainName);
    }

    @GetMapping("/api/auction/blind-opening-bid/{playerName}")
    public Object getAllBlindOpeningBids(@PathVariable String playerName, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.getAllBlindOpeningBids(playerName);
    }

    @GetMapping("/api/auction/blind-opening-bid/{playerName}/all-passed")
    public boolean allBlindOpeningBidsPassed(@PathVariable String playerName, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.allBlindOpeningBidsPassed(playerName);
    }

    @PostMapping("/api/auction/blind-opening-bid/reveal")
    public String revealBlindOpeningBid(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.revealBlindOpeningBid(playerName);
    }

    @PostMapping("/api/auction/blind-opening-bid/submit")
    public String submitBlindOpeningBid(@RequestBody java.util.Map<String, Object> request, HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        String playerName = (String) request.get("playerName");
        Object rawBid = request.get("bidAmount");

        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
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

        return auctionService.submitBlindOpeningBid(
                playerName, SessionAuthorization.getUsername(session), bidAmount);
    }

    @PostMapping("/api/auction/blind-opening-bid/pass")
    public String passBlindOpeningBid(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        String playerName = request.get("playerName");

        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }

        return auctionService.passBlindOpeningBid(playerName, SessionAuthorization.getUsername(session));
    }

    @PostMapping("/api/auction/sold")
    public String sellPlayer(
            @RequestBody SellPlayerRequest request,
            HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");

        return auctionService.sellPlayer(
                request.getPlayerName(),
                request.getCaptainName(),
                request.getSoldPrice());
    }

    @PostMapping("/api/auction/mark-unsold")
    public String markUnsold(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.markCurrentPlayerUnsold();
    }

    @PostMapping("/api/auction/undo")
    public String undoSale(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");

        return auctionService.undoLastSale();
    }

    @PostMapping("/api/auction/manual-sale")
    public String manualSale(
            @RequestBody ManualSaleRequest request,
            HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");

        return auctionService.manualSale(
                request.getPlayerName(),
                request.getNewCaptain(),
                request.getNewPrice(), request.getReason());
    }

    @PostMapping("/api/auction/cancel-sale")
    public String cancelSale(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.cancelSale(playerName);
    }

    @PostMapping("/api/auction/reset")
    public String resetAuction(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");

        return auctionService.resetAuction();
    }

    @PostMapping("/api/auction/start")
    public String startAuction(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.startNominationAuction();
    }

    @PostMapping("/api/auction/nominate")
    public String nominatePlayer(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        String playerName = request.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return "Player name is required.";
        }
        return auctionService.nominatePlayer(playerName, SessionAuthorization.getUsername(session));
    }

    @PostMapping("/api/auction/bid/increase")
    public String increaseLiveBid(HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        return auctionService.increaseLiveBid(SessionAuthorization.getUsername(session));
    }

    @PostMapping("/api/auction/bid/custom")
    public String placeCustomLiveBid(@RequestBody java.util.Map<String, Object> request, HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        Object rawBid = request.get("bidAmount");
        if (rawBid == null) {
            return "Enter a valid bid amount.";
        }
        try {
            return auctionService.placeCustomLiveBid(
                    SessionAuthorization.getUsername(session), Integer.parseInt(rawBid.toString()));
        } catch (NumberFormatException exception) {
            return "Bid amount must be a whole number.";
        }
    }

    @PostMapping("/api/auction/bid/pass")
    public String passOnPlayer(HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        return auctionService.passOnPlayer(SessionAuthorization.getUsername(session));
    }

    @PostMapping("/api/auction/timeout")
    public String callAuctionTimeout(HttpSession session) {
        requireRole(session, "CAPTAIN", "Captain access required.");
        return auctionService.callAuctionTimeout(SessionAuthorization.getUsername(session));
    }

    @PostMapping("/api/auction/end")
    public String endAuction(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.endAuction();
    }

    @GetMapping("/api/auction/status")
    public AuctionConfigEntity getStatus(HttpSession session) {
        requireAuthenticated(session);
        auctionService.completeWheelSpinWhenDue();
        AuctionConfigEntity config = auctionConfigService.getConfig();
        config.setServerTime(System.currentTimeMillis());

        if (config.getAuctionPhase() == com.btc.btc_auction.enums.AuctionPhase.SPINNING) {
            List<PlayerEntity> availablePlayers = playerService.getUnsoldPlayers();
            if (config.getAuctionRound() == 2) {
                availablePlayers = availablePlayers.stream()
                        .filter(player -> !player.isReAuctioned())
                        .toList();
            } else {
                availablePlayers = availablePlayers.stream()
                        .filter(player -> !player.isDeferredToReAuction())
                        .toList();
            }
            config.setWalkoutPlayerNames(availablePlayers.stream()
                    .filter(Objects::nonNull)
                    .filter(player -> player.getName() != null)
                    .filter(player -> !clubbedPlayerPairService.isPlayerTwo(player.getName()))
                    .map(player -> player.getName())
                    .sorted((first, second) -> {
                        int caseInsensitiveOrder = first.compareToIgnoreCase(second);
                        return caseInsensitiveOrder == 0 ? first.compareTo(second) : caseInsensitiveOrder;
                    })
                    .toList());
        } else {
            config.setWalkoutPlayerNames(List.of());
        }

        return config;
    }

    @PostMapping("/api/auction/call-sold")
    public String callSold(
            @RequestBody(required = false) UpdateAuctionRequest request,
            HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");

        if (request == null) {
            return auctionService.callSold();
        }

        return auctionService.callSold(
                request.getCaptainName(),
                request.getCurrentBid());

    }

    @PostMapping("/api/auction/update-current")
    public String updateCurrentAuction(
            @RequestBody UpdateAuctionRequest request,
            HttpSession session) {
        requireCaptainOrAdmin(session);

        String captainName = SessionAuthorization.hasRole(session, "ADMIN")
                ? request.getCaptainName()
                : SessionAuthorization.getUsername(session);

        return auctionService.updateCurrentAuction(
                captainName,
                request.getCurrentBid());
    }

    @PostMapping("/api/auction/random-event")
    public String triggerRandomEvent(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        return auctionService.triggerRandomEventNow();
    }

    @GetMapping("/api/auction/clubbed-pair")
    public Object getClubbedPair() {
        return clubbedPlayerPairService.getActivePairs();
    }

    @PostMapping("/api/auction/clubbed-pair")
    public String createClubbedPair(@RequestBody java.util.Map<String, String> request, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        String playerOne = request.get("playerOne");
        String playerTwo = request.get("playerTwo");

        if (playerOne == null || playerTwo == null || playerOne.isBlank() || playerTwo.isBlank()) {
            return "Both players are required to create an A + C pair.";
        }

        if (playerOne.equalsIgnoreCase(playerTwo)) {
            return "A player cannot be paired with itself.";
        }

        PlayerEntity firstPlayer = playerService.getPlayer(playerOne);
        PlayerEntity secondPlayer = playerService.getPlayer(playerTwo);
        if (firstPlayer == null || secondPlayer == null) {
            return "Both selected players must exist.";
        }
        if (!"A".equalsIgnoreCase(firstPlayer.getSeed()) || !"C".equalsIgnoreCase(secondPlayer.getSeed())) {
            return "A + C pairing requires an A-seeded first player and a C-seeded second player.";
        }

        clubbedPlayerPairService.createPair(playerOne, playerTwo);
        return "A + C pair added: " + playerOne + " + " + playerTwo;
    }

    @DeleteMapping("/api/auction/clubbed-pair")
    public String clearClubbedPair(HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        clubbedPlayerPairService.clearPair();
        return "A + C pair cleared.";
    }

    @DeleteMapping("/api/auction/clubbed-pair/{id}")
    public String deleteClubbedPair(@PathVariable Long id, HttpSession session) {
        requireRole(session, "ADMIN", "Admin access required.");
        clubbedPlayerPairService.deletePair(id);
        return "A + C pair deleted.";
    }

    private void requireRole(HttpSession session, String expectedRole, String message) {
        ResponseEntity<String> authorization = SessionAuthorization.requireRole(session, expectedRole, message);
        if (authorization != null) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(authorization.getStatusCode().value()), authorization.getBody());
        }
    }

    private void requireAuthenticated(HttpSession session) {
        ResponseEntity<String> authorization = SessionAuthorization.requireAuthenticated(session);
        if (authorization != null) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(authorization.getStatusCode().value()), authorization.getBody());
        }
    }

    private void requireCaptainOrAdmin(HttpSession session) {
        requireAuthenticated(session);
        if (!SessionAuthorization.hasRole(session, "CAPTAIN")
                && !SessionAuthorization.hasRole(session, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Captain or admin access required.");
        }
    }
}
