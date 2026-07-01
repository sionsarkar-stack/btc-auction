package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.RtmBidRequest;
import com.btc.btc_auction.model.RtmDecisionRequest;
import com.btc.btc_auction.model.RtmRequest;
import com.btc.btc_auction.service.AuctionConfigService;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.RtmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rtm")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class RtmController {

    private final RtmService rtmService;

    private final AuctionConfigService auctionConfigService;

    private final AuctionService auctionService;

    public RtmController(
            RtmService rtmService,
            AuctionConfigService auctionConfigService,
            AuctionService auctionService) {

        this.rtmService = rtmService;
        this.auctionConfigService = auctionConfigService;
        this.auctionService = auctionService;
    }

    @PostMapping("/claim")
    public String claimRtm(
            @RequestBody RtmRequest request) {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.getAuctionPhase() != AuctionPhase.SOLD) {

            return "RTM is available only after SOLD.";
        }

        return rtmService.claimRtm(
                request.getCaptainName());
    }

    @GetMapping("/current")
    public RtmEntity getCurrent() {

        return rtmService.getCurrentRtm();

    }

    @PostMapping("/reset")
    public String reset() {

        rtmService.clear();

        return "RTM reset.";

    }

    @PostMapping("/confirm")
    public String confirm() {

        return rtmService.confirmRtm();

    }

    @PostMapping("/cancel")
    public String cancel() {

        return rtmService.cancelRtm();

    }

    @PostMapping("/bid")
    public String submitBid(
            @RequestBody RtmBidRequest request) {

        return rtmService.submitBid(
                request.getBidAmount());

    }

    @PostMapping("/accept")
    public String accept(
            @RequestBody RtmDecisionRequest request) {

        return auctionService.acceptRtm(
                request.getCaptainName());

    }

    @PostMapping("/decline")
    public String decline(
            @RequestBody RtmDecisionRequest request) {

        return auctionService.declineRtm(
                request.getCaptainName());

    }

}