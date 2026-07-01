package com.btc.btc_auction.model;

public class TeamStanding {

    private String captainName;

    private int purse;

    private int playersBought;

    private long targetsAchieved;

    private long bountiesRevealed;


    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(
            String captainName) {

        this.captainName = captainName;
    }

    public int getPurse() {
        return purse;
    }

    public void setPurse(
            int purse) {

        this.purse = purse;
    }

    public int getPlayersBought() {
        return playersBought;
    }

    public void setPlayersBought(
            int playersBought) {

        this.playersBought = playersBought;
    }

    public long getTargetsAchieved() {
        return targetsAchieved;
    }

    public void setTargetsAchieved(
            long targetsAchieved) {

        this.targetsAchieved = targetsAchieved;
    }

    public long getBountiesRevealed() {
        return bountiesRevealed;
    }

    public void setBountiesRevealed(
            long bountiesRevealed) {

        this.bountiesRevealed = bountiesRevealed;
    }

}