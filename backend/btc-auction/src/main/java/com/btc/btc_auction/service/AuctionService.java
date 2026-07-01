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
import com.btc.btc_auction.entity.ForbiddenPickEntity;

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
        private final ForbiddenPickService forbiddenPickService;
        private final TribunalVoteService tribunalVoteService;
        private final ReverseTargetService reverseTargetService;
        private final RtmService rtmService;
        private final TrustedCaptainService trustedCaptainService;
        private final AuctionSocketService auctionSocketService;

        private final JokerService jokerService;

        public AuctionService(
                        TeamService teamService,
                        PlayerService playerService,
                        AuctionLogService auctionLogService,
                        AdminActionLogService adminActionLogService,
                        AuctionEventService auctionEventService,
                        AuctionConfigService auctionConfigService,
                        BountyPlayerService bountyPlayerService,
                        ForbiddenPickService forbiddenPickService,
                        TribunalVoteService tribunalVoteService,
                        TrustedCaptainService trustedCaptainService,
                        JokerService jokerService, ReverseTargetService reverseTargetService,
                        RtmService rtmService, AuctionSocketService auctionSocketService,
                        CurrentAuctionService currentAuctionService) {

                this.teamService = teamService;
                this.playerService = playerService;
                this.auctionLogService = auctionLogService;
                this.adminActionLogService = adminActionLogService;
                this.auctionEventService = auctionEventService;
                this.auctionConfigService = auctionConfigService;
                this.bountyPlayerService = bountyPlayerService;
                this.forbiddenPickService = forbiddenPickService;
                this.tribunalVoteService = tribunalVoteService;
                this.trustedCaptainService = trustedCaptainService;
                this.reverseTargetService = reverseTargetService;
                this.jokerService = jokerService;
                this.rtmService = rtmService;
                this.currentAuctionService = currentAuctionService;
                this.auctionSocketService = auctionSocketService;
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

        public String nominatePlayer(
                        String playerName,
                        String seed,

                        String nominatedBy) {
                if (!currentAuctionService
                                .getCurrentAuction()
                                .getCurrentPlayer()
                                .isBlank()) {

                        return "Auction already in progress";
                }

                int basePrice = switch (seed.trim().toLowerCase()) {

                        case "hackers" -> 800;

                        case "developers" -> 600;

                        case "new joiners" -> 100;

                        case "interns" -> 50;

                        default -> 100;
                };

                PlayerEntity player = playerService.getPlayer(
                                playerName);

                if (player == null) {
                        return "Player not found";
                }

                if (player.isSold()) {
                        return "Player already sold";
                }

                currentAuctionService.setCurrentAuction(new Auction(
                                playerName,
                                seed,
                                basePrice,
                                "None",
                                basePrice, nominatedBy));
                AuctionConfigEntity config = auctionConfigService.getConfig();

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

        public String sellPlayer(
                        String playerName,
                        String captainName,
                        int soldPrice) {

                TeamEntity team = teamService.getTeam(captainName);
                ForbiddenPickEntity forbidden = forbiddenPickService.getByCaptain(
                                captainName);

                if (forbidden != null &&
                                forbidden.getPlayerName()
                                                .equalsIgnoreCase(
                                                                playerName)) {

                        return captainName +
                                        " cannot purchase " +
                                        playerName +
                                        " (Forbidden Pick)";
                }

                int penalty = 0;

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

                                TeamEntity rewardTeam = teamService.getTeam(
                                                target.getCaptainName());

                                if (rewardTeam != null) {

                                        rewardTeam.setPurse(
                                                        rewardTeam.getPurse() + 300);

                                        teamService.saveTeam(rewardTeam);

                                        auctionEventService.logEvent(
                                                        "REVERSE_TARGET_TRIGGERED",
                                                        playerName,
                                                        target.getCaptainName(),
                                                        300,
                                                        target.getCaptainName()
                                                                        + " predicted "
                                                                        + captainName
                                                                        + " buying "
                                                                        + playerName);

                                }

                        }

                }
                AuctionConfigEntity config = auctionConfigService.getConfig();

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

                teamService.getAllTeams()
                                .forEach(team -> {

                                        switch (team.getCaptainName()) {

                                                case "Joy":
                                                        team.setPurse(10000);
                                                        break;

                                                case "Rimo":
                                                        team.setPurse(10000);
                                                        break;

                                                case "Sujay":
                                                        team.setPurse(9400);
                                                        break;

                                                case "Dragleeoo":
                                                        team.setPurse(9200);
                                                        break;
                                        }

                                        team.setPlayersBought(0);

                                        team.setPlayersLeft(
                                                        config.getSquadSize());

                                        teamService.saveTeam(team);
                                });

                bountyPlayerService.getAll()
                                .forEach(bounty -> {

                                        bounty.setRevealed(false);

                                        bountyPlayerService.save(
                                                        bounty);
                                });

                auctionLogService.clearLogs();

                auctionEventService.clearEvents();

                bountyPlayerService.deleteAll();

                tribunalVoteService.deleteAll();

                trustedCaptainService.deleteAll();

                forbiddenPickService.deleteAll();

                jokerService.deleteAll();

                reverseTargetService.deleteAll();

                rtmService.clear();

                auctionSocketService.broadcastRefresh();

                return "Auction Reset Successfully";
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

                        return "Cannot call SOLD before any captain has bid.";

                }
                rtmService.clearCurrentAuctionClaim(); // <-- ADD THIS

                AuctionConfigEntity config = auctionConfigService.getConfig();

                config.setAuctionPhase(AuctionPhase.SOLD);

                auctionConfigService.save(config);

                Auction auction = currentAuctionService.getCurrentAuction();

                System.out.println("Leader = " + auction.getLeader());
                System.out.println("Bid = " + auction.getCurrentBid());
                System.out.println("Player = " + auction.getCurrentPlayer());

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

        public String updateCurrentAuction(
                        String captainName,
                        Integer currentBid) {

                Auction auction = currentAuctionService.getCurrentAuction();

                if (auction == null ||
                                auction.getCurrentPlayer().isBlank()) {

                        return "No active auction.";
                }

                auction.setLeader(captainName);
                auction.setCurrentBid(currentBid);

                auctionSocketService.broadcastRefresh();

                return "Updated";
        }

}