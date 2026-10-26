package com.btc.btc_auction.repository;

import com.btc.btc_auction.entity.CursePlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursePlayerRepository extends JpaRepository<CursePlayerEntity, Long> {

    List<CursePlayerEntity> findAllByOrderByIdDesc();

    List<CursePlayerEntity> findByCaptainName(String captainName);

    List<CursePlayerEntity> findByPlayerName(String playerName);

    Optional<CursePlayerEntity> findByCaptainNameAndPlayerName(String captainName, String playerName);

    void deleteByCaptainName(String captainName);

    void deleteByPlayerName(String playerName);
}