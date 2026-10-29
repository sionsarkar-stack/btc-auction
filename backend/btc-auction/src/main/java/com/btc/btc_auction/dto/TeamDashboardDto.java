package com.btc.btc_auction.dto;

import java.util.List;

public class TeamDashboardDto {

    private String captainName;
    private int purse;
    private int tournamentPoints;
    private int playersBought;
    private int playersLeft;
    private List<String> squad;
    private boolean rtmAvailable;

    public TeamDashboardDto(
            String captainName,
            int purse,
            int tournamentPoints,
            int playersBought,
            int playersLeft,
            List<String> squad,
            boolean rtmAvailable) {

        this.captainName = captainName;
        this.purse = purse;
        this.tournamentPoints = tournamentPoints;
        this.playersBought = playersBought;
        this.playersLeft = playersLeft;
        this.squad = squad;
        this.rtmAvailable = rtmAvailable;
    }

    public String getCaptainName() {
        return captainName;
    }

    public int getPurse() {
        return purse;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getTournamentPoints() {
        return tournamentPoints;
    }

    public int getPlayersBought() {
        return playersBought;
    }

    public int getPlayersLeft() {
        return playersLeft;
    }

    public List<String> getSquad() {
        return squad;
    }

    public boolean isRtmAvailable() {
        return rtmAvailable;
    }
}