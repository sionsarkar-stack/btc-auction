package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionEventEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.TeamStanding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StandingService {

    private final TeamService teamService;
    private final AuctionEventService auctionEventService;

    public StandingService(
            TeamService teamService,
            AuctionEventService auctionEventService) {

        this.teamService = teamService;
        this.auctionEventService = auctionEventService;
    }

    public List<TeamStanding> getStandings() {

        List<TeamStanding> standings = new ArrayList<>();

        List<TeamEntity> teams = teamService.getAllTeams();

        List<AuctionEventEntity> events = auctionEventService
                .getAllEvents();

        for (TeamEntity team : teams) {

            TeamStanding standing = new TeamStanding();

            standing.setCaptainName(
                    team.getCaptainName());

            standing.setPurse(
                    team.getPurse());

            standing.setPlayersBought(
                    team.getPlayersBought());

            standing.setTargetsAchieved(

                    events.stream()
                            .filter(e -> "TARGET_ACHIEVED"
                                    .equals(
                                            e.getEventType())
                                    ||
                                    "ALL_TARGETS_ACHIEVED"
                                            .equals(
                                                    e.getEventType()))
                            .filter(e -> team.getCaptainName()
                                    .equals(
                                            e.getCaptainName()))
                            .count());

            standing.setBountiesRevealed(

                    events.stream()
                            .filter(e -> "BOUNTY"
                                    .equals(
                                            e.getEventType())
                                    ||
                                    "GOLDEN_BOUNTY"
                                            .equals(
                                                    e.getEventType()))
                            .filter(e -> team.getCaptainName()
                                    .equals(
                                            e.getCaptainName()))
                            .count());

            

            standings.add(
                    standing);
        }
        standings.sort(
                (a, b) -> Integer.compare(
                        b.getPurse(),
                        a.getPurse()));

        return standings;
    }
}