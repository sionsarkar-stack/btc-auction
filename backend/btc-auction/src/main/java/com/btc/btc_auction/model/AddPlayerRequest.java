package com.btc.btc_auction.model;

public class AddPlayerRequest {

    private String name;
    private String seed;

    public String getName() {
        return name;
    }

    public String getSeed() {
        return seed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }
}