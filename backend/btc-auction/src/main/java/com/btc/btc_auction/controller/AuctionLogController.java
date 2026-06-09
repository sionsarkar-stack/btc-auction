package com.btc.btc_auction.controller;

import com.btc.btc_auction.model.AuctionLog;
import com.btc.btc_auction.service.AuctionLogService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class AuctionLogController {

    private final AuctionLogService auctionLogService;

    public AuctionLogController(
            AuctionLogService auctionLogService) {

        this.auctionLogService = auctionLogService;
    }

    @GetMapping("/api/logs")
    public List<AuctionLog> getLogs() {

        return auctionLogService.getLogs();
    }
}