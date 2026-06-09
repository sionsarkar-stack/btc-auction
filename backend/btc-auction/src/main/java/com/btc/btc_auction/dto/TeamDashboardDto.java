package com.btc.btc_auction.dto;

import java.util.List;

public class TeamDashboardDto {

    private String captainName;
    private int purse;
    private int playersBought;
    private int playersLeft;
    private int maxZBid;
    private int maxABCBid;
    private List<String> squad;

    public TeamDashboardDto(
            String captainName,
            int purse,
            int playersBought,
            int playersLeft,
            int maxZBid,
            int maxABCBid,
            List<String> squad) {

        this.captainName = captainName;
        this.purse = purse;
        this.playersBought = playersBought;
        this.playersLeft = playersLeft;
        this.maxZBid = maxZBid;
        this.maxABCBid = maxABCBid;
        this.squad = squad;
    }

    public String getCaptainName() {
        return captainName;
    }

    public int getPurse() {
        return purse;
    }

    public int getPlayersBought() {
        return playersBought;
    }

    public int getPlayersLeft() {
        return playersLeft;
    }

    public int getMaxZBid() {
        return maxZBid;
    }

    public int getMaxABCBid() {
        return maxABCBid;
    }

    public List<String> getSquad() {
        return squad;
    }
}