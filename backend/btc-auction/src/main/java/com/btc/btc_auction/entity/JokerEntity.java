package com.btc.btc_auction.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.btc.btc_auction.enums.JokerType;

@Entity
@Table(name = "jokers")
public class JokerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    @Enumerated(EnumType.STRING)
    private JokerType jokerType;

    private boolean used;

    private LocalDateTime usedAt;

    public Long getId() {
        return id;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public JokerType getJokerType() {
        return jokerType;
    }

    public void setJokerType(JokerType jokerType) {
        this.jokerType = jokerType;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}