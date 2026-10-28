package com.btc.btc_auction.entity;

import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.enums.RandomEventType;

import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private int bidTimerSeconds = 10;

    private boolean auctionStarted;

    private boolean curseSelectionEnabled;

    private int blindBonanzaAwardsUsed;

    private int auctionRound = 1;

    private String rtmLockdownPlayer;

    private String marketAdjustmentPlayer;

    private Integer marketAdjustment;

    private String pendingSilentAuctionPlayer;

    private String pendingRandomEventType;

    private String pendingRandomEventPlayer;

    private String pendingRandomEventTitle;

    private String pendingRandomEventDescription;

    private Integer pendingRandomEventAmount;

    private boolean marketCrashApplied;

    private Long wheelSpinStartedAt;

    private Long wheelSpinEndsAt;

    private String nextNominationCaptain;

    private Long bidDeadlineAt;

    private Long auctionBreakEndsAt;

    private int auctionsSinceLastBreak;

    private Long auctionTimeoutEndsAt;

    private Long pausedBidTimeRemainingMillis;

    private String timeoutCaptainName;

    @ElementCollection
    @CollectionTable(name = "auction_passed_captains", joinColumns = @JoinColumn(name = "auction_config_id"))
    @Column(name = "captain_name")
    private Set<String> passedCaptains = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "auction_timeout_usage", joinColumns = @JoinColumn(name = "auction_config_id"))
    @MapKeyColumn(name = "captain_name")
    @Column(name = "timeouts_used")
    private Map<String, Integer> timeoutUsage = new HashMap<>();

    @Transient
    private Long serverTime;

    @Transient
    private List<String> walkoutPlayerNames;

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

    public int getBidTimerSeconds() {
        return bidTimerSeconds < 1 ? 10 : bidTimerSeconds;
    }

    public void setBidTimerSeconds(int bidTimerSeconds) {
        this.bidTimerSeconds = Math.max(1, Math.min(120, bidTimerSeconds));
    }

    public boolean isAuctionStarted() {
        return auctionStarted;
    }

    public void setAuctionStarted(boolean auctionStarted) {
        this.auctionStarted = auctionStarted;
    }

    public boolean isCurseSelectionEnabled() {
        return curseSelectionEnabled;
    }

    public void setCurseSelectionEnabled(boolean curseSelectionEnabled) {
        this.curseSelectionEnabled = curseSelectionEnabled;
    }

    public int getBlindBonanzaAwardsUsed() {
        return blindBonanzaAwardsUsed;
    }

    public void setBlindBonanzaAwardsUsed(int blindBonanzaAwardsUsed) {
        this.blindBonanzaAwardsUsed = blindBonanzaAwardsUsed;
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

    public String getNextNominationCaptain() {
        return nextNominationCaptain;
    }

    public void setNextNominationCaptain(String nextNominationCaptain) {
        this.nextNominationCaptain = nextNominationCaptain;
    }

    public Long getBidDeadlineAt() {
        return bidDeadlineAt;
    }

    public void setBidDeadlineAt(Long bidDeadlineAt) {
        this.bidDeadlineAt = bidDeadlineAt;
    }

    public Long getAuctionBreakEndsAt() {
        return auctionBreakEndsAt;
    }

    public void setAuctionBreakEndsAt(Long auctionBreakEndsAt) {
        this.auctionBreakEndsAt = auctionBreakEndsAt;
    }

    public int getAuctionsSinceLastBreak() {
        return auctionsSinceLastBreak;
    }

    public void setAuctionsSinceLastBreak(int auctionsSinceLastBreak) {
        this.auctionsSinceLastBreak = Math.max(0, auctionsSinceLastBreak);
    }

    public Long getAuctionTimeoutEndsAt() {
        return auctionTimeoutEndsAt;
    }

    public void setAuctionTimeoutEndsAt(Long auctionTimeoutEndsAt) {
        this.auctionTimeoutEndsAt = auctionTimeoutEndsAt;
    }

    public Long getPausedBidTimeRemainingMillis() {
        return pausedBidTimeRemainingMillis;
    }

    public void setPausedBidTimeRemainingMillis(Long pausedBidTimeRemainingMillis) {
        this.pausedBidTimeRemainingMillis = pausedBidTimeRemainingMillis;
    }

    public String getTimeoutCaptainName() {
        return timeoutCaptainName;
    }

    public void setTimeoutCaptainName(String timeoutCaptainName) {
        this.timeoutCaptainName = timeoutCaptainName;
    }

    public Set<String> getPassedCaptains() {
        if (passedCaptains == null) {
            passedCaptains = new HashSet<>();
        }
        return passedCaptains;
    }

    public void setPassedCaptains(Set<String> passedCaptains) {
        this.passedCaptains = passedCaptains == null ? new HashSet<>() : new HashSet<>(passedCaptains);
    }

    public Map<String, Integer> getTimeoutUsage() {
        if (timeoutUsage == null) {
            timeoutUsage = new HashMap<>();
        }
        return timeoutUsage;
    }

    public void setTimeoutUsage(Map<String, Integer> timeoutUsage) {
        this.timeoutUsage = timeoutUsage == null ? new HashMap<>() : new HashMap<>(timeoutUsage);
    }

    public Long getServerTime() {
        return serverTime;
    }

    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }

    public List<String> getWalkoutPlayerNames() {
        return walkoutPlayerNames;
    }

    public void setWalkoutPlayerNames(List<String> walkoutPlayerNames) {
        this.walkoutPlayerNames = walkoutPlayerNames;
    }

    public AuctionPhase getAuctionPhase() {
        return auctionPhase;
    }

    public void setAuctionPhase(

            AuctionPhase auctionPhase) {
        this.auctionPhase = auctionPhase;
    }
}
