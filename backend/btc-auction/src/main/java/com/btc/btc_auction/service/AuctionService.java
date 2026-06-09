package com.btc.btc_auction.service;

import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.model.AuctionLog;
import com.btc.btc_auction.model.Player;
import com.btc.btc_auction.model.Team;
import org.springframework.stereotype.Service;

@Service
public class AuctionService {

    private Auction currentAuction = new Auction("", "", 0, "None");

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
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public void nominatePlayer(String playerName,
            String seed) {

        currentAuction = new Auction(
                playerName,
                seed,
                0,
                "None");
    }

    public String sellPlayer(String playerName,
            String captainName,
            int soldPrice) {

        Team team = teamService.getTeam(captainName);

        if (team == null) {
            return "Team not found";
        }

        Player player = playerService.getPlayer(playerName);

        if (player != null) {

            player.setSold(true);
            player.setSoldPrice(soldPrice);
            player.setTeam(captainName);
        }

        team.setPurse(
                team.getPurse() - soldPrice);

        team.setPlayersBought(
                team.getPlayersBought() + 1);

        team.setPlayersLeft(
                team.getPlayersLeft() - 1);

        team.getSquad().add(playerName);

        auctionLogService.addLog(
                new AuctionLog(
                        playerName,
                        captainName,
                        soldPrice));

        currentAuction = new Auction(
                "",
                "",
                0,
                "None");

        return playerName +
                " sold to " +
                captainName +
                " for ₹" +
                soldPrice;
    }

    public String undoLastSale() {

        AuctionLog lastLog = auctionLogService.getLastLog();

        if (lastLog == null) {
            return "No sale to undo";
        }

        Player player = playerService.getPlayer(
                lastLog.getPlayerName());

        Team team = teamService.getTeam(
                lastLog.getCaptainName());

        if (player != null) {

            player.setSold(false);
            player.setSoldPrice(0);
            player.setTeam("");
        }

        if (team != null) {

            team.setPurse(
                    team.getPurse()
                            + lastLog.getSoldPrice());

            team.setPlayersBought(
                    team.getPlayersBought() - 1);

            team.setPlayersLeft(
                    team.getPlayersLeft() + 1);

            team.getSquad()
                    .remove(
                            lastLog.getPlayerName());
        }

        auctionLogService.removeLastLog();

        return "Last sale undone";
    }
}