package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.JokerEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.enums.JokerType;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.JokerUseRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.JokerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jokers")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class JokerController {

    private final JokerService jokerService;

    private final AuctionConfigService auctionConfigService;
    private final AuctionService auctionService;

    public JokerController(
            JokerService jokerService,
            AuctionConfigService auctionConfigService,
            AuctionService auctionService) {

        this.jokerService = jokerService;
        this.auctionConfigService = auctionConfigService;
        this.auctionService = auctionService;
    }

    @GetMapping
    public List<JokerEntity> getAllJokers() {

        return jokerService.getAll();

    }

    @GetMapping("/{captainName}")
    public List<JokerEntity> getCaptainJokers(
            @PathVariable String captainName) {

        return jokerService.getCaptainJokers(
                captainName);

    }

    @PostMapping("/use")
    public String useJoker(
            @RequestBody JokerUseRequest request) {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (!config.isAuctionStarted()) {

            return "Jokers are locked until the auction starts.";

        }

        if (request.getJokerType() == JokerType.VETO &&
                config.getAuctionPhase() != AuctionPhase.NOMINATION) {

            return "VETO can only be used during nomination.";

        }
        if (request.getJokerType() == JokerType.LAST_STRIKE &&
                config.getAuctionPhase() != AuctionPhase.SOLD) {

            return "LAST STRIKE can only be used after SOLD.";

        }

        Auction auction = auctionService.getCurrentAuction();

        if (auction == null ||
                auction.getCurrentPlayer() == null ||
                auction.getCurrentPlayer().trim().isEmpty()) {

            return "No player is currently nominated.";

        }

        String result = jokerService.useJoker(
                request.getCaptainName(),
                request.getJokerType());

        if (!result.contains("activated")) {
            return result;
        }

        switch (request.getJokerType()) {

            case VETO:
                auctionService.vetoCurrentAuction();
                break;

            case LAST_STRIKE:
                auctionService.applyLastStrike(request.getCaptainName());
                break;

            case STEAL_BID:
                // Future implementation
                break;

            case BID_BLOCK:
                // Future implementation
                break;
        }

        return result;

    }

    @PostMapping("/reset")
    public String resetJokers() {

        jokerService.resetAll();

        return "All jokers reset.";

    }

}