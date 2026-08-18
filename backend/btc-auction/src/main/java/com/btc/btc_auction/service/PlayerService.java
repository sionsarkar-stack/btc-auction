package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.PlayerRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    public void addPlayer(String name, String seed, Integer basePrice) {
        addPlayer(name, seed, basePrice, null);
    }

    public void addPlayer(String name, String seed, Integer basePrice, String category) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name is required.");
        }

        PlayerEntity player = new PlayerEntity();

        player.setName(name);
        player.setSeed(seed);
        player.setCategory(category == null || category.isBlank() ? seed : category.trim());
        // CSV imports remain compatible with the original name,seed format.
        // A third basePrice column is optional and overrides the seed default.
        player.setBasePrice(basePrice != null && basePrice > 0
                ? basePrice
                : getSeedBasePrice(seed));
        player.setSold(false);
        player.setSoldPrice(0);
        player.setFinalPrice(0);
        player.setTeam("");

        playerRepository.save(player);
    }

    public void savePlayer(@NonNull PlayerEntity player) {
        playerRepository.save(player);
    }

    private int getSeedBasePrice(String seed) {
        if (seed == null) {
            return 100;
        }

        return switch (seed.trim().toLowerCase()) {
            case "hackers", "icon", "z" -> 800;
            case "developers", "star", "a" -> 600;
            case "new joiners", "challenger", "b" -> 100;
            case "interns", "pro", "c" -> 50;
            default -> 100;
        };
    }

    public void deletePlayer(@NonNull Long id) {
        playerRepository.deleteById(id);
    }

    public void importCsv(
            MultipartFile file)
            throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {

                if (header) {
                    header = false;
                    continue;
                }

                String[] parts = parseCsvLine(line);

                if (parts.length < 2) {
                    continue;
                }

                String name = parts[0].trim();
                String seed = parts[1].trim();

                if (name.isBlank() || seed.isBlank()) {
                    continue;
                }

                Integer basePrice = null;
                if (parts.length >= 3 && !parts[2].trim().isBlank()) {
                    try {
                        basePrice = Integer.valueOf(parts[2].trim());
                    } catch (NumberFormatException ignored) {
                        basePrice = null;
                    }
                }

                String category = parts.length >= 4 ? parts[3].trim() : seed;

                if (getPlayer(name) != null) {
                    continue;
                }

                addPlayer(name, seed, basePrice, category);
            }
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}
