package com.btc.btc_auction.service;

import com.btc.btc_auction.model.Team;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamService {

    private final List<Team> teams = new ArrayList<>();

    public TeamService() {

        teams.add(
                new Team(
                        "Dinda",
                        5000,
                        0,
                        9));

        teams.add(
                new Team(
                        "Boni",
                        6300,
                        0,
                        9));

        teams.add(
                new Team(
                        "Swapneel",
                        6200,
                        0,
                        9));

        teams.add(
                new Team(
                        "Swaswata",
                        6200,
                        0,
                        9));
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Team getTeam(String captainName) {

        return teams.stream()
                .filter(team -> team.getCaptainName().equals(captainName))
                .findFirst()
                .orElse(null);
    }
}