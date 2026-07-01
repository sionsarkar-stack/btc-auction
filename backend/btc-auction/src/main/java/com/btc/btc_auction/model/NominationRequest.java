package com.btc.btc_auction.model;

public class NominationRequest {

    private String playerName;
    private String seed;
    private String captainName;

    public String getPlayerName() {
        return playerName;
    }

    public String getSeed() {
        return seed;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }
}