package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.entity.ReAuctionSnapshotEntity;
import com.btc.btc_auction.entity.RtmEntity;
import com.btc.btc_auction.entity.ValueBetEntity;
import com.btc.btc_auction.repository.AuctionLogRepository;
import com.btc.btc_auction.repository.PlayerRepository;
import com.btc.btc_auction.repository.ReAuctionSnapshotRepository;
import com.btc.btc_auction.repository.RtmRepository;
import com.btc.btc_auction.repository.TeamRepository;
import com.btc.btc_auction.repository.ValueBetRepository;
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
    private final RtmRepository rtmRepository;
    private final ValueBetRepository valueBetRepository;
    private final ObjectMapper objectMapper;

    public ReAuctionSnapshotService(
            ReAuctionSnapshotRepository snapshotRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            AuctionLogRepository auctionLogRepository,
            RtmRepository rtmRepository,
            ValueBetRepository valueBetRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.auctionLogRepository = auctionLogRepository;
        this.rtmRepository = rtmRepository;
        this.valueBetRepository = valueBetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void capture(int valueBetEventsUsed) {
        ReAuctionState state = new ReAuctionState(
                teamRepository.findAll().stream()
                        .map(team -> new TeamState(
                                team.getCaptainName(),
                                team.getPurse(),
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
                valueBetRepository.findAll().stream()
                        .map(valueBet -> new ValueBetState(
                                valueBet.getPlayerName(),
                                valueBet.getCaptainName(),
                                valueBet.getPredictedPrice(),
                                valueBet.isRewarded()))
                        .toList(),
                valueBetEventsUsed);

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

        valueBetRepository.deleteAll();
        List<ValueBetEntity> valueBets = new ArrayList<>();
        for (ValueBetState valueBetState : state.valueBets()) {
            ValueBetEntity valueBet = new ValueBetEntity();
            valueBet.setPlayerName(valueBetState.playerName());
            valueBet.setCaptainName(valueBetState.captainName());
            valueBet.setPredictedPrice(valueBetState.predictedPrice());
            valueBet.setRewarded(valueBetState.rewarded());
            valueBets.add(valueBet);
        }
        valueBetRepository.saveAll(valueBets);

        snapshotRepository.delete(snapshot);
        return state.valueBetEventsUsed();
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
            List<RtmState> rtms,
            List<ValueBetState> valueBets,
            int valueBetEventsUsed) {
    }

    public record TeamState(
            String captainName,
            int purse,
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

    public record ValueBetState(
            String playerName,
            String captainName,
            int predictedPrice,
            boolean rewarded) {
    }
}