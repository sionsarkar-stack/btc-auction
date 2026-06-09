package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.AuctionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionLogRepository
        extends JpaRepository<AuctionLogEntity, Long> {
}