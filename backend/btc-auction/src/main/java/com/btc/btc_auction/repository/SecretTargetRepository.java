package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.SecretTargetEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretTargetRepository extends JpaRepository<SecretTargetEntity, Long> {
    Optional<SecretTargetEntity> findByCaptainName(String captainName);
}
