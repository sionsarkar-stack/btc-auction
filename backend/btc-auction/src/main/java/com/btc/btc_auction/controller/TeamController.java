package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.service.TeamService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/api/teams")
    public List<TeamEntity> getTeams() {
        return teamService.getAllTeams();
    }
}
