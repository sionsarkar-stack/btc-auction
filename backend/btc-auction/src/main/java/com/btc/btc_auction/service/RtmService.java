package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.RtmRepository;
import jakarta.transaction.Transactional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RtmService {

    private final RtmRepository repository;

    private final AuctionEventService auctionEventService;

    private final CurrentAuctionService currentAuctionService;

    private final AuctionConfigService auctionConfigService;

    private final AuctionSocketService auctionSocketService;

    private final TeamService teamService;

    public RtmService(
            RtmRepository repository,
            CurrentAuctionService currentAuctionService,
            AuctionEventService auctionEventService,
            AuctionConfigService auctionConfigService, AuctionSocketService auctionSocketService,
            TeamService teamService) {

        this.repository = repository;
        this.currentAuctionService = currentAuctionService;
        this.auctionEventService = auctionEventService;
        this.auctionConfigService = auctionConfigService;
        this.auctionSocketService = auctionSocketService;
        this.teamService = teamService;
    }

    @Transactional
    public synchronized String claimRtm(
            String captainName) {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.getAuctionPhase() != AuctionPhase.SOLD) {

            return "RTM is only available after SOLD is called.";

        }

        if (repository.existsByCaptainNameAndUsedTrue(
                captainName)) {

            return "RTM already used.";

        }

        Auction auction = currentAuctionService.getCurrentAuction();

        if (auction == null ||
                auction.getCurrentPlayer().isBlank()) {

            return "No active auction.";

        }

        if (auction.getLeader().equalsIgnoreCase(captainName)) {
            return "The current highest bidder cannot use RTM on this player.";
        }

        if (repository.findByPlayerName(
                auction.getCurrentPlayer()).isPresent()) {

            return "RTM already claimed.";

        }

        RtmEntity rtm = new RtmEntity();

        rtm.setCaptainName(
                captainName);

        rtm.setPlayerName(
                auction.getCurrentPlayer());

        rtm.setOriginalBidAmount(
                auction.getCurrentBid());

        rtm.setBidAmount(
                auction.getCurrentBid());

        rtm.setClaimedAt(
                LocalDateTime.now());

        rtm.setUsed(false);

        rtm.setOriginalCaptain(
                auction.getLeader());

        rtm.setStatus("CLAIMED");
        repository.save(rtm);

        auctionEventService.logEvent(
                "RTM_TRIGGERED",
                auction.getCurrentPlayer(),
                captainName,
                auction.getCurrentBid(),
                "Original Bid ₹" + auction.getCurrentBid());

        auctionSocketService.broadcastRefresh();
        return "RTM claimed.";
    }

    public RtmEntity getCurrentRtm() {

        Auction auction = currentAuctionService.getCurrentAuction();

        if (auction == null ||
                auction.getCurrentPlayer().isBlank()) {

            return null;

        }

        return repository.findByPlayerName(
                auction.getCurrentPlayer())
                .orElse(null);

    }

    public String confirmRtm() {

        AuctionConfigEntity config = auctionConfigService.getConfig();

        if (config.getAuctionPhase() != AuctionPhase.SOLD) {

            return "RTM window closed.";

        }

        RtmEntity claim = getCurrentRtm();

        if (claim == null) {

            return "No RTM claim.";

        }

        Auction auction = currentAuctionService.getCurrentAuction();

        if (auction == null ||
                auction.getCurrentPlayer().isBlank()) {

            return "No active auction.";

        }

        auction.setLeader(
                claim.getCaptainName());

        config.setAuctionPhase(
                AuctionPhase.BIDDING);

        auctionConfigService.save(config);

        auctionEventService.logEvent(
                "RTM_CONFIRMED",
                auction.getCurrentPlayer(),
                claim.getCaptainName(),
                auction.getCurrentBid(),
                "Right To Match Activated");

        claim.setUsed(true);
        repository.save(claim);

        auctionSocketService.broadcastRefresh();
        return "RTM Confirmed.";

    }

    public String cancelRtm() {

        Auction auction = currentAuctionService.getCurrentAuction();

        if (auction != null &&
                !auction.getCurrentPlayer().isBlank()) {

            repository.deleteByPlayerName(
                    auction.getCurrentPlayer());

        }
        auctionSocketService.broadcastRefresh();
        return "RTM Cancelled.";

    }

    @Transactional
    public void clearCurrentAuctionClaim() {

        Auction auction = currentAuctionService.getCurrentAuction();

        if (auction == null || auction.getCurrentPlayer().isBlank()) {
            return;
        }

        RtmEntity claim = repository
                .findByPlayerName(auction.getCurrentPlayer())
                .orElse(null);

        if (claim != null) {

            claim.setStatus("COMPLETED");
            claim.setPlayerName("");

            repository.save(claim);
        }
    }

    public void clear() {

        repository.deleteAll();

    }

    public String submitBid(
            Integer bidAmount) {

        RtmEntity claim = getCurrentRtm();

        if (claim == null) {

            return "No active RTM.";

        }

        Auction auction = currentAuctionService.getCurrentAuction();
        TeamEntity team = teamService.getTeam(
                claim.getCaptainName());

        if (team == null) {

            return "Captain not found.";

        }

        int maxBid = teamService.getMaxBid(team);

        if (bidAmount > maxBid) {

            return "RTM bid exceeds maximum bid (₹"
                    + maxBid + ").";

        }

        if (auction == null) {

            return "No active auction.";

        }

        claim.setBidAmount(
                bidAmount);

        claim.setStatus(
                "BID_SUBMITTED");

        repository.save(claim);

        auctionEventService.logEvent(
                "RTM_BID_SUBMITTED",
                auction.getCurrentPlayer(),
                claim.getCaptainName(),
                bidAmount,
                "Original Bid ₹"
                        + claim.getOriginalBidAmount()
                        + " | RTM Bid ₹"
                        + bidAmount);

        auctionSocketService.broadcastRefresh();

        return "RTM bid submitted.";

    }

    public RtmEntity save(@NonNull RtmEntity rtm) {
        return repository.save(rtm);
    }

}
