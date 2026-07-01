package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionEventEntity;
import com.btc.btc_auction.service.AuctionEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class AuctionEventController {

    private final AuctionEventService auctionEventService;

    public AuctionEventController(
            AuctionEventService auctionEventService) {

        this.auctionEventService = auctionEventService;
    }

    @GetMapping("/api/events")
    public List<AuctionEventEntity> getEvents() {

        return auctionEventService
                .getAllEvents();
    }
}