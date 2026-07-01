package com.btc.btc_auction.model;

import com.btc.btc_auction.enums.JokerType;

public class JokerUseRequest {

    private String captainName;

    private JokerType jokerType;

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
}