package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ProtectionPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProtectionPlayerRepository extends JpaRepository<ProtectionPlayerEntity, Long> {

    List<ProtectionPlayerEntity> findByCaptainName(String captainName);

    Optional<ProtectionPlayerEntity> findByCaptainNameAndPlayerName(String captainName, String playerName);
}
