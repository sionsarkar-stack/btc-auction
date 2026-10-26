package com.btc.btc_auction.dto;

public class CaptainConfigDto {

    private final String captainName;
    private final String currentName;
    private final int totalPoints;
    private final int tournamentPoints;

    public CaptainConfigDto(String currentName, String captainName, int totalPoints, int tournamentPoints) {
        this.currentName = currentName;
        this.captainName = captainName;
        this.totalPoints = totalPoints;
        this.tournamentPoints = tournamentPoints;
    }

    public String getCurrentName() {
        return currentName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getTournamentPoints() {
        return tournamentPoints;
    }
}