package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ForbiddenPickEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForbiddenPickRepository
        extends JpaRepository<ForbiddenPickEntity, Long> {

    Optional<ForbiddenPickEntity> findByCaptainName(
            String captainName);

    void deleteByCaptainName(
            String captainName);
}