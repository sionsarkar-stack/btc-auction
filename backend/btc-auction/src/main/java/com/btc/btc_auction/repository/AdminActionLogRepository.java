package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.AdminActionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository
        extends JpaRepository<AdminActionLogEntity, Long> {

}