package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ValueBetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValueBetRepository extends JpaRepository<ValueBetEntity, Long> {

    Optional<ValueBetEntity> findByPlayerNameAndCaptainName(String playerName, String captainName);

    List<ValueBetEntity> findByPlayerName(String playerName);
}