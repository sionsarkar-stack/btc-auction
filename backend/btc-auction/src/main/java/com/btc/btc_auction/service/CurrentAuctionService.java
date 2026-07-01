package com.btc.btc_auction.service;

import com.btc.btc_auction.model.Auction;
import org.springframework.stereotype.Service;

@Service
public class CurrentAuctionService {

    private Auction currentAuction = new Auction(
            "",
            "",
            0,
            "None",
            0,
            "");

    public Auction getCurrentAuction() {

        return currentAuction;

    }

    public void setCurrentAuction(
            Auction currentAuction) {

        this.currentAuction = currentAuction;

    }

}