package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository
        extends JpaRepository<PlayerEntity, Long> {

    Optional<PlayerEntity> findByName(String name);
}