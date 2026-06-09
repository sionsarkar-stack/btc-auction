package com.btc.btc_auction.model;

public class NominationRequest {

    private String playerName;
    private String seed;

    public String getPlayerName() {
        return playerName;
    }

    public String getSeed() {
        return seed;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }
}