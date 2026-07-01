package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bounty_players")
public class BountyPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;

    private boolean golden;

    private boolean revealed;

    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(
            String playerName) {

        this.playerName = playerName;
    }

    public boolean isGolden() {
        return golden;
    }

    public void setGolden(
            boolean golden) {

        this.golden = golden;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(
            boolean revealed) {

        this.revealed = revealed;
    }
}