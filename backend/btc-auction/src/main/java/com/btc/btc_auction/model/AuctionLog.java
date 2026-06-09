package com.btc.btc_auction.model;

public class AuctionLog {

    private String playerName;
    private String captainName;
    private int soldPrice;

    public AuctionLog(String playerName,
            String captainName,
            int soldPrice) {

        this.playerName = playerName;
        this.captainName = captainName;
        this.soldPrice = soldPrice;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public int getSoldPrice() {
        return soldPrice;
    }
}