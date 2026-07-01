package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ReverseTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReverseTargetRepository
        extends JpaRepository<ReverseTargetEntity, Long> {

    Optional<ReverseTargetEntity> findByCaptainName(
            String captainName);
}