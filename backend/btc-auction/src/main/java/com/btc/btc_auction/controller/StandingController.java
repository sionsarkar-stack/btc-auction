package com.btc.btc_auction.controller;

import com.btc.btc_auction.model.TeamStanding;
import com.btc.btc_auction.service.StandingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class StandingController {

    private final StandingService standingService;

    public StandingController(
            StandingService standingService) {

        this.standingService = standingService;
    }

    @GetMapping("/api/standings")
    public List<TeamStanding> getStandings() {

        return standingService
                .getStandings();
    }
}