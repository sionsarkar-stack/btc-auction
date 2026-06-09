package com.btc.btc_auction.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private String captainName;
    private int purse;
    private int playersBought;
    private int playersLeft;
    private List<String> squad;

    public Team(String captainName,
            int purse,
            int playersBought,
            int playersLeft) {

        this.captainName = captainName;
        this.purse = purse;
        this.playersBought = playersBought;
        this.playersLeft = playersLeft;
        this.squad = new ArrayList<>();
    }

    // Getters

    public String getCaptainName() {
        return captainName;
    }

    public int getPurse() {
        return purse;
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

    // Setters

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public void setPurse(int purse) {
        this.purse = purse;
    }

    public void setPlayersBought(int playersBought) {
        this.playersBought = playersBought;
    }

    public void setPlayersLeft(int playersLeft) {
        this.playersLeft = playersLeft;
    }

    public void setSquad(List<String> squad) {
        this.squad = squad;
    }

    // BTC Calculations

    public int getMaxZBid() {
        return purse - (200 * (playersLeft - 1));
    }

    public int getMaxABCBid() {
        return purse - (100 * (playersLeft - 1));
    }
}