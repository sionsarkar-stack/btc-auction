package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.repository.AuctionConfigRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class AuctionConfigService {

    private final AuctionConfigRepository repository;

    public AuctionConfigService(
            AuctionConfigRepository repository) {

        this.repository = repository;
    }

    public AuctionConfigEntity getConfig() {

        return repository
                .findAll()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public AuctionConfigEntity save(
            @NonNull AuctionConfigEntity config) {

        return repository.save(config);
    }

    @PostConstruct
    public void initializeDefaultConfig() {

        if (repository.count() > 0) {
            return;
        }

        AuctionConfigEntity config = new AuctionConfigEntity();

        config.setSeasonName(
                "BTC Season 11");

        // The captain occupies one of the ten squad places, leaving nine purchases.
        config.setSquadSize(10);

        config.setTargetBonus(150);

        config.setTargetCompletionBonus(250);

        config.setTargetMissPenalty(100);

        config.setBountyBonus(100);

        config.setGoldenBountyBonus(200);

        config.setStealPenalty(200);

        config.setAuctionStarted(false);

        repository.save(config);
    }
}
