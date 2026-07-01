package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tribunal_votes")
public class TribunalVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who is casting the vote
    private String votingCaptain;

    // Against whom
    private String targetCaptain;

    // Player selected
    private String playerName;

    public Long getId() {
        return id;
    }

    public String getVotingCaptain() {
        return votingCaptain;
    }

    public void setVotingCaptain(String votingCaptain) {
        this.votingCaptain = votingCaptain;
    }

    public String getTargetCaptain() {
        return targetCaptain;
    }

    public void setTargetCaptain(String targetCaptain) {
        this.targetCaptain = targetCaptain;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}