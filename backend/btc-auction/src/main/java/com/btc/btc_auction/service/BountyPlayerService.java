package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BountyPlayerEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.BountyPlayerRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class BountyPlayerService {

    private final BountyPlayerRepository repository;
    private final PlayerService playerService;

    public BountyPlayerService(
            BountyPlayerRepository repository,
            PlayerService playerService) {

        this.repository = repository;
        this.playerService = playerService;
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

    public boolean randomizeBounties() {
        List<PlayerEntity> candidates = new ArrayList<>(playerService.getUnsoldPlayers());

        if (candidates.size() < 6) {
            return false;
        }

        Collections.shuffle(candidates);
        deleteAll();

        for (int index = 0; index < 6; index++) {
            BountyPlayerEntity bounty = new BountyPlayerEntity();
            bounty.setPlayerName(candidates.get(index).getName());
            bounty.setGolden(index >= 4);
            bounty.setRevealed(false);
            save(bounty);
        }

        return true;
    }
}