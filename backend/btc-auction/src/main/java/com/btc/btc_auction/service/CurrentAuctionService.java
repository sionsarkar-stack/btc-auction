package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.CurrentAuctionEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.CurrentAuctionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CurrentAuctionService {

    private static final long CURRENT_AUCTION_ID = 1L;

    private final CurrentAuctionRepository repository;

    private Auction currentAuction = emptyAuction();

    public CurrentAuctionService() {
        this.repository = null;
    }

    @Autowired
    public CurrentAuctionService(CurrentAuctionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void loadPersistedAuction() {
        if (repository == null) {
            return;
        }

        currentAuction = repository.findById(CURRENT_AUCTION_ID)
                .map(this::toAuction)
                .orElseGet(CurrentAuctionService::emptyAuction);
    }

    public synchronized Auction getCurrentAuction() {

        return currentAuction;

    }

    public synchronized void setCurrentAuction(
            Auction currentAuction) {

        this.currentAuction = currentAuction == null ? emptyAuction() : currentAuction;
        saveCurrentAuction();

    }

    public synchronized void saveCurrentAuction() {
        if (repository == null) {
            return;
        }

        repository.save(toEntity(currentAuction));

    }

    private @NonNull CurrentAuctionEntity toEntity(Auction auction) {
        CurrentAuctionEntity entity = new CurrentAuctionEntity();
        entity.setId(CURRENT_AUCTION_ID);
        entity.setCurrentPlayer(defaultString(auction.getCurrentPlayer()));
        entity.setSeed(defaultString(auction.getSeed()));
        entity.setCurrentBid(auction.getCurrentBid());
        entity.setLeader(defaultString(auction.getLeader()));
        entity.setBasePrice(auction.getBasePrice());
        return entity;

    }

    private Auction toAuction(CurrentAuctionEntity entity) {
        return new Auction(
                defaultString(entity.getCurrentPlayer()),
                defaultString(entity.getSeed()),
                entity.getCurrentBid(),
                defaultString(entity.getLeader()),
                entity.getBasePrice());

    }

    private static Auction emptyAuction() {
        return new Auction("", "", 0, "None", 0, "");

    }

    private static String defaultString(String value) {
        return value == null ? "" : value;

    }

}