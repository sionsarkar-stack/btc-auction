package com.btc.btc_auction.model;

public class BountyConfigRequest {

    private String playerOne;
    private String playerTwo;
    private String playerThree;
    private String playerFour;

    private String goldenPlayer;

    public String getPlayerOne() {
        return playerOne;
    }

    public void setPlayerOne(
            String playerOne) {

        this.playerOne = playerOne;
    }

    public String getPlayerTwo() {
        return playerTwo;
    }

    public void setPlayerTwo(
            String playerTwo) {

        this.playerTwo = playerTwo;
    }

    public String getPlayerThree() {
        return playerThree;
    }

    public void setPlayerThree(
            String playerThree) {

        this.playerThree = playerThree;
    }

    public String getPlayerFour() {
        return playerFour;
    }

    public void setPlayerFour(
            String playerFour) {

        this.playerFour = playerFour;
    }

    public String getGoldenPlayer() {
        return goldenPlayer;
    }

    public void setGoldenPlayer(
            String goldenPlayer) {

        this.goldenPlayer = goldenPlayer;
    }
}