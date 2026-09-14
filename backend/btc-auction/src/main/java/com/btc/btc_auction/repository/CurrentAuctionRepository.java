package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.CurrentAuctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrentAuctionRepository extends JpaRepository<CurrentAuctionEntity, Long> {
}