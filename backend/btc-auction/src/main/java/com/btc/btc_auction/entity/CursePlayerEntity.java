package com.btc.btc_auction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "curse_players")
public class CursePlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    private String playerName;

    private boolean triggered;

    private String settledBuyer;

    private int settledAmount;

    private int buyerDeduction;

    public Long getId() {
        return id;
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

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public String getSettledBuyer() {
        return settledBuyer;
    }

    public void setSettledBuyer(String settledBuyer) {
        this.settledBuyer = settledBuyer;
    }

    public int getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(int settledAmount) {
        this.settledAmount = settledAmount;
    }

    public int getBuyerDeduction() {
        return buyerDeduction;
    }

    public void setBuyerDeduction(int buyerDeduction) {
        this.buyerDeduction = buyerDeduction;
    }
}