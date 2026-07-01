package com.btc.btc_auction.controller;

import com.btc.btc_auction.dto.DashboardResponse;
import com.btc.btc_auction.dto.TeamDashboardDto;
import com.btc.btc_auction.repository.RtmRepository;
import com.btc.btc_auction.service.AuctionService;
import com.btc.btc_auction.service.PlayerService;
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
public class DashboardController {

        private final AuctionService auctionService;
        private final TeamService teamService;
        private final PlayerService playerService;
        private final RtmRepository rtmRepository;

        public DashboardController(
                        AuctionService auctionService,
                        TeamService teamService,
                        PlayerService playerService,
                        RtmRepository rtmRepository) {

                this.auctionService = auctionService;
                this.teamService = teamService;
                this.playerService = playerService;
                this.rtmRepository = rtmRepository;
        }

        @GetMapping("/api/dashboard")
        public DashboardResponse getDashboard() {

                List<TeamDashboardDto> teams = teamService.getAllTeams()
                                .stream()
                                .map(team -> {

                                        List<String> squad = playerService.getAllPlayers()
                                                        .stream()
                                                        .filter(player -> team.getCaptainName()
                                                                        .equals(player.getTeam()))
                                                        .map(player -> player.getName())
                                                        .toList();

                                        int maxBid = team.getPurse()

                                                        - (100 * (team.getPlayersLeft() - 1));

                                        boolean rtmAvailable = !rtmRepository.existsByCaptainNameAndUsedTrue(
                                                        team.getCaptainName());

                                        System.out.println(
                                                        team.getCaptainName()
                                                                        + " -> RTM Available = "
                                                                        + rtmAvailable);

                                        return new TeamDashboardDto(
                                                        team.getCaptainName(),
                                                        team.getPurse(),
                                                        team.getPlayersBought(),
                                                        team.getPlayersLeft(),
                                                        maxBid,
                                                        squad,
                                                        rtmAvailable);
                                })
                                .toList();

                return new DashboardResponse(
                                auctionService.getCurrentAuction(),
                                teams);
        }
}