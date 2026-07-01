package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.JokerEntity;
import com.btc.btc_auction.enums.JokerType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JokerRepository
        extends JpaRepository<JokerEntity, Long> {

    List<JokerEntity> findByCaptainName(
            String captainName);

    Optional<JokerEntity> findByCaptainNameAndJokerType(
            String captainName,
            JokerType jokerType);
}