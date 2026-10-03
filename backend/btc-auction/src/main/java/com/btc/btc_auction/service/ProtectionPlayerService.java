package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.ProtectionPlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.ProtectionPlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProtectionPlayerService {

    private final ProtectionPlayerRepository repository;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final AuctionEventService auctionEventService;
    private final AdminActionLogService adminActionLogService;
    private final AuctionConfigService auctionConfigService;

    public ProtectionPlayerService(
            ProtectionPlayerRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionEventService auctionEventService) {
        this(repository, teamService, playerService, auctionEventService, null, null);
    }

    public ProtectionPlayerService(
            ProtectionPlayerRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionEventService auctionEventService,
            AdminActionLogService adminActionLogService) {
        this(repository, teamService, playerService, auctionEventService, adminActionLogService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProtectionPlayerService(
            ProtectionPlayerRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionEventService auctionEventService,
            AdminActionLogService adminActionLogService,
            AuctionConfigService auctionConfigService) {
        this.repository = repository;
        this.teamService = teamService;
        this.playerService = playerService;
        this.auctionEventService = auctionEventService;
        this.adminActionLogService = adminActionLogService;
        this.auctionConfigService = auctionConfigService;
    }

    public String saveProtection(String captainName, String playerName) {
        if (auctionConfigService != null) {
            if (!auctionConfigService.getConfig().isProtectionSelectionEnabled()) {
                return "Protection player selection has not been enabled by admin.";
            }
            if (auctionConfigService.getConfig().isAuctionStarted()) {
                return "Protection players can only be selected before the auction starts.";
            }
        }
        if (captainName == null || captainName.isBlank() || playerName == null || playerName.isBlank()) {
            return "Captain and player are required.";
        }

        if (playerService.getPlayer(playerName) == null) {
            return "Player not found.";
        }

        List<ProtectionPlayerEntity> existing = repository.findByCaptainName(captainName);
        if (existing.size() >= 2) {
            return "Each captain can protect only 2 players.";
        }

        if (repository.findByCaptainNameAndPlayerName(captainName, playerName).isPresent()) {
            return "This player is already protected.";
        }

        ProtectionPlayerEntity entity = new ProtectionPlayerEntity();
        entity.setCaptainName(captainName);
        entity.setPlayerName(playerName);
        repository.save(entity);
        if (adminActionLogService != null) {
            adminActionLogService.addLog(
                    "PROTECTION_PLAYER_SELECTED",
                    playerName,
                    "",
                    captainName,
                    0,
                    0,
                    "Captain selected this protection player before the auction");
        }
        return "Protection saved.";
    }

    public List<ProtectionPlayerEntity> getByCaptain(String captainName) {
        return repository.findByCaptainName(captainName);
    }

    public List<ProtectionPlayerEntity> getAll() {
        return repository.findAll();
    }

    public String autoSelectForTesting() {
        if (auctionConfigService != null) {
            AuctionConfigEntity config = auctionConfigService.getConfig();
            config.setProtectionSelectionEnabled(true);
            auctionConfigService.save(config);
        }

        List<PlayerEntity> players = playerService.getUnsoldPlayers();
        List<TeamEntity> teams = teamService.getAllTeams();
        if (teams.isEmpty() || players.size() < 2) {
            return "At least two players and one captain are required.";
        }

        int playerIndex = 0;
        for (TeamEntity team : teams) {
            List<ProtectionPlayerEntity> existing = repository.findByCaptainName(team.getCaptainName());
            while (existing.size() < 2) {
                PlayerEntity player = players.get(playerIndex % players.size());
                playerIndex++;
                if (existing.stream().anyMatch(item -> item.getPlayerName().equalsIgnoreCase(player.getName()))) {
                    continue;
                }

                ProtectionPlayerEntity protection = new ProtectionPlayerEntity();
                protection.setCaptainName(team.getCaptainName());
                protection.setPlayerName(player.getName());
                existing.add(repository.save(protection));
                if (adminActionLogService != null) {
                    adminActionLogService.addLog(
                            "PROTECTION_PLAYER_SELECTED",
                            player.getName(),
                            "",
                            team.getCaptainName(),
                            0,
                            0,
                            "Protection player auto-selected by admin bypass for testing");
                }
            }
        }
        return "Protection players auto-selected for testing.";
    }

    public int applyPurchaseReward(String playerName, String buyerCaptainName) {
        List<ProtectionPlayerEntity> matches = repository.findAll().stream()
                .filter(protection -> protection.getPlayerName().equalsIgnoreCase(playerName))
                .toList();

        if (matches.isEmpty()) {
            return 0;
        }

        ProtectionPlayerEntity protection = matches.get(0);
        TeamEntity protectorTeam = teamService.getTeam(protection.getCaptainName());
        TeamEntity buyerTeam = teamService.getTeam(buyerCaptainName);
        if (protectorTeam == null || buyerTeam == null) {
            return 0;
        }

        int protectionBonus = auctionConfigService == null ? 300
                : auctionConfigService.getConfig().getProtectionBonus();
        int protectionPenalty = auctionConfigService == null ? 200
                : auctionConfigService.getConfig().getProtectionPenalty();
        int adjustment;
        if (protection.getCaptainName().equalsIgnoreCase(buyerCaptainName)) {
            adjustment = protectionBonus;
            protectorTeam.setPurse(protectorTeam.getPurse() + protectionBonus);
            teamService.saveTeam(protectorTeam);
            auctionEventService.logEvent(
                    "PROTECTION_REVEALED",
                    playerName,
                    buyerCaptainName,
                    adjustment,
                    "Protected player bought by " + buyerCaptainName
                            + "; purse increased by +₹" + protectionBonus);
            if (adminActionLogService != null) {
                adminActionLogService.addLog(
                        "PROTECTION_BOOST",
                        playerName,
                        "",
                        buyerCaptainName,
                        0,
                        adjustment,
                        "Purse increased by +₹" + protectionBonus);
            }
            auctionEventService.logEvent(
                    "PROTECTION_BOOST",
                    playerName,
                    buyerCaptainName,
                    adjustment,
                    "Purse increased by +₹" + protectionBonus);
            return adjustment;
        }

        adjustment = -protectionPenalty;
        protectorTeam.setPurse(Math.max(0, protectorTeam.getPurse() - protectionPenalty));
        teamService.saveTeam(protectorTeam);
        auctionEventService.logEvent(
                "PROTECTION_REVEALED",
                playerName,
                buyerCaptainName,
                protectionPenalty,
                "Protected player bought by " + buyerCaptainName
                        + "; protector purse reduced by -₹" + protectionPenalty);
        if (adminActionLogService != null) {
            adminActionLogService.addLog(
                    "PROTECTION_PENALTY",
                    playerName,
                    "",
                    buyerCaptainName,
                    0,
                    adjustment,
                    "Protector purse reduced by -₹" + protectionPenalty);
        }
        auctionEventService.logEvent(
                "PROTECTION_PENALTY",
                playerName,
                buyerCaptainName,
                adjustment,
                "Protector purse reduced by -₹" + protectionPenalty);
        return adjustment;
    }

    public void clearAll() {
        repository.deleteAll();
    }
}
