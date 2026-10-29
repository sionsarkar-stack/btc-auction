package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.PlayerRepository;
import com.btc.btc_auction.repository.TeamRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EffectivePurseService {

    private static final Logger log = LoggerFactory.getLogger(EffectivePurseService.class);

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final EffectiveSalePriceService effectiveSalePriceService;
    private final ClubbedPlayerPairService clubbedPlayerPairService;
    private final CursePlayerRepository cursePlayerRepository;

    public EffectivePurseService(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            EffectiveSalePriceService effectiveSalePriceService,
            ClubbedPlayerPairService clubbedPlayerPairService,
            CursePlayerRepository cursePlayerRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.effectiveSalePriceService = effectiveSalePriceService;
        this.clubbedPlayerPairService = clubbedPlayerPairService;
        this.cursePlayerRepository = cursePlayerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcilePurses() {
        List<PlayerEntity> players = effectiveSalePriceService.applyToPlayers(playerRepository.findAll());
        Map<String, PlayerEntity> soldPlayers = new HashMap<>();
        players.stream().filter(PlayerEntity::isSold)
                .forEach(player -> soldPlayers.put(key(player.getName()), player));
        Set<String> includedPackagePlayers = new HashSet<>();
        clubbedPlayerPairService.getActivePairs().forEach(pair -> {
            PlayerEntity first = soldPlayers.get(key(pair.getPlayerOne()));
            PlayerEntity second = soldPlayers.get(key(pair.getPlayerTwo()));
            if (first != null && second != null && key(first.getTeam()).equals(key(second.getTeam()))) {
                includedPackagePlayers.add(key(second.getName()));
            }
        });

        Map<String, Integer> credits = new HashMap<>();
        soldPlayers.values().stream()
                .filter(player -> !includedPackagePlayers.contains(key(player.getName())))
                .forEach(player -> credits.merge(key(player.getTeam()),
                        player.getFinalPrice() - player.getEffectivePrice(), Integer::sum));

        Map<String, String> packageKeys = new HashMap<>();
        clubbedPlayerPairService.getActivePairs().forEach(pair -> {
            packageKeys.put(key(pair.getPlayerOne()), key(pair.getPlayerOne()));
            packageKeys.put(key(pair.getPlayerTwo()), key(pair.getPlayerOne()));
        });
        Map<String, Map<String, Integer>> rewardsBySetter = new HashMap<>();
        cursePlayerRepository.findAll().stream()
                .filter(curse -> curse.isTriggered() && curse.getSettledAmount() > 0)
                .filter(curse -> {
                    PlayerEntity player = soldPlayers.get(key(curse.getPlayerName()));
                    return player != null && key(player.getTeam()).equals(key(curse.getSettledBuyer()));
                })
                .forEach(curse -> rewardsBySetter
                        .computeIfAbsent(key(curse.getCaptainName()), ignored -> new HashMap<>())
                        .merge(packageKeys.getOrDefault(key(curse.getPlayerName()), key(curse.getPlayerName())),
                                curse.getSettledAmount(), Math::max));

        for (TeamEntity team : teamRepository.findAll()) {
            int credit = credits.getOrDefault(key(team.getCaptainName()), 0);
            int reward = rewardsBySetter.getOrDefault(key(team.getCaptainName()), Map.of())
                    .values().stream().mapToInt(Integer::intValue).sum();
            int difference = credit - team.getEffectivePriceCredit()
                    + reward - team.getCurseRewardCredit();
            if (difference != 0 || credit != team.getEffectivePriceCredit()
                    || reward != team.getCurseRewardCredit()) {
                team.setPurse(team.getPurse() + difference);
                team.setEffectivePriceCredit(credit);
                team.setCurseRewardCredit(reward);
                teamRepository.save(team);
                log.info("Reconciled effective-price purse for {} by {} to {}",
                        team.getCaptainName(), difference, team.getPurse());
            }
        }
    }

    private String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
