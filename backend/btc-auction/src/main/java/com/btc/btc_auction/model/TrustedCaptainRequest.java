package com.btc.btc_auction.model;

public class TrustedCaptainRequest {

    private String captainName;

    private String trustedCaptain;

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(
            String captainName) {

        this.captainName = captainName;
    }

    public String getTrustedCaptain() {
        return trustedCaptain;
    }

    public void setTrustedCaptain(
            String trustedCaptain) {

        this.trustedCaptain = trustedCaptain;
    }
}