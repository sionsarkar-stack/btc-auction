package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.ReAuctionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReAuctionSnapshotRepository extends JpaRepository<ReAuctionSnapshotEntity, Long> {
}