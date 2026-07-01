package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.SilentBidEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SilentBidRepository
        extends JpaRepository<SilentBidEntity, Long> {

    List<SilentBidEntity> findByPlayerName(
            String playerName);

    Optional<SilentBidEntity> findByPlayerNameAndCaptainName(
            String playerName,
            String captainName);

    Optional<SilentBidEntity> findByCaptainName(
            String captainName);

}