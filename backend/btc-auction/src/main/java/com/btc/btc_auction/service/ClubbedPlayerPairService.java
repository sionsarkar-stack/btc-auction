package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.repository.ClubbedPlayerPairRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClubbedPlayerPairService {

    private final ClubbedPlayerPairRepository repository;

    public ClubbedPlayerPairService(ClubbedPlayerPairRepository repository) {
        this.repository = repository;
    }

    public Optional<ClubbedPlayerPairEntity> getActivePair() {
        return repository.findFirstByActiveTrue();
    }

    public List<ClubbedPlayerPairEntity> getActivePairs() {
        List<ClubbedPlayerPairEntity> pairs = repository.findAllByActiveTrue();
        return pairs == null ? List.of() : pairs;
    }

    public boolean isPaired(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }

        return getActivePairs().stream()
                .anyMatch(pair -> pair.getPlayerOne().equalsIgnoreCase(playerName)
                        || pair.getPlayerTwo().equalsIgnoreCase(playerName));
    }

    public boolean isPlayerTwo(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }

        return getActivePairs().stream()
                .anyMatch(pair -> pair.getPlayerTwo().equalsIgnoreCase(playerName));
    }

    public Optional<ClubbedPlayerPairEntity> getPairForPlayer(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }

        return getActivePairs().stream()
                .filter(pair -> pair.getPlayerOne().equalsIgnoreCase(playerName)
                        || pair.getPlayerTwo().equalsIgnoreCase(playerName))
                .findFirst();
    }

    public String getPairedPlayer(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        return getPairForPlayer(playerName)
                .map(pair -> {
                    if (pair.getPlayerOne().equalsIgnoreCase(playerName)) {
                        return pair.getPlayerTwo();
                    }
                    if (pair.getPlayerTwo().equalsIgnoreCase(playerName)) {
                        return pair.getPlayerOne();
                    }
                    return null;
                })
                .orElse(null);
    }

    public ClubbedPlayerPairEntity createPair(String playerOne, String playerTwo) {
        if (playerOne == null || playerTwo == null || playerOne.isBlank() || playerTwo.isBlank()) {
            throw new IllegalArgumentException("Both players are required to create a clubbed pair.");
        }

        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne(playerOne.trim());
        pair.setPlayerTwo(playerTwo.trim());
        pair.setActive(true);
        return repository.save(pair);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deletePair(Long id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void clearPair() {
        repository.deleteByActiveTrue();
    }
}
