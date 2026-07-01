package com.btc.btc_auction.model;

public class TribunalVoteRequest {

    private String votingCaptain;

    private String targetCaptain;

    private String playerName;

    public String getVotingCaptain() {
        return votingCaptain;
    }

    public void setVotingCaptain(
            String votingCaptain) {

        this.votingCaptain = votingCaptain;
    }

    public String getTargetCaptain() {
        return targetCaptain;
    }

    public void setTargetCaptain(
            String targetCaptain) {

        this.targetCaptain = targetCaptain;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(
            String playerName) {

        this.playerName = playerName;
    }

}