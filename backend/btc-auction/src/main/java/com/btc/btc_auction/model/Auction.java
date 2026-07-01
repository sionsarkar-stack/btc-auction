package com.btc.btc_auction.model;

public class Auction {

    private String currentPlayer;
    private String seed;
    private int currentBid;
    private String leader;
    private int basePrice;
    private String nominatedBy;

    public Auction(String currentPlayer,
            String seed,
            int currentBid,
            String leader,
            int basePrice,
            String nominatedBy) {

        this.currentPlayer = currentPlayer;
        this.seed = seed;
        this.currentBid = currentBid;
        this.leader = leader;
        this.nominatedBy = nominatedBy;
        this.basePrice = basePrice;

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

    public int getBasePrice() {
        return basePrice;
    }

    public String getNominatedBy() {
        return nominatedBy;
    }

    public void setCurrentPlayer(
            String currentPlayer) {

        this.currentPlayer = currentPlayer;

    }

    public void setSeed(
            String seed) {

        this.seed = seed;

    }

    public void setCurrentBid(
            int currentBid) {

        this.currentBid = currentBid;

    }

    public void setLeader(
            String leader) {

        this.leader = leader;

    }

    public void setBasePrice(
            int basePrice) {

        this.basePrice = basePrice;

    }

    public void setNominatedBy(
            String nominatedBy) {

        this.nominatedBy = nominatedBy;

    }
}