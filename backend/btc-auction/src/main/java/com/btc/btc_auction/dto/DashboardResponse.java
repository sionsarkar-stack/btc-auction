package com.btc.btc_auction.dto;

import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.Team;

import java.util.List;

public class DashboardResponse {

    private Auction currentAuction;
    private List<Team> teams;

    public DashboardResponse(Auction currentAuction,
                             List<Team> teams) {
        this.currentAuction = currentAuction;
        this.teams = teams;
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public List<Team> getTeams() {
        return teams;
    }
}