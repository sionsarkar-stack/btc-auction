package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<PlayerEntity> getAllPlayers() {
        return playerRepository.findAll();
    }

    public List<PlayerEntity> getUnsoldPlayers() {
        return playerRepository.findAll()
                .stream()
                .filter(player -> !player.isSold())
                .toList();
    }

    public PlayerEntity getPlayer(String name) {
        return playerRepository
                .findByName(name)
                .orElse(null);
    }

    public void addPlayer(String name, String seed) {

        PlayerEntity player = new PlayerEntity();

        player.setName(name);
        player.setSeed(seed);
        player.setSold(false);
        player.setSoldPrice(0);
        player.setTeam("");

        playerRepository.save(player);
    }

    public void savePlayer(PlayerEntity player) {
        playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }
}