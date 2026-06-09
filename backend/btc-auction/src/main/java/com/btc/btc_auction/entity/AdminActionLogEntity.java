package com.btc.btc_auction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_action_logs")
public class AdminActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actionType;

    private String playerName;

    private String oldCaptain;

    private String newCaptain;

    private int oldPrice;

    private int newPrice;

    private String reason;

    private LocalDateTime timestamp;

    public AdminActionLogEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getOldCaptain() {
        return oldCaptain;
    }

    public void setOldCaptain(String oldCaptain) {
        this.oldCaptain = oldCaptain;
    }

    public String getNewCaptain() {
        return newCaptain;
    }

    public void setNewCaptain(String newCaptain) {
        this.newCaptain = newCaptain;
    }

    public int getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(int oldPrice) {
        this.oldPrice = oldPrice;
    }

    public int getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(int newPrice) {
        this.newPrice = newPrice;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}