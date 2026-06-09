package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.AuctionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionEventRepository
                extends JpaRepository<AuctionEventEntity, Long> {

}