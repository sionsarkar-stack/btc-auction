package com.btc.btc_auction.model;

import java.util.List;

public class TribunalCaptainStatus {

    private String captainName;

    private boolean submitted;

    private String trustedCaptain;

    private List<TribunalVoteRequest> votes;

    public TribunalCaptainStatus() {
    }

    public TribunalCaptainStatus(
            String captainName,
            boolean submitted,
            String trustedCaptain,
            List<TribunalVoteRequest> votes) {

        this.captainName = captainName;
        this.submitted = submitted;
        this.trustedCaptain = trustedCaptain;
        this.votes = votes;
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

    public List<TribunalVoteRequest> getVotes() {
        return votes;
    }

    public void setVotes(List<TribunalVoteRequest> votes) {
        this.votes = votes;
    }
}