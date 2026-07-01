package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.PlayerRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    public void savePlayer(@NonNull PlayerEntity player) {
        playerRepository.save(player);
    }

    public void deletePlayer(@NonNull Long id) {
        playerRepository.deleteById(id);
    }

    public void importCsv(
            MultipartFile file)
            throws Exception {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        file.getInputStream()));

        String line;

        boolean header = true;

        while ((line = reader.readLine()) != null) {

            if (header) {

                header = false;
                continue;
            }

            String[] parts = line.split(",");

            if (parts.length < 2) {
                continue;
            }

            String name = parts[0].trim();

            String seed = parts[1].trim();

            if (getPlayer(name) != null) {
                continue;
            }

            addPlayer(
                    name,
                    seed);
        }
    }
}