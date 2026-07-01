package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.TrustedCaptainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustedCaptainRepository
        extends JpaRepository<TrustedCaptainEntity, Long> {

    Optional<TrustedCaptainEntity> findByCaptainName(
            String captainName);

    void deleteByCaptainName(
            String captainName);
}