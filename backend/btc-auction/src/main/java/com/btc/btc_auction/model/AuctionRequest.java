package com.btc.btc_auction.model;

public class AuctionRequest {

    private String playerName;
    private String captainName;
    private int price;

    public String getPlayerName() {
        return playerName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public int getPrice() {
        return price;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}