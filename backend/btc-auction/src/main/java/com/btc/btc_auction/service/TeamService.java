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
        return teamRepository.findAll().stream()
                .sorted((first, second) -> {
                    Long firstId = first.getId();
                    Long secondId = second.getId();
                    if (firstId == null)
                        return secondId == null ? 0 : 1;
                    return secondId == null ? -1 : firstId.compareTo(secondId);
                })
                .toList();
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
        return getAllTeams();
    }

    public boolean isValidBidIncrement(int amount) {
        return amount > 0 && (amount <= 1000 ? amount % 50 == 0 : amount % 100 == 0);
    }

    public TeamEntity createCaptain(String captainName, int totalPoints) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(captainName);
        team.setPurse(totalPoints);
        team.setTournamentPoints(0);
        team.setPlayersBought(0);
        team.setWildPickUsed(false);
        team.setPlayersLeft(9);
        return teamRepository.save(team);
    }

    public void deleteCaptain(String captainName) {
        teamRepository.findByCaptainName(captainName).ifPresent(teamRepository::delete);
    }
}
