package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class EffectiveSalePriceService {

    private final BlindOpeningBidRepository blindOpeningBidRepository;
    private final CursePlayerRepository cursePlayerRepository;
    private final ClubbedPlayerPairService clubbedPlayerPairService;

    public EffectiveSalePriceService(
            BlindOpeningBidRepository blindOpeningBidRepository,
            CursePlayerRepository cursePlayerRepository,
            ClubbedPlayerPairService clubbedPlayerPairService) {
        this.blindOpeningBidRepository = blindOpeningBidRepository;
        this.cursePlayerRepository = cursePlayerRepository;
        this.clubbedPlayerPairService = clubbedPlayerPairService;
    }

    public List<PlayerEntity> applyToPlayers(List<PlayerEntity> players) {
        List<PlayerEntity> soldPlayers = players.stream()
                .filter(PlayerEntity::isSold)
                .toList();
        if (soldPlayers.isEmpty()) {
            return players;
        }

        List<String> soldPlayerNames = soldPlayers.stream()
                .map(PlayerEntity::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, PlayerEntity> playersByName = new HashMap<>();
        for (PlayerEntity player : soldPlayers) {
            playersByName.put(playerKey(player.getName()), player);
        }

        Map<String, Integer> bonuses = new HashMap<>();
        for (BlindOpeningBidEntity bid : blindOpeningBidRepository.findByPlayerNameIn(soldPlayerNames)) {
            PlayerEntity player = playersByName.get(playerKey(bid.getPlayerName()));
            if (bid.isBonusAwarded() && player != null
                    && sameCaptain(player.getTeam(), bid.getCaptainName())) {
                bonuses.merge(playerKey(bid.getPlayerName()), bid.getBonusAwardedAmount(), Integer::sum);
            }
        }

        Map<String, Integer> buyerDeductions = new HashMap<>();
        for (CursePlayerEntity curse : cursePlayerRepository.findByPlayerNameIn(soldPlayerNames)) {
            PlayerEntity player = playersByName.get(playerKey(curse.getPlayerName()));
            if (curse.isTriggered() && player != null
                    && sameCaptain(player.getTeam(), curse.getSettledBuyer())
                    && curse.getSettledAmount() > 0) {
                buyerDeductions.merge(
                        playerKey(curse.getPlayerName()),
                        curse.getSettledAmount(),
                        Math::max);
            }
        }

        for (PlayerEntity player : soldPlayers) {
            player.setEffectivePrice(effectivePrice(
                    player,
                    bonuses.getOrDefault(playerKey(player.getName()), 0),
                    buyerDeductions.getOrDefault(playerKey(player.getName()), 0)));
        }

        for (ClubbedPlayerPairEntity pair : clubbedPlayerPairService.getActivePairs()) {
            PlayerEntity playerOne = playersByName.get(playerKey(pair.getPlayerOne()));
            PlayerEntity playerTwo = playersByName.get(playerKey(pair.getPlayerTwo()));
            if (playerOne == null || playerTwo == null
                    || !Objects.equals(playerOne.getTeam(), playerTwo.getTeam())) {
                continue;
            }

            int packageBonus = Math.max(
                    bonuses.getOrDefault(playerKey(pair.getPlayerOne()), 0),
                    bonuses.getOrDefault(playerKey(pair.getPlayerTwo()), 0));
            int packagePenalty = Math.max(
                    buyerDeductions.getOrDefault(playerKey(pair.getPlayerOne()), 0),
                    buyerDeductions.getOrDefault(playerKey(pair.getPlayerTwo()), 0));
            int effectivePackagePrice = effectivePrice(playerOne, packageBonus, packagePenalty);
            playerOne.setEffectivePrice(effectivePackagePrice);
            playerTwo.setEffectivePrice(effectivePackagePrice);
        }

        return players;
    }

    private int effectivePrice(PlayerEntity player, int bonus, int penalty) {
        return Math.max(0, player.getFinalPrice() - bonus + penalty);
    }

    private boolean sameCaptain(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private String playerKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
