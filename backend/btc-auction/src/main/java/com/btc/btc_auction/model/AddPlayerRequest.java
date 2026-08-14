package com.btc.btc_auction.model;

public class AddPlayerRequest {

    private String name;
    private String seed;
    private Integer basePrice;

    public String getName() {
        return name;
    }

    public String getSeed() {
        return seed;
    }

    public Integer getBasePrice() {
        return basePrice;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public void setBasePrice(Integer basePrice) {
        this.basePrice = basePrice;
    }
}
