package com.btc.btc_auction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rtm")
public class RtmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    private String playerName;

    private int bidAmount;

    private LocalDateTime claimedAt;

    private boolean used;

    private Integer rtmBid;

    private String originalCaptain;

    private String status;

    private Integer originalBidAmount;

    public Long getId() {
        return id;
    }

    public Integer getRtmBid() {
        return rtmBid;
    }

    public void setRtmBid(Integer rtmBid) {
        this.rtmBid = rtmBid;
    }

    public String getOriginalCaptain() {
        return originalCaptain;
    }

    public void setOriginalCaptain(String originalCaptain) {
        this.originalCaptain = originalCaptain;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(int bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Integer getOriginalBidAmount() {
        return originalBidAmount;
    }

    public void setOriginalBidAmount(Integer originalBidAmount) {
        this.originalBidAmount = originalBidAmount;
    }
}