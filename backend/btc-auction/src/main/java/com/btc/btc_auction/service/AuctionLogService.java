package com.btc.btc_auction.service;

import com.btc.btc_auction.model.AuctionLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionLogService {

    private final List<AuctionLog> logs = new ArrayList<>();

    public void addLog(AuctionLog log) {
        logs.add(log);
    }

    public List<AuctionLog> getLogs() {
        return logs;
    }

    public AuctionLog getLastLog() {

        if (logs.isEmpty()) {
            return null;
        }

        return logs.get(logs.size() - 1);
    }

    public void removeLastLog() {

        if (!logs.isEmpty()) {
            logs.remove(logs.size() - 1);
        }
    }
}