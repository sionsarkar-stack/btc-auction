package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.AuctionLog;
import org.springframework.stereotype.Service;

@Service
public class AuctionService {

    private Auction currentAuction;

    private final TeamService teamService;
    private final PlayerService playerService;
    private final AuctionLogService auctionLogService;

    public AuctionService(
            TeamService teamService,
            PlayerService playerService,
            AuctionLogService auctionLogService) {

        this.teamService = teamService;
        this.playerService = playerService;
        this.auctionLogService = auctionLogService;

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

        return "Last sale undone";
    }
}