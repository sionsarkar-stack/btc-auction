package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.SilentBidEntity;
import com.btc.btc_auction.model.SilentBidResult;
import com.btc.btc_auction.repository.SilentBidRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SilentBidService {

    private final SilentBidRepository repository;

    private final TeamService teamService;

    private final AuctionService auctionService;

    private final AuctionEventService auctionEventService;

    public SilentBidService(
            SilentBidRepository repository,
            TeamService teamService, AuctionService auctionService,

            AuctionEventService auctionEventService) {

        this.repository = repository;
        this.teamService = teamService;
        this.auctionService = auctionService;

        this.auctionEventService = auctionEventService;

    }

    public void startRound(
            String playerName) {

        repository.deleteAll();

        teamService.getAllTeams()

                .forEach(team -> {

                    SilentBidEntity bid = new SilentBidEntity();
                    bid.setTieBreakRound(false);

                    bid.setPlayerName(playerName);

                    bid.setCaptainName(
                            team.getCaptainName());

                    bid.setBidAmount(0);

                    bid.setSubmitted(false);
                    bid.setEligibleForTieBreak(true);

                    repository.save(bid);

                });

    }

    public String submitBid(
            String playerName,
            String captainName,
            int amount) {

        SilentBidEntity bid = repository.findByPlayerNameAndCaptainName(
                playerName,
                captainName)
                .orElse(null);

        if (bid == null) {

            return "Silent bid not found.";

        }

        if (!bid.isEligibleForTieBreak()) {

            return "You are eliminated from this silent bidding round.";

        }

        if (bid.isSubmitted()) {

            return "Bid already submitted.";

        }

        bid.setBidAmount(amount);
        bid.setSubmitted(true);

        repository.save(bid);

        return "Bid submitted.";

    }

    public List<SilentBidEntity> getAllBids() {

        return repository.findAll();

    }

    @SuppressWarnings("null")
    public SilentBidEntity getWinner() {

        List<SilentBidEntity> bids = repository.findAll();

        if (bids.isEmpty()) {

            return null;

        }

        // Don't reveal until every captain has submitted
        boolean allSubmitted = bids.stream()
                .allMatch(
                        SilentBidEntity::isSubmitted);

        if (!allSubmitted) {

            return null;

        }

        return bids.stream()

                .max(
                        Comparator.comparingInt(
                                SilentBidEntity::getBidAmount))

                .orElse(null);

    }

    public void clearRound() {

        repository.deleteAll();

    }

    public SilentBidEntity getCaptainBid(
            String captainName) {

        return repository
                .findByCaptainName(captainName)
                .orElse(null);

    }

    public boolean isRoundActive() {

        return repository.count() > 0;

    }

    public String sellWinner() {

        SilentBidResult bidResult = getResult();

        if (bidResult.isTie()) {

            return "Tie still exists.";

        }

        SilentBidEntity winner = bidResult.getWinner();
        if (winner == null) {

            return "Winner not available.";

        }

        String saleResult = auctionService.sellPlayer(
                winner.getPlayerName(),
                winner.getCaptainName(),
                winner.getBidAmount());

        auctionEventService.logEvent(
                "SILENT_BID_SOLD",
                winner.getPlayerName(),
                winner.getCaptainName(),
                winner.getBidAmount(),
                "Silent Bid Winner");

        repository.deleteAll();

        return saleResult;
    }

    @SuppressWarnings("null")
    public SilentBidResult getResult() {

        List<SilentBidEntity> bids = repository.findAll();

        SilentBidResult result = new SilentBidResult();

        if (bids.isEmpty()) {

            return result;

        }

        boolean allSubmitted = bids.stream()
                .allMatch(SilentBidEntity::isSubmitted);

        if (!allSubmitted) {

            return result;

        }

        int highest = bids.stream()
                .mapToInt(SilentBidEntity::getBidAmount)
                .max()
                .orElse(0);

        List<SilentBidEntity> highestBidders = bids.stream()
                .filter(b -> b.getBidAmount() == highest)
                .toList();

        if (highestBidders.size() == 1) {

            result.setTie(false);
            result.setWinner(highestBidders.get(0));

            return result;

        }

        result.setTie(true);

        List<String> tiedCaptains = highestBidders.stream()

                .map(SilentBidEntity::getCaptainName)

                .toList();

        result.setTiedCaptains(
                tiedCaptains);

        // Prepare next tie-break round
        for (SilentBidEntity bid : bids) {

            if (tiedCaptains.contains(
                    bid.getCaptainName())) {
                bid.setTieBreakRound(true);
                bid.setSubmitted(false);
                bid.setEligibleForTieBreak(true);

                bid.setBidAmount(0);

            } else {
                bid.setTieBreakRound(true);
                bid.setEligibleForTieBreak(false);

            }

            repository.save(bid);

        }

        return result;

    }
}