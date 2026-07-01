package com.btc.btc_auction.entity;

import com.btc.btc_auction.enums.AuctionPhase;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auction_config")
public class AuctionConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seasonName;

    private boolean showSpecialFeatures = true;

    private int squadSize;

    private int targetBonus;

    private int targetCompletionBonus;

    private int bountyBonus;

    private int goldenBountyBonus;

    private int stealPenalty;

    private boolean tribunalLocked;

    private boolean auctionStarted;

    @Enumerated(EnumType.STRING)
    private AuctionPhase auctionPhase = AuctionPhase.NO_AUCTION;

    public Long getId() {
        return id;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }

    public boolean isShowSpecialFeatures() {
        return showSpecialFeatures;
    }

    public void setShowSpecialFeatures(boolean showSpecialFeatures) {
        this.showSpecialFeatures = showSpecialFeatures;
    }

    public int getSquadSize() {
        return squadSize;
    }

    public void setSquadSize(int squadSize) {
        this.squadSize = squadSize;
    }

    public int getTargetBonus() {
        return targetBonus;
    }

    public void setTargetBonus(int targetBonus) {
        this.targetBonus = targetBonus;
    }

    public int getTargetCompletionBonus() {
        return targetCompletionBonus;
    }

    public void setTargetCompletionBonus(
            int targetCompletionBonus) {
        this.targetCompletionBonus = targetCompletionBonus;
    }

    public int getBountyBonus() {
        return bountyBonus;
    }

    public void setBountyBonus(int bountyBonus) {
        this.bountyBonus = bountyBonus;
    }

    public int getGoldenBountyBonus() {
        return goldenBountyBonus;
    }

    public void setGoldenBountyBonus(
            int goldenBountyBonus) {
        this.goldenBountyBonus = goldenBountyBonus;
    }

    public int getStealPenalty() {
        return stealPenalty;
    }

    public void setStealPenalty(int stealPenalty) {
        this.stealPenalty = stealPenalty;
    }

    public boolean isTribunalLocked() {
        return tribunalLocked;
    }

    public void setTribunalLocked(boolean tribunalLocked) {
        this.tribunalLocked = tribunalLocked;
    }

    public boolean isAuctionStarted() {
        return auctionStarted;
    }

    public void setAuctionStarted(boolean auctionStarted) {
        this.auctionStarted = auctionStarted;
    }

    public AuctionPhase getAuctionPhase() {
        return auctionPhase;
    }

    public void setAuctionPhase(

            AuctionPhase auctionPhase) {
        this.auctionPhase = auctionPhase;
    }
}