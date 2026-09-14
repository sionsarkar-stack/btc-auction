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

        int playersToBuy = Math.max(0, team.getPlayersLeft());
        int reserveSlots = Math.max(0, playersToBuy - 2);
        return Math.max(0,
                team.getPurse()
                        - (100 * reserveSlots));

    }

    public boolean isValidBidIncrement(int amount) {
        return amount > 0 && (amount <= 1000 ? amount % 50 == 0 : amount % 100 == 0);
    }

    public TeamEntity createCaptain(String captainName, int totalPoints) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(captainName);
        team.setPurse(totalPoints);
        team.setPlayersBought(0);
        team.setWildPickUsed(false);
        team.setPlayersLeft(9);
        return teamRepository.save(team);
    }

    public void deleteCaptain(String captainName) {
        teamRepository.findByCaptainName(captainName).ifPresent(teamRepository::delete);
    }
}
