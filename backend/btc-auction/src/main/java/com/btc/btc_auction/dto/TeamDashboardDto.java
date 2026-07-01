package com.btc.btc_auction.dto;

import java.util.List;

public class TeamDashboardDto {

    private String captainName;
    private int purse;
    private int playersBought;
    private int playersLeft;
    private int maxBid;
    private List<String> squad;
    private boolean rtmAvailable;

    public TeamDashboardDto(
            String captainName,
            int purse,
            int playersBought,
            int playersLeft,
            int maxBid,
            List<String> squad,
            boolean rtmAvailable) {

        this.captainName = captainName;
        this.purse = purse;
        this.playersBought = playersBought;
        this.playersLeft = playersLeft;
        this.maxBid = maxBid;
        this.squad = squad;
        this.rtmAvailable = rtmAvailable;
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

    public int getMaxBid() {
        return maxBid;
    }

    public List<String> getSquad() {
        return squad;
    }

    public boolean isRtmAvailable() {
        return rtmAvailable;
    }
}