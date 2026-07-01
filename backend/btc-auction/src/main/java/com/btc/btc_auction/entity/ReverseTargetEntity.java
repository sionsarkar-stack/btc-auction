package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reverse_targets")
public class ReverseTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    private String rivalCaptain;

    private String playerName;

    public Long getId() {
        return id;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getRivalCaptain() {
        return rivalCaptain;
    }

    public void setRivalCaptain(String rivalCaptain) {
        this.rivalCaptain = rivalCaptain;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}