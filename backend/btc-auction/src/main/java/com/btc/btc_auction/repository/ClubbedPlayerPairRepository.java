package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubbedPlayerPairRepository extends JpaRepository<ClubbedPlayerPairEntity, Long> {

    Optional<ClubbedPlayerPairEntity> findFirstByActiveTrue();

    List<ClubbedPlayerPairEntity> findAllByActiveTrue();

    void deleteByActiveTrue();
}
