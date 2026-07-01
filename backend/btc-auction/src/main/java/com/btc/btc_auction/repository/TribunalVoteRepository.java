package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.TribunalVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TribunalVoteRepository
        extends JpaRepository<TribunalVoteEntity, Long> {

    List<TribunalVoteEntity> findByTargetCaptain(
            String targetCaptain);

    List<TribunalVoteEntity> findByVotingCaptain(
            String votingCaptain);

    void deleteByVotingCaptain(
            String votingCaptain);

    void deleteByVotingCaptainAndTargetCaptain(
            String votingCaptain,
            String targetCaptain);
}