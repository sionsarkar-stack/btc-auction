package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "silent_bids")
public class SilentBidEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;

    private String captainName;

    private Integer bidAmount;

    private boolean submitted;

    private boolean eligibleForTieBreak = true;

    private boolean tieBreakRound;

    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public Integer getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Integer bidAmount) {
        this.bidAmount = bidAmount;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public boolean isEligibleForTieBreak() {
        return eligibleForTieBreak;
    }

    public void setEligibleForTieBreak(boolean eligibleForTieBreak) {
        this.eligibleForTieBreak = eligibleForTieBreak;
    }

    public boolean isTieBreakRound() {
        return tieBreakRound;
    }

    public void setTieBreakRound(boolean tieBreakRound) {
        this.tieBreakRound = tieBreakRound;
    }
}
