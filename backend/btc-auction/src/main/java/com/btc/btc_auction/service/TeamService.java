package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.TeamRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(
            TeamRepository teamRepository) {

        this.teamRepository = teamRepository;
    }

    public List<TeamEntity> getAllTeams() {

        return teamRepository.findAll();
    }

    public TeamEntity getTeam(
            String captainName) {

        return teamRepository
                .findByCaptainName(captainName)
                .orElse(null);
    }

    public void saveTeam(
            @NonNull TeamEntity team) {

        teamRepository.save(team);
    }

    public List<TeamEntity> getTeams() {
        return teamRepository.findAll();
    }

    public int getMaxBid(
            TeamEntity team) {

        if (team == null) {

            return 0;

        }

        return Math.max(0, team.getPurse()
                - Math.max(0, team.getPlayersLeft()) - 2 * 100);

    }

    /** Replaces legacy teams with the four official Season 11 captains. */
    public void resetSeasonElevenTeams() {
        teamRepository.deleteAll();
        saveSeasonElevenTeam("Sen", 5000);
        saveSeasonElevenTeam("Gappu", 5300);
        saveSeasonElevenTeam("Anirban", 5300);
        saveSeasonElevenTeam("Joy", 5300);
    }

    private void saveSeasonElevenTeam(String captainName, int purse) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(captainName);
        team.setPurse(purse);
        team.setPlayersBought(0);
        // Ten-player squads include the captain, so only nine players are bought.
        team.setPlayersLeft(9);
        teamRepository.save(team);
    }
}
