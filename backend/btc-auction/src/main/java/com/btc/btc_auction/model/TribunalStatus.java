package com.btc.btc_auction.model;

public class TribunalStatus {

    private String captainName;

    private boolean submitted;

    private String trustedCaptain;

    public TribunalStatus() {
    }

    public TribunalStatus(
            String captainName,
            boolean submitted,
            String trustedCaptain) {

        this.captainName = captainName;
        this.submitted = submitted;
        this.trustedCaptain = trustedCaptain;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public String getTrustedCaptain() {
        return trustedCaptain;
    }

    public void setTrustedCaptain(String trustedCaptain) {
        this.trustedCaptain = trustedCaptain;
    }
}