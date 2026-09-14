package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.entity.ValueBetEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.ValueBetRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValueBetService {

    private static final int VALUE_BET_REWARD = 200;

    private final ValueBetRepository repository;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final AuctionConfigService auctionConfigService;
    private final CurrentAuctionService currentAuctionService;
    private final AuctionEventService auctionEventService;
    private final AdminActionLogService adminActionLogService;

    public ValueBetService(
            ValueBetRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionConfigService auctionConfigService,
            CurrentAuctionService currentAuctionService,
            AuctionEventService auctionEventService) {
        this(repository, teamService, playerService, auctionConfigService,
                currentAuctionService, auctionEventService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ValueBetService(
            ValueBetRepository repository,
            TeamService teamService,
            PlayerService playerService,
            AuctionConfigService auctionConfigService,
            CurrentAuctionService currentAuctionService,
            AuctionEventService auctionEventService,
            AdminActionLogService adminActionLogService) {
        this.repository = repository;
        this.teamService = teamService;
        this.playerService = playerService;
        this.auctionConfigService = auctionConfigService;
        this.currentAuctionService = currentAuctionService;
        this.auctionEventService = auctionEventService;
        this.adminActionLogService = adminActionLogService;
    }

    public synchronized String submitPrediction(String playerName, String captainName, int predictedPrice) {
        if (predictedPrice <= 0) {
            return "Prediction must be greater than zero.";
        }

        if (!teamService.isValidBidIncrement(predictedPrice)) {
            return "Prediction must be a multiple of 50 up to ₹1000, then a multiple of 100.";
        }

        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (config == null || (config.getAuctionPhase() != AuctionPhase.OPENING_BID
                && config.getAuctionPhase() != AuctionPhase.BLIND_OPENING_BID)
                || config.getValueBetPlayer() == null
                || !config.getValueBetPlayer().equalsIgnoreCase(playerName)) {
            return "Value Bet event is not active for this player.";
        }

        Auction auction = currentAuctionService.getCurrentAuction();
        if (auction == null || !auction.getCurrentPlayer().equalsIgnoreCase(playerName)) {
            return "Player is not the current auction player.";
        }

        if (playerService.getPlayer(playerName) == null) {
            return "Player not found.";
        }

        if (teamService.getTeam(captainName) == null) {
            return "Captain not found.";
        }

        if (repository.findByPlayerNameAndCaptainName(playerName, captainName).isPresent()) {
            return "Value Bet already submitted for this player.";
        }

        ValueBetEntity bet = new ValueBetEntity();
        bet.setPlayerName(playerName);
        bet.setCaptainName(captainName);
        bet.setPredictedPrice(predictedPrice);
        bet.setRewarded(false);
        repository.save(bet);
        if (adminActionLogService != null) {
            adminActionLogService.addLog(
                    "VALUE_BET_SUBMITTED",
                    playerName,
                    "",
                    captainName,
                    predictedPrice,
                    predictedPrice,
                    "Private final-price prediction submitted");
        }
        return "Value Bet submitted.";
    }

    @Transactional
    public synchronized int applyRewards(String playerName, int finalPrice) {
        List<ValueBetEntity> bets = repository.findByPlayerName(playerName);
        for (ValueBetEntity bet : bets) {
            if (bet.isRewarded()) {
                return 0;
            }
        }

        ValueBetEntity closestBet = null;
        int closestDifference = Integer.MAX_VALUE;
        long earliestBetId = Long.MAX_VALUE;

        for (ValueBetEntity bet : bets) {
            int difference = Math.abs(bet.getPredictedPrice() - finalPrice);
            long betId = bet.getId() == null ? Long.MAX_VALUE : bet.getId();
            if (difference > closestDifference
                    || (difference == closestDifference && betId >= earliestBetId)) {
                continue;
            }

            closestBet = bet;
            closestDifference = difference;
            earliestBetId = betId;
        }

        int rewardCount = 0;
        if (closestBet != null) {
            TeamEntity team = teamService.getTeam(closestBet.getCaptainName());
            if (team != null) {
                team.setPurse(team.getPurse() + VALUE_BET_REWARD);
                teamService.saveTeam(team);
                closestBet.setRewarded(true);
                repository.save(closestBet);
                auctionEventService.logEvent(
                        "VALUE_BET_REWARD",
                        playerName,
                        closestBet.getCaptainName(),
                        VALUE_BET_REWARD,
                        "Prediction ₹" + closestBet.getPredictedPrice() + " was closest to final price ₹" + finalPrice);
                if (adminActionLogService != null) {
                    adminActionLogService.addLog(
                            "VALUE_BET_REWARD",
                            playerName,
                            "",
                            closestBet.getCaptainName(),
                            closestBet.getPredictedPrice(),
                            VALUE_BET_REWARD,
                            "Closest prediction to final price ₹" + finalPrice + "; ₹200 awarded");
                }
                rewardCount++;
            }
        }
        AuctionConfigEntity config = auctionConfigService.getConfig();
        if (playerName.equalsIgnoreCase(config.getValueBetPlayer())) {
            config.setValueBetPlayer(null);
            auctionConfigService.save(config);
        }
        return rewardCount;
    }

    public void clearAll() {
        repository.deleteAll();
    }
}