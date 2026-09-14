package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BlindOpeningBidService {

    private final BlindOpeningBidRepository repository;
    private final TeamService teamService;
    private final CurrentAuctionService currentAuctionService;
    private final AuctionSocketService auctionSocketService;
    private final AuctionEventService auctionEventService;

    public BlindOpeningBidService(
            BlindOpeningBidRepository repository,
            TeamService teamService,
            CurrentAuctionService currentAuctionService,
            AuctionSocketService auctionSocketService,
            AuctionEventService auctionEventService) {
        this.repository = repository;
        this.teamService = teamService;
        this.currentAuctionService = currentAuctionService;
        this.auctionSocketService = auctionSocketService;
        this.auctionEventService = auctionEventService;
    }

    public void startRound(String playerName) {
        repository.deleteAll();

        teamService.getAllTeams().forEach(team -> {
            BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
            bid.setPlayerName(playerName);
            bid.setCaptainName(team.getCaptainName());
            bid.setBidAmount(0);
            bid.setSubmitted(false);
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

        BlindOpeningBidEntity bid = repository.findByPlayerNameAndCaptainName(playerName, captainName).orElse(null);
        if (bid == null) {
            return "Blind opening bid not found.";
        }

        int maxBid = teamService.getMaxBid(team);
        if (amount > maxBid) {
            return "Bid exceeds the captain's maximum bid (₹" + maxBid + ").";
        }

        bid.setBidAmount(amount);
        bid.setSubmitted(true);
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
                .max(Comparator.comparingInt(bid -> bid.getBidAmount()))
                .orElse(null);

        if (winner == null) {
            return null;
        }

        String highestBidCaptains = bids.stream()
                .filter(bid -> Objects.equals(bid.getBidAmount(), winner.getBidAmount()))
                .map(bid -> bid.getCaptainName())
                .collect(Collectors.joining(", "));

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

    public void clearRound(String playerName) {
        repository.findByPlayerName(playerName).forEach(repository::delete);
        auctionSocketService.broadcastRefresh();
    }
}
