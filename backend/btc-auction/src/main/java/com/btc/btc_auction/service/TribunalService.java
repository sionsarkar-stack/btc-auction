package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ForbiddenPickEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.entity.TribunalVoteEntity;
import com.btc.btc_auction.entity.TrustedCaptainEntity;
import com.btc.btc_auction.model.TribunalCaptainStatus;
import com.btc.btc_auction.model.TribunalStatus;
import com.btc.btc_auction.model.TribunalVoteRequest;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TribunalService {

    private final TribunalVoteService voteService;
    private final TrustedCaptainService trustedCaptainService;
    private final ForbiddenPickService forbiddenPickService;
    private final TeamService teamService;

    public TribunalService(
            TribunalVoteService voteService,
            TrustedCaptainService trustedCaptainService,
            ForbiddenPickService forbiddenPickService,
            TeamService teamService) {

        this.voteService = voteService;
        this.trustedCaptainService = trustedCaptainService;
        this.forbiddenPickService = forbiddenPickService;
        this.teamService = teamService;
    }

    public void generateForbiddenPick(
            String captainName) {

        List<TribunalVoteEntity> votes = voteService.getVotesAgainst(
                captainName);

        if (votes.isEmpty()) {
            return;
        }

        Map<String, Integer> voteCount = new HashMap<>();

        for (TribunalVoteEntity vote : votes) {

            voteCount.put(
                    vote.getPlayerName(),
                    voteCount.getOrDefault(
                            vote.getPlayerName(),
                            0) + 1);
        }

        String forbiddenPlayer = null;

        // Majority wins
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {

            if (entry.getValue() >= 2) {

                forbiddenPlayer = entry.getKey();

                break;
            }
        }

        // No majority
        if (forbiddenPlayer == null) {

            TrustedCaptainEntity trusted = trustedCaptainService.getByCaptain(
                    captainName);

            if (trusted == null) {
                return;
            }

            for (TribunalVoteEntity vote : votes) {

                if (vote.getVotingCaptain()
                        .equalsIgnoreCase(
                                trusted.getTrustedCaptain())) {

                    forbiddenPlayer = vote.getPlayerName();

                    break;
                }
            }
        }

        if (forbiddenPlayer == null) {
            return;
        }

        forbiddenPickService.deleteByCaptain(
                captainName);

        ForbiddenPickEntity pick = new ForbiddenPickEntity();

        pick.setCaptainName(
                captainName);

        pick.setPlayerName(
                forbiddenPlayer);

        forbiddenPickService.save(
                pick);
    }

    public void generateAll() {

        teamService.getAllTeams()

                .forEach(team ->

                generateForbiddenPick(

                        team.getCaptainName()));

    }

    public List<TribunalCaptainStatus> getStatus() {

        List<TribunalCaptainStatus> result = new ArrayList<>();

        for (TeamEntity team : teamService.getAllTeams()) {

            TrustedCaptainEntity trusted = trustedCaptainService.getByCaptain(
                    team.getCaptainName());

            List<TribunalVoteEntity> voteEntities = voteService.getVotesByCaptain(
                    team.getCaptainName());

            List<TribunalVoteRequest> votes = new ArrayList<>();

            for (TribunalVoteEntity vote : voteEntities) {

                TribunalVoteRequest request = new TribunalVoteRequest();

                request.setVotingCaptain(
                        vote.getVotingCaptain());

                request.setTargetCaptain(
                        vote.getTargetCaptain());

                request.setPlayerName(
                        vote.getPlayerName());

                votes.add(request);
            }

            result.add(

                    new TribunalCaptainStatus(

                            team.getCaptainName(),

                            voteEntities.size() == 3,

                            trusted == null
                                    ? ""
                                    : trusted.getTrustedCaptain(),

                            votes));
        }

        return result;
    }
}