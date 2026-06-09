package com.btc.btc_auction.model;

public class Player {

    private String name;
    private String seed;
    private boolean sold;
    private int soldPrice;
    private String team;

    public Player(String name,
            String seed,
            boolean sold,
            int soldPrice,
            String team) {

        this.name = name;
        this.seed = seed;
        this.sold = sold;
        this.soldPrice = soldPrice;
        this.team = team;
    }

    public String getName() {
        return name;
    }

    public String getSeed() {
        return seed;
    }

    public boolean isSold() {
        return sold;
    }

    public int getSoldPrice() {
        return soldPrice;
    }

    public String getTeam() {
        return team;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public void setSoldPrice(int soldPrice) {
        this.soldPrice = soldPrice;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}