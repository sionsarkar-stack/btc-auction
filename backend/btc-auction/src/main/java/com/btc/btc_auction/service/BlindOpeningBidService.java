package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BlindOpeningBidService {

    private static final int NORMAL_BLIND_BONUS_POINTS = 300;
    private static final int BLIND_BONANZA_POINTS = 500;
    private static final int MAX_BLIND_BONUS_AWARDS = 4;

    private final BlindOpeningBidRepository repository;
    private final TeamService teamService;
    private final CurrentAuctionService currentAuctionService;
    private final AuctionSocketService auctionSocketService;
    private final AuctionEventService auctionEventService;
    private final AuctionConfigService auctionConfigService;
    private BidAffordabilityService bidAffordabilityService;

    public BlindOpeningBidService(
            BlindOpeningBidRepository repository,
            TeamService teamService,
            CurrentAuctionService currentAuctionService,
            AuctionSocketService auctionSocketService,
            AuctionEventService auctionEventService) {
        this(repository, teamService, currentAuctionService, auctionSocketService, auctionEventService, null);
    }

    @Autowired
    public BlindOpeningBidService(
            BlindOpeningBidRepository repository, TeamService teamService,
            CurrentAuctionService currentAuctionService, AuctionSocketService auctionSocketService,
            AuctionEventService auctionEventService, AuctionConfigService auctionConfigService,
            BidAffordabilityService bidAffordabilityService) {
        this(repository, teamService, currentAuctionService, auctionSocketService,
                auctionEventService, auctionConfigService);
        this.bidAffordabilityService = bidAffordabilityService;
    }

    public BlindOpeningBidService(
            BlindOpeningBidRepository repository,
            TeamService teamService,
            CurrentAuctionService currentAuctionService,
            AuctionSocketService auctionSocketService,
            AuctionEventService auctionEventService,
            AuctionConfigService auctionConfigService) {
        this.repository = repository;
        this.teamService = teamService;
        this.currentAuctionService = currentAuctionService;
        this.auctionSocketService = auctionSocketService;
        this.auctionEventService = auctionEventService;
        this.auctionConfigService = auctionConfigService;
    }

    public void startRound(String playerName) {
        repository.deleteByPlayerName(playerName);

        teamService.getAllTeams().forEach(team -> {
            BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
            bid.setPlayerName(playerName);
            bid.setCaptainName(team.getCaptainName());
            bid.setBidAmount(0);
            bid.setSubmitted(false);
            bid.setPassed(false);
            repository.save(bid);
        });

        auctionSocketService.broadcastRefresh();
    }

    public String submitBid(String playerName, String captainName, int amount) {
        if (amount <= 0) {
            return "Bid must be greater than zero.";
        }

        if (!teamService.isValidBidIncrement(amount)) {
            return "Bid must be a multiple of 50 up to ₹1000, then a multiple of 100.";
        }

        TeamEntity team = teamService.getTeam(captainName);
        if (team == null) {
            return "Captain not found.";
        }

        if (bidAffordabilityService != null) {
            String error = bidAffordabilityService.validate(playerName, captainName, amount, true);
            if (error != null) {
                return error;
            }
        }

        BlindOpeningBidEntity bid = repository.findByPlayerNameAndCaptainName(playerName, captainName).orElse(null);
        if (bid == null) {
            return "Blind opening bid not found.";
        }

        bid.setBidAmount(amount);
        bid.setSubmitted(true);
        bid.setPassed(false);
        repository.save(bid);

        auctionEventService.logEvent(
                "STARTING_BID_SUBMITTED",
                playerName,
                captainName,
                amount,
                "Starting bid submitted privately");

        auctionSocketService.broadcastRefresh();
        return "Blind opening bid submitted.";
    }

    public String passBid(String playerName, String captainName) {
        TeamEntity team = teamService.getTeam(captainName);
        if (team == null) {
            return "Captain not found.";
        }

        BlindOpeningBidEntity bid = repository.findByPlayerNameAndCaptainName(playerName, captainName).orElse(null);
        if (bid == null) {
            return "Blind opening bid not found.";
        }

        bid.setBidAmount(0);
        bid.setPassed(true);
        bid.setSubmitted(true);
        repository.save(bid);

        auctionEventService.logEvent(
                "STARTING_BID_PASSED",
                playerName,
                captainName,
                0,
                "Captain passed on the starting bid");

        auctionSocketService.broadcastRefresh();
        return "Blind opening bid passed.";
    }

    public List<BlindOpeningBidEntity> getAllBids(String playerName) {
        return repository.findByPlayerName(playerName);
    }

    public BlindOpeningBidEntity revealWinner(String playerName) {
        List<BlindOpeningBidEntity> bids = repository.findByPlayerName(playerName);
        if (bids.isEmpty()) {
            return null;
        }

        boolean allSubmitted = bids.stream().allMatch(bid -> bid != null && bid.isSubmitted());
        if (!allSubmitted) {
            return null;
        }

        BlindOpeningBidEntity winner = bids.stream()
                .filter(bid -> !bid.isPassed() && bid.getBidAmount() != null && bid.getBidAmount() > 0)
                .max(Comparator.comparingInt(bid -> bid.getBidAmount()))
                .orElse(null);

        if (winner == null) {
            bids.forEach(bid -> {
                if (bid.isBonusEligible()) {
                    bid.setBonusEligible(false);
                    repository.save(bid);
                }
            });
            auctionEventService.logEvent(
                    "STARTING_BID_ALL_PASSED",
                    playerName,
                    null,
                    0,
                    "All captains passed on the starting bid. Player is ready to be marked unsold.");
            return null;
        }

        int highestBidAmount = winner.getBidAmount();
        String highestBidCaptains = bids.stream()
                .filter(bid -> !bid.isPassed()
                        && Objects.equals(bid.getBidAmount(), highestBidAmount))
                .map(bid -> bid.getCaptainName())
                .collect(Collectors.joining(", "));

        long tiedCaptainCount = bids.stream()
                .filter(bid -> !bid.isPassed()
                        && Objects.equals(bid.getBidAmount(), highestBidAmount))
                .count();
        if (tiedCaptainCount > 1) {
            bids.stream()
                    .filter(bid -> !bid.isPassed()
                            && Objects.equals(bid.getBidAmount(), highestBidAmount))
                    .forEach(bid -> {
                        bid.setBidAmount(0);
                        bid.setPassed(false);
                        bid.setSubmitted(false);
                        repository.save(bid);
                    });

            auctionEventService.logEvent(
                    "STARTING_BID_TIE",
                    playerName,
                    highestBidCaptains,
                    highestBidAmount,
                    "Tie declared at ₹" + highestBidAmount
                            + ". Tied captains must submit again or pass.");
            auctionSocketService.broadcastRefresh();
            return null;
        }

        bids.forEach(bid -> {
            boolean eligible = bid.getCaptainName().equalsIgnoreCase(winner.getCaptainName());
            if (bid.isBonusEligible() != eligible) {
                bid.setBonusEligible(eligible);
                repository.save(bid);
            }
        });

        Auction auction = currentAuctionService.getCurrentAuction();
        if (auction != null) {
            auction.setLeader(winner.getCaptainName());
            auction.setCurrentBid(winner.getBidAmount());
            currentAuctionService.setCurrentAuction(auction);
        }

        auctionEventService.logEvent(
                "STARTING_BID_WINNER",
                playerName,
                highestBidCaptains,
                winner.getBidAmount(),
                "Highest starting bid: " + highestBidCaptains
                        + " at ₹" + winner.getBidAmount());

        auctionSocketService.broadcastRefresh();
        return winner;
    }

    public int awardBonusIfEligible(String playerName, String finalBuyer) {
        return awardBonusIfEligibleForSale(List.of(playerName), finalBuyer);
    }

    public int awardBonusIfEligibleForSale(List<String> playerNames, String finalBuyer) {
        var config = auctionConfigService == null ? null : auctionConfigService.getConfig();

        List<String> salePlayerNames = playerNames.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<BlindOpeningBidEntity> saleBids = salePlayerNames.stream()
                .flatMap(playerName -> repository.findByPlayerName(playerName).stream())
                .toList();

        if (saleBids.stream().anyMatch(BlindOpeningBidEntity::isBonusAwarded)) {
            return 0;
        }

        BlindOpeningBidEntity eligibleBid = saleBids.stream()
                .filter(BlindOpeningBidEntity::isBonusEligible)
                .filter(bid -> bid.getCaptainName().equalsIgnoreCase(finalBuyer))
                .sorted(Comparator.comparing(
                        (BlindOpeningBidEntity bid) -> isBlindBonanzaActive(config, bid.getPlayerName()))
                        .reversed())
                .findFirst()
                .orElse(null);
        if (eligibleBid == null) {
            return 0;
        }

        TeamEntity team = teamService.getTeam(finalBuyer);
        if (team == null) {
            return 0;
        }

        String bonusPlayerName = eligibleBid.getPlayerName();
        boolean blindBonanzaActive = isBlindBonanzaActive(config, bonusPlayerName);
        int bonusAmount = blindBonanzaActive ? BLIND_BONANZA_POINTS : NORMAL_BLIND_BONUS_POINTS;

        if (blindBonanzaActive) {
            config.setBlindBonanzaAwardsUsed(config.getBlindBonanzaAwardsUsed() + 1);
            auctionConfigService.save(config);
        }
        eligibleBid.setBonusAwarded(true);
        eligibleBid.setBonusAwardedAmount(bonusAmount);
        repository.save(eligibleBid);
        String eventType = blindBonanzaActive ? "BLIND_BONANZA_REWARDED" : "BLIND_BID_BONUS";
        String eventDetails = blindBonanzaActive
                ? "Blind Bonanza reduced the effective price by ₹500."
                : "Highest valid blind bidder won the player; effective price reduced by ₹300.";
        auctionEventService.logEvent(
                eventType,
                bonusPlayerName,
                finalBuyer,
                bonusAmount,
                eventDetails);
        auctionSocketService.broadcastRefresh();
        return bonusAmount;
    }

    private boolean isBlindBonanzaActive(AuctionConfigEntity config, String playerName) {
        return config != null
                && config.getPendingRandomEventType() == com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA
                && playerName.equalsIgnoreCase(config.getPendingRandomEventPlayer())
                && config.getBlindBonanzaAwardsUsed() < MAX_BLIND_BONUS_AWARDS;
    }

    public void reverseBonusIfAwarded(String playerName) {
        BlindOpeningBidEntity awardedBid = repository.findByPlayerName(playerName).stream()
                .filter(BlindOpeningBidEntity::isBonusAwarded)
                .findFirst()
                .orElse(null);
        if (awardedBid == null) {
            return;
        }

        int bonusAmount = awardedBid.getBonusAwardedAmount() > 0
                ? awardedBid.getBonusAwardedAmount()
                : BLIND_BONANZA_POINTS;
        if (bonusAmount == BLIND_BONANZA_POINTS && auctionConfigService != null) {
            var config = auctionConfigService.getConfig();
            if (config != null) {
                config.setBlindBonanzaAwardsUsed(Math.max(0, config.getBlindBonanzaAwardsUsed() - 1));
                auctionConfigService.save(config);
            }
        }
        awardedBid.setBonusAwarded(false);
        awardedBid.setBonusAwardedAmount(0);
        repository.save(awardedBid);
    }

    public void clearAll() {
        repository.deleteAll();
    }

    public boolean allCaptainsPassed(String playerName) {
        List<BlindOpeningBidEntity> bids = repository.findByPlayerName(playerName);
        return !bids.isEmpty()
                && bids.stream().allMatch(bid -> bid != null && bid.isSubmitted() && bid.isPassed());
    }

    public void clearRound(String playerName) {
        repository.findByPlayerName(playerName).forEach(repository::delete);
        auctionSocketService.broadcastRefresh();
    }

}
