package com.btc.btc_auction.dto;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;

import java.util.List;

public class DashboardResponse {

    private Auction currentAuction;
    private List<TeamDashboardDto> teams;

    public DashboardResponse(Auction currentAuction,
            List<TeamDashboardDto> teams) {
        this.currentAuction = currentAuction;
        this.teams = teams;
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public List<TeamDashboardDto> getTeams() {
        return teams;
    }
}