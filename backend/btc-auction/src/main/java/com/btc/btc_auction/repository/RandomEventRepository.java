package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.RandomEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RandomEventRepository extends JpaRepository<RandomEventEntity, Long> {

    List<RandomEventEntity> findAllByActiveTrue();
}
