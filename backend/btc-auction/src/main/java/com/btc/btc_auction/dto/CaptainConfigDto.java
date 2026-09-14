package com.btc.btc_auction.dto;

public class CaptainConfigDto {

    private final String captainName;
    private final String currentName;
    private final int totalPoints;

    public CaptainConfigDto(String currentName, String captainName, int totalPoints) {
        this.currentName = currentName;
        this.captainName = captainName;
        this.totalPoints = totalPoints;
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
}