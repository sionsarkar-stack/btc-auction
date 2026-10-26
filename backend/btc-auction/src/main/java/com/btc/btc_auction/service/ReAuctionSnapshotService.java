package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.ReAuctionSnapshotEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.repository.AuctionLogRepository;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import com.btc.btc_auction.repository.PlayerRepository;
import com.btc.btc_auction.repository.ReAuctionSnapshotRepository;
import com.btc.btc_auction.repository.RtmRepository;
import com.btc.btc_auction.repository.TeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReAuctionSnapshotService {

    private static final long SNAPSHOT_ID = 1L;

    private final ReAuctionSnapshotRepository snapshotRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final AuctionLogRepository auctionLogRepository;
    private final BlindOpeningBidRepository blindOpeningBidRepository;
    private final CursePlayerRepository cursePlayerRepository;
    private final RtmRepository rtmRepository;
    private final ObjectMapper objectMapper;

    public ReAuctionSnapshotService(
            ReAuctionSnapshotRepository snapshotRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            AuctionLogRepository auctionLogRepository,
            BlindOpeningBidRepository blindOpeningBidRepository,
            CursePlayerRepository cursePlayerRepository,
            RtmRepository rtmRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.auctionLogRepository = auctionLogRepository;
        this.blindOpeningBidRepository = blindOpeningBidRepository;
        this.cursePlayerRepository = cursePlayerRepository;
        this.rtmRepository = rtmRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void capture(int blindBonanzaAwardsUsed) {
        ReAuctionState state = new ReAuctionState(
                teamRepository.findAll().stream()
                        .map(team -> new TeamState(
                                team.getCaptainName(),
                                team.getPurse(),
                                team.getTournamentPoints(),
                                team.getPlayersBought(),
                                team.getPlayersLeft(),
                                team.isWildPickUsed()))
                        .toList(),
                playerRepository.findAll().stream()
                        .map(player -> new PlayerState(
                                player.getName(),
                                player.isSold(),
                                player.getSoldPrice(),
                                player.getFinalPrice(),
                                player.getTeam(),
                                player.isReAuctioned(),
                                player.isDeferredToReAuction()))
                        .toList(),
                auctionLogRepository.findAll().stream()
                        .map(log -> new AuctionLogState(
                                log.getPlayerName(),
                                log.getCaptainName(),
                                log.getSoldPrice()))
                        .toList(),
                cursePlayerRepository.findAll().stream()
                        .map(curse -> new CurseState(
                                curse.getCaptainName(),
                                curse.getPlayerName(),
                                curse.isTriggered(),
                                curse.getSettledBuyer(),
                                curse.getSettledAmount(),
                                curse.getBuyerDeduction()))
                        .toList(),
                blindOpeningBidRepository.findAll().stream()
                        .map(bid -> new BlindOpeningBidState(
                                bid.getPlayerName(),
                                bid.getCaptainName(),
                                bid.getBidAmount(),
                                bid.isSubmitted(),
                                bid.isPassed(),
                                bid.isBonusEligible(),
                                bid.isBonusAwarded(),
                                bid.getBonusAwardedAmount()))
                        .toList(),
                rtmRepository.findAll().stream()
                        .map(rtm -> new RtmState(
                                rtm.getCaptainName(),
                                rtm.getPlayerName(),
                                rtm.getBidAmount(),
                                rtm.getClaimedAt() == null ? null : rtm.getClaimedAt().toString(),
                                rtm.isUsed(),
                                rtm.getRtmBid(),
                                rtm.getOriginalCaptain(),
                                rtm.getStatus(),
                                rtm.getOriginalBidAmount()))
                        .toList(),
                blindBonanzaAwardsUsed);

        ReAuctionSnapshotEntity snapshot = new ReAuctionSnapshotEntity();
        snapshot.setId(SNAPSHOT_ID);
        snapshot.setStateJson(write(state));
        snapshotRepository.save(snapshot);
    }

    public boolean hasSnapshot() {
        return snapshotRepository.existsById(SNAPSHOT_ID);
    }

    @Transactional
    public int restore() {
        ReAuctionSnapshotEntity snapshot = snapshotRepository.findById(SNAPSHOT_ID)
                .orElseThrow(() -> new IllegalStateException("Re-auction snapshot is unavailable."));
        ReAuctionState state = read(snapshot.getStateJson());

        for (TeamState teamState : state.teams()) {
            teamRepository.findByCaptainName(teamState.captainName()).ifPresent(team -> {
                team.setPurse(teamState.purse());
                team.setTournamentPoints(teamState.tournamentPoints());
                team.setPlayersBought(teamState.playersBought());
                team.setPlayersLeft(teamState.playersLeft());
                team.setWildPickUsed(teamState.wildPickUsed());
                teamRepository.save(team);
            });
        }

        for (PlayerState playerState : state.players()) {
            playerRepository.findByName(playerState.name()).ifPresent(player -> {
                player.setSold(playerState.sold());
                player.setSoldPrice(playerState.soldPrice());
                player.setFinalPrice(playerState.finalPrice());
                player.setTeam(playerState.team());
                player.setReAuctioned(playerState.reAuctioned());
                player.setDeferredToReAuction(playerState.deferredToReAuction());
                playerRepository.save(player);
            });
        }

        auctionLogRepository.deleteAll();
        List<AuctionLogEntity> auctionLogs = new ArrayList<>();
        for (AuctionLogState logState : state.auctionLogs()) {
            AuctionLogEntity log = new AuctionLogEntity();
            log.setPlayerName(logState.playerName());
            log.setCaptainName(logState.captainName());
            log.setSoldPrice(logState.soldPrice());
            auctionLogs.add(log);
        }
        auctionLogRepository.saveAll(auctionLogs);

        cursePlayerRepository.deleteAll();
        List<CursePlayerEntity> curses = new ArrayList<>();
        if (state.curses() != null) {
            for (CurseState curseState : state.curses()) {
                CursePlayerEntity curse = new CursePlayerEntity();
                curse.setCaptainName(curseState.captainName());
                curse.setPlayerName(curseState.playerName());
                curse.setTriggered(curseState.triggered());
                curse.setSettledBuyer(curseState.settledBuyer());
                curse.setSettledAmount(curseState.settledAmount());
                curse.setBuyerDeduction(curseState.buyerDeduction());
                curses.add(curse);
            }
        }
        cursePlayerRepository.saveAll(curses);

        blindOpeningBidRepository.deleteAll();
        List<BlindOpeningBidEntity> blindBids = new ArrayList<>();
        if (state.blindOpeningBids() != null) {
            for (BlindOpeningBidState bidState : state.blindOpeningBids()) {
                BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
                bid.setPlayerName(bidState.playerName());
                bid.setCaptainName(bidState.captainName());
                bid.setBidAmount(bidState.bidAmount());
                bid.setSubmitted(bidState.submitted());
                bid.setPassed(bidState.passed());
                bid.setBonusEligible(bidState.bonusEligible());
                bid.setBonusAwarded(bidState.bonusAwarded());
                bid.setBonusAwardedAmount(bidState.bonusAwardedAmount());
                blindBids.add(bid);
            }
        }
        blindOpeningBidRepository.saveAll(blindBids);

        rtmRepository.deleteAll();
        List<RtmEntity> rtms = new ArrayList<>();
        for (RtmState rtmState : state.rtms()) {
            RtmEntity rtm = new RtmEntity();
            rtm.setCaptainName(rtmState.captainName());
            rtm.setPlayerName(rtmState.playerName());
            rtm.setBidAmount(rtmState.bidAmount());
            rtm.setClaimedAt(rtmState.claimedAt() == null ? null : LocalDateTime.parse(rtmState.claimedAt()));
            rtm.setUsed(rtmState.used());
            rtm.setRtmBid(rtmState.rtmBid());
            rtm.setOriginalCaptain(rtmState.originalCaptain());
            rtm.setStatus(rtmState.status());
            rtm.setOriginalBidAmount(rtmState.originalBidAmount());
            rtms.add(rtm);
        }
        rtmRepository.saveAll(rtms);

        snapshotRepository.delete(snapshot);
        return state.blindBonanzaAwardsUsed();
    }

    public void clear() {
        snapshotRepository.deleteById(SNAPSHOT_ID);
    }

    private String write(ReAuctionState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to save the re-auction snapshot.", exception);
        }
    }

    private ReAuctionState read(String stateJson) {
        try {
            return objectMapper.readValue(stateJson, ReAuctionState.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to restore the re-auction snapshot.", exception);
        }
    }

    public record ReAuctionState(
            List<TeamState> teams,
            List<PlayerState> players,
            List<AuctionLogState> auctionLogs,
            List<CurseState> curses,
            List<BlindOpeningBidState> blindOpeningBids,
            List<RtmState> rtms,
            int blindBonanzaAwardsUsed) {
    }

    public record TeamState(
            String captainName,
            int purse,
            int tournamentPoints,
            int playersBought,
            int playersLeft,
            boolean wildPickUsed) {
    }

    public record PlayerState(
            String name,
            boolean sold,
            int soldPrice,
            int finalPrice,
            String team,
            boolean reAuctioned,
            boolean deferredToReAuction) {
    }

    public record AuctionLogState(
            String playerName,
            String captainName,
            int soldPrice) {
    }

    public record CurseState(
            String captainName,
            String playerName,
            boolean triggered,
            String settledBuyer,
            int settledAmount,
            int buyerDeduction) {
    }

    public record BlindOpeningBidState(
            String playerName,
            String captainName,
            Integer bidAmount,
            boolean submitted,
            boolean passed,
            boolean bonusEligible,
            boolean bonusAwarded,
            int bonusAwardedAmount) {
    }

    public record RtmState(
            String captainName,
            String playerName,
            int bidAmount,
            String claimedAt,
            boolean used,
            Integer rtmBid,
            String originalCaptain,
            String status,
            Integer originalBidAmount) {
    }

}