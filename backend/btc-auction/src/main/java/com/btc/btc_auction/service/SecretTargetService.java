package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.SecretTargetEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.SecretTargetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecretTargetService {
    private final SecretTargetRepository repository;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final AuctionEventService eventService;
    private final AuctionConfigService configService;
    private final AdminActionLogService adminActionLogService;

    public SecretTargetService(SecretTargetRepository repository, PlayerService playerService,
            TeamService teamService, AuctionEventService eventService, AuctionConfigService configService,
            AdminActionLogService adminActionLogService) {
        this.repository = repository;
        this.playerService = playerService;
        this.teamService = teamService;
        this.eventService = eventService;
        this.configService = configService;
        this.adminActionLogService = adminActionLogService;
    }

    public String save(SecretTargetEntity target) {
        if (configService.getConfig().isAuctionStarted()) {
            return "Secret targets must be selected before the auction starts.";
        }
        if (target.getCaptainName() == null || target.getCaptainName().isBlank()
                || target.getPlayerOne() == null || target.getPlayerOne().isBlank()
                || target.getPlayerTwo() == null || target.getPlayerTwo().isBlank()) {
            return "Captain and both target players are required.";
        }
        if (target.getPlayerOne().equalsIgnoreCase(target.getPlayerTwo())) {
            return "Select two different target players.";
        }
        if (teamService.getTeam(target.getCaptainName()) == null
                || playerService.getPlayer(target.getPlayerOne()) == null
                || playerService.getPlayer(target.getPlayerTwo()) == null) {
            return "Captain or target player was not found.";
        }
        if (repository.findByCaptainName(target.getCaptainName()).isPresent()) {
            return "Secret targets already submitted.";
        }
        target.setSettled(false);
        target.setPlayerOneSettled(false);
        target.setPlayerTwoSettled(false);
        target.setPairSettled(false);
        target.setAwardedAmount(0);
        repository.save(target);
        adminActionLogService.addLog("SECRET_TARGET_SUBMITTED",
                target.getPlayerOne() + ", " + target.getPlayerTwo(),
                target.getCaptainName(),
                "Secret targets locked");
        return "Secret targets saved.";
    }

    public SecretTargetEntity getByCaptain(String captainName) {
        return repository.findByCaptainName(captainName).orElse(null);
    }

    public List<SecretTargetEntity> getAll() {
        return repository.findAll();
    }

    public void settleAll() {
        for (SecretTargetEntity target : repository.findAll()) {
            if (target.isSettled()) {
                continue;
            }

            settleUnresolvedTarget(target, true);
            settleUnresolvedTarget(target, false);
            updateSettlementStatus(target);
            repository.save(target);
        }
    }

    public int applyPurchaseReward(String playerName, String buyerCaptainName) {
        int buyerAdjustment = 0;

        for (SecretTargetEntity target : repository.findAll()) {
            if (target.isSettled()) {
                continue;
            }

            int adjustment = 0;
            if (target.getPlayerOne().equalsIgnoreCase(playerName)
                    && !target.isPlayerOneSettled()) {
                adjustment = settlePurchasedTarget(target, true, playerName, buyerCaptainName);
            } else if (target.getPlayerTwo().equalsIgnoreCase(playerName)
                    && !target.isPlayerTwoSettled()) {
                adjustment = settlePurchasedTarget(target, false, playerName, buyerCaptainName);
            } else {
                continue;
            }

            updateSettlementStatus(target);
            repository.save(target);

            if (target.getCaptainName().equalsIgnoreCase(buyerCaptainName)) {
                buyerAdjustment += adjustment;
            }
        }

        return buyerAdjustment;
    }

    private int settlePurchasedTarget(
            SecretTargetEntity target,
            boolean firstTarget,
            String playerName,
            String buyerCaptainName) {

        boolean boughtByTargetCaptain = target.getCaptainName().equalsIgnoreCase(buyerCaptainName);
        int adjustment = boughtByTargetCaptain
                ? isOtherTargetBoughtByCaptain(target, firstTarget)
                        ? configService.getConfig().getTargetCompletionBonus()
                        : configService.getConfig().getTargetBonus()
                : -configService.getConfig().getTargetMissPenalty();

        if (firstTarget) {
            target.setPlayerOneSettled(true);
        } else {
            target.setPlayerTwoSettled(true);
        }

        return applyPurseAdjustment(
                target,
                playerName,
                adjustment,
                boughtByTargetCaptain
                        ? "Secret target bought by captain"
                        : "Secret target not bought by captain");
    }

    private void settleUnresolvedTarget(
            SecretTargetEntity target,
            boolean firstTarget) {

        boolean alreadySettled = firstTarget
                ? target.isPlayerOneSettled()
                : target.isPlayerTwoSettled();
        if (alreadySettled) {
            return;
        }

        String playerName = firstTarget ? target.getPlayerOne() : target.getPlayerTwo();
        boolean boughtByTargetCaptain = isBoughtByTargetCaptain(target, playerName);
        int adjustment = boughtByTargetCaptain
                ? isOtherTargetBoughtByCaptain(target, firstTarget)
                        ? configService.getConfig().getTargetCompletionBonus()
                        : configService.getConfig().getTargetBonus()
                : -configService.getConfig().getTargetMissPenalty();

        if (firstTarget) {
            target.setPlayerOneSettled(true);
        } else {
            target.setPlayerTwoSettled(true);
        }

        applyPurseAdjustment(
                target,
                playerName,
                adjustment,
                boughtByTargetCaptain
                        ? "Secret target bought by captain"
                        : "Secret target not bought by captain");
    }

    private boolean isOtherTargetBoughtByCaptain(
            SecretTargetEntity target,
            boolean firstTarget) {

        String otherPlayer = firstTarget ? target.getPlayerTwo() : target.getPlayerOne();
        return isBoughtByTargetCaptain(target, otherPlayer);
    }

    private boolean isBoughtByTargetCaptain(
            SecretTargetEntity target,
            String playerName) {

        PlayerEntity player = playerService.getPlayer(playerName);
        return player != null
                && player.isSold()
                && target.getCaptainName().equalsIgnoreCase(player.getTeam());
    }

    private int applyPurseAdjustment(
            SecretTargetEntity target,
            String playerName,
            int adjustment,
            String details) {

        TeamEntity team = teamService.getTeam(target.getCaptainName());
        if (team == null) {
            return 0;
        }

        if (adjustment != 0) {
            team.setPurse(team.getPurse() + adjustment);
            teamService.saveTeam(team);
            eventService.logEvent(
                    "SECRET_TARGET_SETTLED",
                    playerName,
                    target.getCaptainName(),
                    adjustment,
                    details);
            adminActionLogService.addLog(
                    "SECRET_TARGET_SETTLED",
                    playerName,
                    target.getCaptainName(),
                    details + "; adjustment ₹" + adjustment);
        }

        target.setAwardedAmount(target.getAwardedAmount() + adjustment);
        return adjustment;
    }

    private void updateSettlementStatus(SecretTargetEntity target) {
        boolean allTargetsSettled = target.isPlayerOneSettled()
                && target.isPlayerTwoSettled();
        target.setPairSettled(allTargetsSettled);
        target.setSettled(allTargetsSettled);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
