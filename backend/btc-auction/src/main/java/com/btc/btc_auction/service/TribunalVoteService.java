package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TribunalVoteEntity;
import com.btc.btc_auction.repository.TribunalVoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TribunalVoteService {

    private final TribunalVoteRepository repository;

    public TribunalVoteService(
            TribunalVoteRepository repository) {

        this.repository = repository;
    }

    public void save(
            TribunalVoteEntity vote) {

        repository.save(vote);
    }

    public List<TribunalVoteEntity> getVotesAgainst(
            String captainName) {

        return repository.findByTargetCaptain(
                captainName);
    }

    public List<TribunalVoteEntity> getVotesByCaptain(
            String captainName) {

        return repository.findByVotingCaptain(
                captainName);
    }

    public void deleteVotesByCaptain(
            String captainName) {

        repository.deleteByVotingCaptain(
                captainName);
    }

    public void deleteAll() {

        repository.deleteAll();
    }

    public void deleteVote(
            String votingCaptain,
            String targetCaptain) {

        repository.deleteByVotingCaptainAndTargetCaptain(
                votingCaptain,
                targetCaptain);
    }

    public List<TribunalVoteEntity> getAll() {

        return repository.findAll();
    }
}