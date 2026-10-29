package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    private int purse;

    private int tournamentPoints;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer effectivePriceCredit;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer curseRewardCredit;

    private int playersBought;

    private int playersLeft;

    private Boolean wildPickUsed = false;

    public TeamEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public int getPurse() {
        return purse;
    }

    public void setPurse(int purse) {
        this.purse = purse;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getTournamentPoints() {
        return tournamentPoints;
    }

    public void setTournamentPoints(int tournamentPoints) {
        this.tournamentPoints = tournamentPoints;
    }

    public int getEffectivePriceCredit() {
        return effectivePriceCredit == null ? 0 : effectivePriceCredit;
    }

    public void setEffectivePriceCredit(int effectivePriceCredit) {
        this.effectivePriceCredit = effectivePriceCredit;
    }

    public int getCurseRewardCredit() {
        return curseRewardCredit == null ? 0 : curseRewardCredit;
    }

    public void setCurseRewardCredit(int curseRewardCredit) {
        this.curseRewardCredit = curseRewardCredit;
    }

    public int getPlayersBought() {
        return playersBought;
    }

    public void setPlayersBought(int playersBought) {
        this.playersBought = playersBought;
    }

    public int getPlayersLeft() {
        return playersLeft;
    }

    public void setPlayersLeft(int playersLeft) {
        this.playersLeft = playersLeft;
    }

    public boolean isWildPickUsed() {
        return Boolean.TRUE.equals(wildPickUsed);
    }

    public void setWildPickUsed(boolean wildPickUsed) {
        this.wildPickUsed = wildPickUsed;
    }
}