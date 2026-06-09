package com.btc.btc_auction.model;

public class Auction {

    private String currentPlayer;
    private String seed;
    private int currentBid;
    private String leader;

    public Auction(String currentPlayer,
                   String seed,
                   int currentBid,
                   String leader) {

        this.currentPlayer = currentPlayer;
        this.seed = seed;
        this.currentBid = currentBid;
        this.leader = leader;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public String getSeed() {
        return seed;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public String getLeader() {
        return leader;
    }
}