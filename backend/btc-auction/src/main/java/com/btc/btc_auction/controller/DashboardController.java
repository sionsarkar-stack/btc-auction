package com.btc.btc_auction.controller;

import com.btc.btc_auction.dto.DashboardResponse;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.TeamService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {

    private final AuctionService auctionService;
    private final TeamService teamService;

    public DashboardController(AuctionService auctionService,
            TeamService teamService) {
        this.auctionService = auctionService;
        this.teamService = teamService;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse getDashboard() {

        return new DashboardResponse(
                auctionService.getCurrentAuction(),
                teamService.getAllTeams());
    }
}