package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionConfigRepository
        extends JpaRepository<AuctionConfigEntity, Long> {
}