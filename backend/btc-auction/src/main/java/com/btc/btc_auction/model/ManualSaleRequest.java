package com.btc.btc_auction.model;

public class ManualSaleRequest {

    private String playerName;
    private String newCaptain;
    private int newPrice;
    private String reason;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getNewCaptain() {
        return newCaptain;
    }

    public void setNewCaptain(String newCaptain) {
        this.newCaptain = newCaptain;
    }

    public int getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(int newPrice) {
        this.newPrice = newPrice;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}