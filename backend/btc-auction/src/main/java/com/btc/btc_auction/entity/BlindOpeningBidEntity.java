package com.btc.btc_auction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blind_opening_bids")
public class BlindOpeningBidEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;

    private String captainName;

    private Integer bidAmount;

    private boolean submitted;

    private boolean passed;

    private boolean bonusEligible;

    private boolean bonusAwarded;

    private int bonusAwardedAmount;

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

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isBonusEligible() {
        return bonusEligible;
    }

    public void setBonusEligible(boolean bonusEligible) {
        this.bonusEligible = bonusEligible;
    }

    public boolean isBonusAwarded() {
        return bonusAwarded;
    }

    public void setBonusAwarded(boolean bonusAwarded) {
        this.bonusAwarded = bonusAwarded;
    }

    public int getBonusAwardedAmount() {
        return bonusAwardedAmount;
    }

    public void setBonusAwardedAmount(int bonusAwardedAmount) {
        this.bonusAwardedAmount = bonusAwardedAmount;
    }
}
