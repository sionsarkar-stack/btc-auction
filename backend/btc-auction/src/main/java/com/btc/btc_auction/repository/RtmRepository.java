package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.RtmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.transaction.Transactional;

public interface RtmRepository extends JpaRepository<RtmEntity, Long> {

    Optional<RtmEntity> findByPlayerName(String playerName);

    Optional<RtmEntity> findByCaptainName(String captainName);

    @Modifying
    @Transactional
    void deleteByPlayerName(String playerName);

    boolean existsByCaptainNameAndUsedTrue(String captainName);

}