package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "auction_logs")
public class AuctionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;

    private String captainName;

    private int soldPrice;

    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public int getSoldPrice() {
        return soldPrice;
    }

    public void setSoldPrice(int soldPrice) {
        this.soldPrice = soldPrice;
    }
}