package com.btc.btc_auction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secret_targets")
public class SecretTargetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String captainName;
    private String playerOne;
    private String playerTwo;
    private boolean settled;

    public Long getId() { return id; }
    public String getCaptainName() { return captainName; }
    public void setCaptainName(String captainName) { this.captainName = captainName; }
    public String getPlayerOne() { return playerOne; }
    public void setPlayerOne(String playerOne) { this.playerOne = playerOne; }
    public String getPlayerTwo() { return playerTwo; }
    public void setPlayerTwo(String playerTwo) { this.playerTwo = playerTwo; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
}
