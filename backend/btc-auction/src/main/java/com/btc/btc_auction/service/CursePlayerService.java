package com.btc.btc_auction.service;

import com.btc.btc_auction.dto.CurseSelectionDto;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.CursePlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CursePlayerService {

    private static final int CURSES_PER_CAPTAIN = 2;
    private static final int CURSE_PERCENT = 20;
    private static final int POINTS_STEP = 50;

    private final CursePlayerRepository repository;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final AuctionConfigService auctionConfigService;
    private final AuctionEventService auctionEventService;
    private final AdminActionLogService adminActionLogService;
    private final AuctionSocketService auctionSocketService;

    public CursePlayerService(
            CursePlayerRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionConfigService auctionConfigService,
            AuctionEventService auctionEventService,
            AdminActionLogService adminActionLogService,
            AuctionSocketService auctionSocketService) {
        this.repository = repository;
        this.teamService = teamService;
        this.playerService = playerService;
        this.auctionConfigService = auctionConfigService;
        this.auctionEventService = auctionEventService;
        this.adminActionLogService = adminActionLogService;
        this.auctionSocketService = auctionSocketService;
    }

    public String enableSelection() {
        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (config.isAuctionStarted()) {
            return "Curse selection cannot be enabled after the auction starts.";
        }
        config.setCurseSelectionEnabled(true);
        auctionConfigService.save(config);
        auctionSocketService.broadcastRefresh();
        return "Curse selection enabled for captains.";
    }

    public String saveCurse(String captainName, String playerName) {
        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (!config.isCurseSelectionEnabled()) {
            return "Curse selection has not been enabled by the admin.";
        }
        if (config.isAuctionStarted()) {
            return "Curses can only be selected before the auction starts.";
        }
        if (captainName == null || captainName.isBlank() || teamService.getTeam(captainName) == null) {
            return "Captain not found.";
        }
        if (playerName == null || playerName.isBlank()) {
            return "Player is required.";
        }

        PlayerEntity player = playerService.getPlayer(playerName);
        if (player == null || player.isSold()) {
            return "Available player not found.";
        }

        List<CursePlayerEntity> existing = repository.findByCaptainName(captainName);
        if (existing.size() >= CURSES_PER_CAPTAIN) {
            return "Each captain can curse only 2 players.";
        }
        if (repository.findByCaptainNameAndPlayerName(captainName, playerName).isPresent()) {
            return "This player is already cursed by your team.";
        }

        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setCaptainName(captainName);
        curse.setPlayerName(playerName);
        repository.save(curse);
        logCurseSelection(captainName, playerName);
        auctionSocketService.broadcastRefresh();
        return "Curse saved secretly.";
    }

    public List<CursePlayerEntity> getByCaptain(String captainName) {
        return repository.findByCaptainName(captainName);
    }

    public List<CurseSelectionDto> getAllSelections() {
        return repository.findAllByOrderByIdDesc().stream()
                .map(curse -> new CurseSelectionDto(
                        curse.getId(),
                        curse.getCaptainName(),
                        curse.getPlayerName()))
                .toList();
    }

    public Map<String, Integer> getSelectionCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TeamEntity team : teamService.getAllTeams()) {
            counts.put(team.getCaptainName(), repository.findByCaptainName(team.getCaptainName()).size());
        }
        return counts;
    }

    public boolean areAllCaptainsReady(List<TeamEntity> teams) {
        AuctionConfigEntity config = auctionConfigService.getConfig();
        return config.isCurseSelectionEnabled()
                && !teams.isEmpty()
                && teams.stream().allMatch(
                        team -> repository.findByCaptainName(team.getCaptainName()).size() == CURSES_PER_CAPTAIN);
    }

    @Transactional
    public String autoSelectForTesting() {
        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (config.isAuctionStarted()) {
            return "Curse selections cannot be changed after the auction starts.";
        }

        List<TeamEntity> teams = teamService.getAllTeams();
        List<PlayerEntity> players = playerService.getUnsoldPlayers().stream()
                .filter(player -> !player.isDeferredToReAuction())
                .toList();
        if (teams.isEmpty() || players.size() < CURSES_PER_CAPTAIN) {
            return "At least two players and one captain are required.";
        }

        config.setCurseSelectionEnabled(true);
        auctionConfigService.save(config);
        for (TeamEntity team : teams) {
            List<CursePlayerEntity> existing = repository.findByCaptainName(team.getCaptainName());
            List<String> assignedNames = new ArrayList<>(
                    existing.stream().map(CursePlayerEntity::getPlayerName).toList());
            while (assignedNames.size() < CURSES_PER_CAPTAIN) {
                List<PlayerEntity> candidates = players.stream()
                        .filter(player -> !assignedNames.contains(player.getName()))
                        .toList();
                if (candidates.isEmpty()) {
                    return "Unable to select two distinct curse players for every captain.";
                }
                PlayerEntity player = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                saveAutoSelection(team.getCaptainName(), player.getName());
                assignedNames.add(player.getName());
            }
        }
        auctionSocketService.broadcastRefresh();
        return "Two curse players were auto-selected for every captain.";
    }

    @Transactional
    public void settleSale(String playerName, String buyerCaptainName, int finalPayablePrice) {
        List<CursePlayerEntity> curses = repository.findByPlayerName(playerName).stream()
                .filter(curse -> !curse.isTriggered())
                .toList();
        if (curses.isEmpty()) {
            return;
        }

        List<String> setters = curses.stream()
                .map(CursePlayerEntity::getCaptainName)
                .distinct()
                .toList();
        boolean buyerSetTheCurse = setters.stream()
                .anyMatch(setter -> setter.equalsIgnoreCase(buyerCaptainName));
        if (buyerSetTheCurse) {
            markTriggered(curses, buyerCaptainName, 0, 0);
            auctionEventService.logEvent(
                    "CURSE_CANCELLED",
                    playerName,
                    buyerCaptainName,
                    0,
                    "Curse cancelled: the player was bought by a captain who cursed them.");
            return;
        }

        int curseAmount = calculateCurseAmount(finalPayablePrice, curses.size());
        TeamEntity buyer = teamService.getTeam(buyerCaptainName);
        int pointsBefore = buyer.getTournamentPoints();
        buyer.setTournamentPoints(Math.max(0, pointsBefore - curseAmount));
        int buyerDeduction = pointsBefore - buyer.getTournamentPoints();
        teamService.saveTeam(buyer);

        for (String setter : setters) {
            TeamEntity setterTeam = teamService.getTeam(setter);
            if (setterTeam != null) {
                setterTeam.setTournamentPoints(setterTeam.getTournamentPoints() + curseAmount);
                teamService.saveTeam(setterTeam);
            }
        }

        String setterNames = String.join(", ", setters);
        String details = "Curse set by " + setterNames + ". Buyer lost " + buyerDeduction
                + " tournament points; each setter gained " + curseAmount + ".";
        auctionEventService.logEvent("CURSE_REVEALED", playerName, buyerCaptainName, curseAmount, details);
        adminActionLogService.addLog("CURSE_SETTLED", playerName, "", buyerCaptainName, 0, -buyerDeduction, details);
        markTriggered(curses, buyerCaptainName, curseAmount, buyerDeduction);
    }

    public int calculateCurseAmount(int finalPayablePrice, int curseCount) {
        double rawPoints = (double) finalPayablePrice * CURSE_PERCENT * curseCount / 100;
        return (int) (Math.floor(rawPoints / POINTS_STEP + 0.5) * POINTS_STEP);
    }

    @Transactional
    public void expireForUnsoldPlayers() {
        repository.findAll().stream()
                .filter(curse -> !curse.isTriggered())
                .filter(curse -> {
                    PlayerEntity player = playerService.getPlayer(curse.getPlayerName());
                    return player == null || !player.isSold();
                })
                .forEach(repository::delete);
    }

    @Transactional
    public void undoSettlement(String playerName) {
        List<CursePlayerEntity> triggered = repository.findByPlayerName(playerName).stream()
                .filter(CursePlayerEntity::isTriggered)
                .toList();
        if (triggered.isEmpty()) {
            return;
        }

        CursePlayerEntity settlement = triggered.get(0);
        if (settlement.getSettledAmount() > 0) {
            TeamEntity buyer = teamService.getTeam(settlement.getSettledBuyer());
            if (buyer != null) {
                buyer.setTournamentPoints(buyer.getTournamentPoints() + settlement.getBuyerDeduction());
                teamService.saveTeam(buyer);
            }
            triggered.stream().map(CursePlayerEntity::getCaptainName).distinct().forEach(setter -> {
                TeamEntity setterTeam = teamService.getTeam(setter);
                if (setterTeam != null) {
                    setterTeam.setTournamentPoints(setterTeam.getTournamentPoints() - settlement.getSettledAmount());
                    teamService.saveTeam(setterTeam);
                }
            });
        }

        triggered.forEach(curse -> {
            curse.setTriggered(false);
            curse.setSettledBuyer(null);
            curse.setSettledAmount(0);
            curse.setBuyerDeduction(0);
        });
        repository.saveAll(triggered);
    }

    public void deleteForCaptain(String captainName) {
        repository.deleteByCaptainName(captainName);
    }

    public void renameCaptain(String oldName, String newName) {
        repository.findByCaptainName(oldName).forEach(curse -> {
            curse.setCaptainName(newName);
            repository.save(curse);
        });
    }

    public void clearAll() {
        repository.deleteAll();
    }

    private void markTriggered(List<CursePlayerEntity> curses, String buyer, int amount, int buyerDeduction) {
        curses.forEach(curse -> {
            curse.setTriggered(true);
            curse.setSettledBuyer(buyer);
            curse.setSettledAmount(amount);
            curse.setBuyerDeduction(buyerDeduction);
        });
        repository.saveAll(curses);
    }

    private void saveAutoSelection(String captainName, String playerName) {
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setCaptainName(captainName);
        curse.setPlayerName(playerName);
        repository.save(curse);
        logCurseSelection(captainName, playerName);
    }

    private void logCurseSelection(String captainName, String playerName) {
        adminActionLogService.addLog(
                "CURSE_SELECTED",
                playerName,
                captainName,
                "Curse player selected.");
    }
}