package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionLogEntity;
import com.btc.btc_auction.repository.AuctionLogRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuctionLogService {

    private final AuctionLogRepository auctionLogRepository;

    public AuctionLogService(
            AuctionLogRepository auctionLogRepository) {

        this.auctionLogRepository = auctionLogRepository;
    }

    public void addLog(
            @NonNull AuctionLogEntity log) {

        auctionLogRepository.save(log);
    }

    public List<AuctionLogEntity> getLogs() {

        return auctionLogRepository.findAll();
    }

    public AuctionLogEntity getLastLog() {

        List<AuctionLogEntity> logs = auctionLogRepository.findAll();

        if (logs.isEmpty()) {
            return null;
        }

        return logs.get(
                logs.size() - 1);
    }

    public void removeLastLog() {

        AuctionLogEntity lastLog = getLastLog();

        if (lastLog != null) {

            auctionLogRepository.delete(
                    lastLog);
        }
    }

    public void clearLogs() {

        auctionLogRepository.deleteAll();
    }
}