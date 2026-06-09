package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;
import org.springframework.stereotype.Service;

@Service
public class AuctionService {

    private Auction currentAuction;

    private final TeamService teamService;
    private final PlayerService playerService;
    private final AuctionLogService auctionLogService;
    private final AdminActionLogService adminActionLogService;
    private final AuctionEventService auctionEventService;

    public AuctionService(
            TeamService teamService,
            PlayerService playerService,
            AuctionLogService auctionLogService,
            AdminActionLogService adminActionLogService,
            AuctionEventService auctionEventService) {

        this.teamService = teamService;
        this.playerService = playerService;
        this.auctionLogService = auctionLogService;
        this.adminActionLogService = adminActionLogService;
        this.auctionEventService = auctionEventService;

        currentAuction = new Auction(
                "",
                "",
                0,
                "None");
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public void nominatePlayer(
            String playerName,
            String seed) {

        currentAuction = new Auction(
                playerName,
                seed,
                0,
                "None");
        auctionEventService.logEvent(
                "PLAYER_NOMINATED",
                playerName,
                "",
                0,
                "Seed " + seed);
    }

    public String sellPlayer(
            String playerName,
            String captainName,
            int soldPrice) {

        TeamEntity team = teamService.getTeam(captainName);

        if (team == null) {
            return "Team not found";
        }

        PlayerEntity player = playerService.getPlayer(playerName);

        if (player == null) {
            return "Player not found";
        }

        player.setSold(true);
        player.setSoldPrice(soldPrice);
        player.setTeam(captainName);

        playerService.savePlayer(player);

        team.setPurse(
                team.getPurse() - soldPrice);

        team.setPlayersBought(
                team.getPlayersBought() + 1);

        team.setPlayersLeft(
                team.getPlayersLeft() - 1);

        teamService.saveTeam(team);

        AuctionLogEntity log = new AuctionLogEntity();

        log.setPlayerName(playerName);
        log.setCaptainName(captainName);
        log.setSoldPrice(soldPrice);

        auctionLogService.addLog(log);
        auctionEventService.logEvent(
                "PLAYER_SOLD",
                playerName,
                captainName,
                soldPrice,
                "Player sold in auction");

        currentAuction = new Auction(
                "",
                "",
                0,
                "None");

        return playerName
                + " sold to "
                + captainName
                + " for ₹"
                + soldPrice;
    }

    public String undoLastSale() {

        AuctionLogEntity lastLog = auctionLogService.getLastLog();

        if (lastLog == null) {
            return "No sale to undo";
        }

        PlayerEntity player = playerService.getPlayer(
                lastLog.getPlayerName());

        TeamEntity team = teamService.getTeam(
                lastLog.getCaptainName());

        if (player != null) {

            player.setSold(false);
            player.setSoldPrice(0);
            player.setTeam("");

            playerService.savePlayer(player);
        }

        if (team != null) {

            team.setPurse(
                    team.getPurse()
                            + lastLog.getSoldPrice());

            team.setPlayersBought(
                    team.getPlayersBought() - 1);

            team.setPlayersLeft(
                    team.getPlayersLeft() + 1);

            teamService.saveTeam(team);
        }

        auctionLogService.removeLastLog();
        auctionEventService.logEvent(
                "SALE_UNDONE",
                lastLog.getPlayerName(),
                lastLog.getCaptainName(),
                lastLog.getSoldPrice(),
                "Undo last sale");

        return "Last sale undone";
    }

    public String manualSale(
            String playerName,
            String newCaptain,
            int newPrice,
            String reason) {

        PlayerEntity player = playerService.getPlayer(playerName);

        if (player == null) {
            return "Player not found";
        }

        String oldCaptain = player.getTeam();

        int oldPrice = player.getSoldPrice();

        TeamEntity oldTeam = teamService.getTeam(oldCaptain);

        TeamEntity newTeam = teamService.getTeam(newCaptain);

        if (newTeam == null) {
            return "New team not found";
        }

        if (oldTeam != null) {

            oldTeam.setPurse(
                    oldTeam.getPurse() + oldPrice);

            oldTeam.setPlayersBought(
                    oldTeam.getPlayersBought() - 1);

            oldTeam.setPlayersLeft(
                    oldTeam.getPlayersLeft() + 1);

            teamService.saveTeam(oldTeam);
        }

        newTeam.setPurse(
                newTeam.getPurse() - newPrice);

        newTeam.setPlayersBought(
                newTeam.getPlayersBought() + 1);

        newTeam.setPlayersLeft(
                newTeam.getPlayersLeft() - 1);

        teamService.saveTeam(newTeam);

        adminActionLogService.addLog(
                "MANUAL_SALE",
                playerName,
                oldCaptain,
                newCaptain,
                oldPrice,
                newPrice,
                reason);
        auctionEventService.logEvent(
                "MANUAL_SALE",
                playerName,
                newCaptain,
                newPrice,
                reason);

        player.setTeam(newCaptain);
        player.setSoldPrice(newPrice);

        playerService.savePlayer(player);

        return "Manual Sale Updated";
    }
}