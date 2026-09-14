package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TeamRepository
                extends JpaRepository<TeamEntity, Long> {

        Optional<TeamEntity> findByCaptainName(
                        String captainName);

        List<TeamEntity> findAllByOrderByIdAsc();
}