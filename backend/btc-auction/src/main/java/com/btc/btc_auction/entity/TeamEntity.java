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

    private int playersBought;

    private int playersLeft;

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
}