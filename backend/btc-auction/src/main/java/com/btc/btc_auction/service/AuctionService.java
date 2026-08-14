package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.ReverseTargetEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;

import java.util.List;

import org.springframework.stereotype.Service;
import com.btc.btc_auction.entity.BountyPlayerEntity;

@Service
public class AuctionService {

        private final CurrentAuctionService currentAuctionService;
        private final TeamService teamService;
        private final PlayerService playerService;
        private final AuctionLogService auctionLogService;
        private final AdminActionLogService adminActionLogService;
        private final AuctionEventService auctionEventService;
        private final AuctionConfigService auctionConfigService;
        private final BountyPlayerService bountyPlayerService;
        private final ReverseTargetService reverseTargetService;
        private final RtmService rtmService;
        private final AuctionSocketService auctionSocketService;
        private final SecretTargetService secretTargetService;

        private final JokerService jokerService;

        public AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        BountyPlayerService bountyPlayerService,
                        JokerService jokerService, ReverseTargetService reverseTargetService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService, SecretTargetService secretTargetService) {

                this.teamService = teamService;
                this.playerService = playerService;
                this.auctionLogService = auctionLogService;
                this.adminActionLogService = adminActionLogService;
                this.auctionEventService = auctionEventService;
                this.auctionConfigService = auctionConfigService;
                this.bountyPlayerService = bountyPlayerService;
                this.reverseTargetService = reverseTargetService;
                this.jokerService = jokerService;
                this.rtmService = rtmService;
                this.currentAuctionService = currentAuctionService;
                this.auctionSocketService = auctionSocketService;
                this.secretTargetService = secretTargetService;
                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));
                AuctionConfigEntity config = auctionConfigService.getConfig();

                config.setAuctionPhase(
                                AuctionPhase.NOMINATION);

                auctionConfigService.save(config);
        }

        public Auction getCurrentAuction() {

                return currentAuctionService.getCurrentAuction();

        }

        public synchronized String nominatePlayer(
                        String playerName,
                        String seed,

                        String nominatedBy) {
                if (!currentAuctionService
                                .getCurrentAuction()
                                .getCurrentPlayer()
                                .isBlank()) {

                        return "Auction already in progress";
                }

                AuctionConfigEntity config = auctionConfigService.getConfig();
                if (!config.isAuctionStarted()) {
                        return "Auction has not started";
                }

                PlayerEntity player = playerService.getPlayer(
                                playerName);

                if (player == null) {
                        return "Player not found";
                }

                if (player.isSold()) {
                        return "Player already sold";
                }

                TeamEntity nominatingTeam = teamService.getTeam(nominatedBy);
                if (nominatingTeam == null) {
                        return "Nominating captain not found";
                }

                if (nominatingTeam.getPlayersLeft() <= 0) {
                        return "Your squad is already complete";
                }

                if (player.getBasePrice() <= 0) {
                        return "Player has no valid base price";
                }

                currentAuctionService.setCurrentAuction(new Auction(
                                playerName,
                                seed,
                                player.getBasePrice(),
                                "None",
                                player.getBasePrice(), nominatedBy));
                config.setAuctionPhase(
                                AuctionPhase.NOMINATION);

                auctionConfigService.save(config);

                auctionEventService.logEvent(
                                "PLAYER_NOMINATED",
                                playerName,
                                nominatedBy,
                                0,
                                "Seed " + seed);
                auctionSocketService.broadcastRefresh();

                return "Player nominated";
        }

        public synchronized String sellPlayer(
                        String playerName,
                        String captainName,
                        int soldPrice) {

                Auction auction = currentAuctionService.getCurrentAuction();
                AuctionConfigEntity config = auctionConfigService.getConfig();

                if (auction == null || auction.getCurrentPlayer().isBlank()) {
                        return "No active auction";
                }

                if (config.getAuctionPhase() != AuctionPhase.SOLD) {
                        return "Call SOLD before confirming the sale";
                }

                if (!auction.getCurrentPlayer().equalsIgnoreCase(playerName)
                                || !auction.getLeader().equalsIgnoreCase(captainName)
                                || auction.getCurrentBid() != soldPrice) {
                        return "Sale must match the current SOLD player, leader, and bid";
                }

                TeamEntity team = teamService.getTeam(captainName);
                int penalty = 0;

                if (team == null) {
                        return "Team not found";
                }

                if (team.getPlayersLeft() <= 0) {
                        return "Squad is already complete";
                }

                if (soldPrice > teamService.getMaxBid(team)) {
                        return "Sold price exceeds the team's maximum bid";
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
                                team.getPurse()
                                                - soldPrice
                                                - penalty);

                team.setPlayersBought(
                                team.getPlayersBought() + 1);

                team.setPlayersLeft(
                                team.getPlayersLeft() - 1);

                teamService.saveTeam(team);
                List<ReverseTargetEntity> targets = reverseTargetService.getAll();

                for (ReverseTargetEntity target : targets) {

                        if (target.getRivalCaptain().equalsIgnoreCase(captainName)
                                        && target.getPlayerName().equalsIgnoreCase(playerName)) {

                                team.setPurse(team.getPurse() - 200);
                                teamService.saveTeam(team);

                                        auctionEventService.logEvent(
                                                        "REVERSE_TARGET_TRIGGERED",
                                                        playerName,
                                                        captainName,
                                                        -200,
                                                        "Reverse target set by "
                                                                        + target.getCaptainName()
                                                                        + " triggered; ₹200 deducted");

                        }

                }
                BountyPlayerEntity bounty = bountyPlayerService
                                .getByPlayer(
                                                playerName);

                if (bounty != null &&
                                !bounty.isRevealed()) {

                        int reward = bounty.isGolden()
                                        ? config.getGoldenBountyBonus()
                                        : config.getBountyBonus();

                        team.setPurse(
                                        team.getPurse()
                                                        + reward);

                        teamService.saveTeam(team);

                        bounty.setRevealed(true);

                        bountyPlayerService.save(
                                        bounty);

                        auctionEventService.logEvent(
                                        bounty.isGolden()
                                                        ? "GOLDEN_BOUNTY"
                                                        : "BOUNTY",
                                        playerName,
                                        captainName,
                                        reward,
                                        bounty.isGolden()
                                                        ? "Golden bounty revealed"
                                                        : "Bounty revealed");
                }
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
                rtmService.clearCurrentAuctionClaim();
                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));

                config.setAuctionPhase(
                                AuctionPhase.NO_AUCTION);

                auctionConfigService.save(config);
                auctionSocketService.broadcastRefresh();

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

                auctionSocketService.broadcastRefresh();

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

                auctionSocketService.broadcastRefresh();

                return "Manual Sale Updated";
        }

        public String resetAuction() {

                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setAuctionStarted(false);
                config.setSeasonName("BTC Season 11");
                config.setSquadSize(10);
                config.setTargetBonus(150);
                config.setTargetCompletionBonus(400);
                config.setBountyBonus(100);
                config.setGoldenBountyBonus(200);
                config.setStealPenalty(200);
                config.setAuctionPhase(
                                AuctionPhase.NO_AUCTION);

                auctionConfigService.save(config);

                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));

                playerService.getAllPlayers()
                                .forEach(player -> {

                                        player.setSold(false);
                                        player.setSoldPrice(0);
                                        player.setTeam("");

                                        playerService.savePlayer(player);
                                });

                teamService.resetSeasonElevenTeams();

                bountyPlayerService.getAll()
                                .forEach(bounty -> {

                                        bounty.setRevealed(false);

                                        bountyPlayerService.save(
                                                        bounty);
                                });

                auctionLogService.clearLogs();

                auctionEventService.clearEvents();

                bountyPlayerService.deleteAll();

                jokerService.deleteAll();

                reverseTargetService.deleteAll();

                secretTargetService.deleteAll();

                rtmService.clear();

                auctionSocketService.broadcastRefresh();

                return "Auction Reset Successfully";
        }

        public String endAuction() {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setAuctionStarted(false);
                config.setAuctionPhase(AuctionPhase.NO_AUCTION);
                auctionConfigService.save(config);
                secretTargetService.settleAll();
                auctionSocketService.broadcastRefresh();
                return "Auction ended and secret targets settled.";
        }

        public String vetoCurrentAuction() {

                if (currentAuctionService
                                .getCurrentAuction()
                                .getCurrentPlayer().isBlank()) {

                        return "No active nomination.";

                }

                String player = currentAuctionService
                                .getCurrentAuction()
                                .getCurrentPlayer();

                AuctionConfigEntity config = auctionConfigService.getConfig();

                config.setAuctionPhase(
                                AuctionPhase.NO_AUCTION);

                auctionConfigService.save(config);

                auctionEventService.logEvent(
                                "PLAYER_VETOED",
                                player,
                                "",
                                0,
                                "Nomination cancelled by VETO");

                currentAuctionService.setCurrentAuction(
                                new Auction(
                                                "",
                                                "",
                                                0,
                                                "None",
                                                0,
                                                ""));
                auctionSocketService.broadcastRefresh();

                return player + " nomination cancelled.";

        }

        public String applyLastStrike(

                        String captainName) {

                if (currentAuctionService.getCurrentAuction().getCurrentPlayer().isBlank()) {

                        return "No active auction.";

                }

                currentAuctionService.getCurrentAuction().setCurrentBid(

                                currentAuctionService.getCurrentAuction().getCurrentBid() + 100);

                currentAuctionService.getCurrentAuction().setLeader(

                                captainName);

                AuctionConfigEntity config =

                                auctionConfigService.getConfig();

                config.setAuctionPhase(

                                AuctionPhase.BIDDING);

                auctionConfigService.save(config);

                rtmService.clearCurrentAuctionClaim();

                auctionEventService.logEvent(

                                "LAST_STRIKE",

                                currentAuctionService.getCurrentAuction().getCurrentPlayer(),

                                captainName,

                                currentAuctionService.getCurrentAuction().getCurrentBid(),

                                "⚡ Last Strike");
                auctionSocketService.broadcastRefresh();

                return "⚡ Last Strike activated.";

        }

        public String callSold() {

                if (currentAuctionService.getCurrentAuction().getCurrentPlayer().isBlank()) {
                        return "No active auction.";
                }
                if ("None".equals(currentAuctionService.getCurrentAuction().getLeader())) {
                        Auction auction = currentAuctionService.getCurrentAuction();
                        auction.setLeader(auction.getNominatedBy());
                        auction.setCurrentBid(auction.getBasePrice());

                        AuctionConfigEntity config = auctionConfigService.getConfig();
                        config.setAuctionPhase(AuctionPhase.SOLD);
                        auctionConfigService.save(config);

                        return sellPlayer(auction.getCurrentPlayer(), auction.getNominatedBy(), auction.getBasePrice());

                }
                rtmService.clearCurrentAuctionClaim(); // <-- ADD THIS

                AuctionConfigEntity config = auctionConfigService.getConfig();

                config.setAuctionPhase(AuctionPhase.SOLD);

                auctionConfigService.save(config);

                auctionEventService.logEvent(
                                "SOLD",
                                currentAuctionService.getCurrentAuction().getCurrentPlayer(),
                                currentAuctionService.getCurrentAuction().getLeader(),
                                currentAuctionService.getCurrentAuction().getCurrentBid(),
                                "Waiting for RTM / Last Strike");
                auctionSocketService.broadcastRefresh();

                return "Waiting for RTM / Last Strike.";
        }

        public String acceptRtm(
                        String captainName) {

                RtmEntity claim = rtmService.getCurrentRtm();

                if (claim == null) {

                        return "No active RTM.";

                }

                if (!"BID_SUBMITTED".equals(
                                claim.getStatus())) {

                        return "RTM bid not submitted.";

                }

                if (!claim.getOriginalCaptain().equals(
                                captainName)) {

                        return "Only the original winning captain can accept.";

                }
                String player = claim.getPlayerName();
                String winner = claim.getOriginalCaptain();
                String rtmCaptain = claim.getCaptainName();
                int bid = claim.getBidAmount();

                claim.setUsed(true);
                rtmService.save(claim);

                String result = sellPlayer(
                                player,
                                winner,
                                bid);

                auctionEventService.logEvent(
                                "RTM_ACCEPTED",
                                player,
                                winner,
                                bid,
                                "Matched RTM bid from " + rtmCaptain);

                return result;

        }

        public String declineRtm(
                        String captainName) {

                RtmEntity claim = rtmService.getCurrentRtm();

                if (claim == null) {

                        return "No active RTM.";

                }

                if (!"BID_SUBMITTED".equals(
                                claim.getStatus())) {

                        return "RTM bid not submitted.";

                }

                if (!claim.getOriginalCaptain().equals(
                                captainName)) {

                        return "Only the original winning captain can decline.";

                }
                claim.setUsed(true);
                rtmService.save(claim);
                String result = sellPlayer(
                                claim.getPlayerName(),
                                claim.getCaptainName(), // RTM captain ✅
                                claim.getBidAmount());

                auctionEventService.logEvent(
                                "RTM_DECLINED",
                                claim.getPlayerName(),
                                claim.getCaptainName(),
                                claim.getBidAmount(),
                                "Original captain declined to match");

                return result;

        }

        public synchronized String updateCurrentAuction(
                        String captainName,
                        Integer currentBid) {

                Auction auction = currentAuctionService.getCurrentAuction();

                if (auction == null ||
                                auction.getCurrentPlayer().isBlank()) {

                        return "No active auction.";
                }

                if (currentBid == null || currentBid <= 0) {
                        return "A positive bid is required.";
                }

                TeamEntity team = teamService.getTeam(captainName);
                if (team == null) {
                        return "Captain not found.";
                }

                if (team.getPlayersLeft() <= 0) {
                        return "Squad is already complete.";
                }

                if ("None".equals(auction.getLeader())) {
                        if (!auction.getNominatedBy().equalsIgnoreCase(captainName)) {
                                return "Only the nominating captain can place the opening bid.";
                        }
                        if (currentBid != auction.getBasePrice()) {
                                return "The opening bid must equal the base price (₹"
                                                + auction.getBasePrice() + ").";
                        }
                } else {
                        int minimumIncrement = auction.getCurrentBid() <= 1000 ? 50 : 100;
                        if (currentBid < auction.getCurrentBid() + minimumIncrement) {
                                return "Minimum bid is ₹" + (auction.getCurrentBid() + minimumIncrement) + ".";
                        }
                }

                int maxBid = teamService.getMaxBid(team);
                if (currentBid > maxBid) {
                        return "Bid exceeds maximum bid (₹" + maxBid + ").";
                }

                auction.setLeader(captainName);
                auction.setCurrentBid(currentBid);

                auctionSocketService.broadcastRefresh();

                return "Updated";
        }

}
