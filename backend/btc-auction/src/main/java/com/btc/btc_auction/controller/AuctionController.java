package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.ManualSaleRequest;
import com.btc.btc_auction.model.NominationRequest;
import com.btc.btc_auction.model.SellPlayerRequest;
import com.btc.btc_auction.model.UpdateAuctionRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.JokerService;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class AuctionController {

    private final AuctionService auctionService;

    private final AuctionConfigService auctionConfigService;

    private final JokerService jokerService;

    public AuctionController(AuctionService auctionService, AuctionConfigService auctionConfigService,
            JokerService jokerService) {
        this.auctionService = auctionService;
        this.auctionConfigService = auctionConfigService;
        this.jokerService = jokerService;
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

    @PostMapping("/api/auction/start")
    public String startAuction() {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.isAuctionStarted()) {

            return "Auction already started.";

        }

        config.setAuctionStarted(true);

        auctionConfigService.save(config);

        jokerService.assignRandomJokers();

        return "Auction Started Successfully";
    }

    @PostMapping("/api/auction/end")
    public String endAuction() {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        config.setAuctionStarted(false);

        auctionConfigService.save(config);

        return "Auction Ended";
    }

    @GetMapping("/api/auction/status")
    public AuctionConfigEntity getStatus() {

        return auctionConfigService.getConfig();

    }

    @PostMapping("/api/auction/call-sold")
    public String callSold() {

        return auctionService.callSold();

    }

    @PostMapping("/api/auction/update-current")
    public String updateCurrentAuction(
            @RequestBody UpdateAuctionRequest request) {

        return auctionService.updateCurrentAuction(
                request.getCaptainName(),
                request.getCurrentBid());
    }
}