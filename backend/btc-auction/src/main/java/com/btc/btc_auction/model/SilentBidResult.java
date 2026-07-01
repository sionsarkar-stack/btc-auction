package com.btc.btc_auction.model;

import com.btc.btc_auction.entity.SilentBidEntity;

import java.util.List;

public class SilentBidResult {

    private boolean tie;

    private SilentBidEntity winner;

    private List<String> tiedCaptains;

    public boolean isTie() {
        return tie;
    }

    public void setTie(boolean tie) {
        this.tie = tie;
    }

    public SilentBidEntity getWinner() {
        return winner;
    }

    public void setWinner(SilentBidEntity winner) {
        this.winner = winner;
    }

    public List<String> getTiedCaptains() {
        return tiedCaptains;
    }

    public void setTiedCaptains(List<String> tiedCaptains) {
        this.tiedCaptains = tiedCaptains;
    }

}