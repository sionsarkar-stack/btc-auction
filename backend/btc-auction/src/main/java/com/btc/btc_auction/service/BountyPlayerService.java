package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BountyPlayerEntity;
import com.btc.btc_auction.repository.BountyPlayerRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BountyPlayerService {

    private final BountyPlayerRepository repository;

    public BountyPlayerService(
            BountyPlayerRepository repository) {

        this.repository = repository;
    }

    public void save(
            @NonNull BountyPlayerEntity bounty) {

        repository.save(bounty);
    }

    public List<BountyPlayerEntity> getAll() {

        return repository.findAll();
    }

    public BountyPlayerEntity getByPlayer(
            String playerName) {

        return repository
                .findByPlayerName(
                        playerName)
                .orElse(null);
    }

    public void deleteAll() {

        repository.deleteAll();
    }
}