package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.BountyPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface BountyPlayerRepository
                extends JpaRepository<BountyPlayerEntity, Long> {

        Optional<BountyPlayerEntity> findByPlayerName(
                        String playerName);

        List<BountyPlayerEntity> findByRevealedFalse();

        @NonNull
        List<BountyPlayerEntity> findAll();
}