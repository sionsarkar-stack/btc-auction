package com.btc.btc_auction.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trusted_captains")
public class TrustedCaptainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String captainName;

    private String trustedCaptain;

    public Long getId() {
        return id;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getTrustedCaptain() {
        return trustedCaptain;
    }

    public void setTrustedCaptain(String trustedCaptain) {
        this.trustedCaptain = trustedCaptain;
    }
}