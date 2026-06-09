package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.TeamRepository;
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
            TeamEntity team) {

        teamRepository.save(team);
    }

    public List<TeamEntity> getTeams() {
        return teamRepository.findAll();
    }
}