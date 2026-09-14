package com.btc.btc_auction.entity;

import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.enums.RandomEventType;

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

    private int squadSize;

    private int targetBonus;

    private int targetCompletionBonus;

    private Integer targetMissPenalty;

    private int stealPenalty;

    private Integer protectionBonus;

    private Integer protectionPenalty;

    private boolean auctionStarted;

    private boolean protectionSelectionEnabled;

    private int auctionRound = 1;

    private String rtmLockdownPlayer;

    private String marketAdjustmentPlayer;

    private Integer marketAdjustment;

    private String valueBetPlayer;

    private int valueBetEventsUsed;

    private String pendingSilentAuctionPlayer;

    private String pendingRandomEventType;

    private String pendingRandomEventPlayer;

    private String pendingRandomEventTitle;

    private String pendingRandomEventDescription;

    private Integer pendingRandomEventAmount;

    private boolean marketCrashApplied;

    private Long wheelSpinStartedAt;

    private Long wheelSpinEndsAt;

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

    public int getTargetMissPenalty() {
        return targetMissPenalty == null ? 100 : targetMissPenalty;
    }

    public void setTargetMissPenalty(int targetMissPenalty) {
        this.targetMissPenalty = targetMissPenalty;
    }

    public int getStealPenalty() {
        return stealPenalty;
    }

    public void setStealPenalty(int stealPenalty) {
        this.stealPenalty = stealPenalty;
    }

    public int getProtectionBonus() {
        return protectionBonus == null ? 300 : protectionBonus;
    }

    public void setProtectionBonus(int protectionBonus) {
        this.protectionBonus = protectionBonus;
    }

    public int getProtectionPenalty() {
        return protectionPenalty == null ? 200 : protectionPenalty;
    }

    public void setProtectionPenalty(int protectionPenalty) {
        this.protectionPenalty = protectionPenalty;
    }

    public boolean isAuctionStarted() {
        return auctionStarted;
    }

    public void setAuctionStarted(boolean auctionStarted) {
        this.auctionStarted = auctionStarted;
    }

    public boolean isProtectionSelectionEnabled() {
        return protectionSelectionEnabled;
    }

    public void setProtectionSelectionEnabled(boolean protectionSelectionEnabled) {
        this.protectionSelectionEnabled = protectionSelectionEnabled;
    }

    public int getAuctionRound() {
        return auctionRound;
    }

    public void setAuctionRound(int auctionRound) {
        this.auctionRound = auctionRound;
    }

    public String getRtmLockdownPlayer() {
        return rtmLockdownPlayer;
    }

    public void setRtmLockdownPlayer(String rtmLockdownPlayer) {
        this.rtmLockdownPlayer = rtmLockdownPlayer;
    }

    public String getMarketAdjustmentPlayer() {
        return marketAdjustmentPlayer;
    }

    public void setMarketAdjustmentPlayer(String marketAdjustmentPlayer) {
        this.marketAdjustmentPlayer = marketAdjustmentPlayer;
    }

    public int getMarketAdjustment() {
        return marketAdjustment == null ? 0 : marketAdjustment;
    }

    public void setMarketAdjustment(int marketAdjustment) {
        this.marketAdjustment = marketAdjustment;
    }

    public String getValueBetPlayer() {
        return valueBetPlayer;
    }

    public void setValueBetPlayer(String valueBetPlayer) {
        this.valueBetPlayer = valueBetPlayer;
    }

    public int getValueBetEventsUsed() {
        return valueBetEventsUsed;
    }

    public void setValueBetEventsUsed(int valueBetEventsUsed) {
        this.valueBetEventsUsed = valueBetEventsUsed;
    }

    public String getPendingSilentAuctionPlayer() {
        return pendingSilentAuctionPlayer;
    }

    public void setPendingSilentAuctionPlayer(String pendingSilentAuctionPlayer) {
        this.pendingSilentAuctionPlayer = pendingSilentAuctionPlayer;
    }

    public RandomEventType getPendingRandomEventType() {
        if (pendingRandomEventType == null || pendingRandomEventType.isBlank()) {
            return null;
        }

        try {
            return RandomEventType.valueOf(pendingRandomEventType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public void setPendingRandomEventType(RandomEventType pendingRandomEventType) {
        this.pendingRandomEventType = pendingRandomEventType == null ? null : pendingRandomEventType.name();
    }

    public String getPendingRandomEventPlayer() {
        return pendingRandomEventPlayer;
    }

    public void setPendingRandomEventPlayer(String pendingRandomEventPlayer) {
        this.pendingRandomEventPlayer = pendingRandomEventPlayer;
    }

    public String getPendingRandomEventTitle() {
        return pendingRandomEventTitle;
    }

    public void setPendingRandomEventTitle(String pendingRandomEventTitle) {
        this.pendingRandomEventTitle = pendingRandomEventTitle;
    }

    public String getPendingRandomEventDescription() {
        return pendingRandomEventDescription;
    }

    public void setPendingRandomEventDescription(String pendingRandomEventDescription) {
        this.pendingRandomEventDescription = pendingRandomEventDescription;
    }

    public Integer getPendingRandomEventAmount() {
        return pendingRandomEventAmount;
    }

    public void setPendingRandomEventAmount(Integer pendingRandomEventAmount) {
        this.pendingRandomEventAmount = pendingRandomEventAmount;
    }

    public boolean isMarketCrashApplied() {
        return marketCrashApplied;
    }

    public void setMarketCrashApplied(boolean marketCrashApplied) {
        this.marketCrashApplied = marketCrashApplied;
    }

    public Long getWheelSpinStartedAt() {
        return wheelSpinStartedAt;
    }

    public void setWheelSpinStartedAt(Long wheelSpinStartedAt) {
        this.wheelSpinStartedAt = wheelSpinStartedAt;
    }

    public Long getWheelSpinEndsAt() {
        return wheelSpinEndsAt;
    }

    public void setWheelSpinEndsAt(Long wheelSpinEndsAt) {
        this.wheelSpinEndsAt = wheelSpinEndsAt;
    }

    public AuctionPhase getAuctionPhase() {
        return auctionPhase;
    }

    public void setAuctionPhase(

            AuctionPhase auctionPhase) {
        this.auctionPhase = auctionPhase;
    }
}
