package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AdminActionLogEntity;
import com.btc.btc_auction.repository.AdminActionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminActionLogService {

    private final AdminActionLogRepository adminActionLogRepository;

    public AdminActionLogService(
            AdminActionLogRepository adminActionLogRepository) {

        this.adminActionLogRepository = adminActionLogRepository;
    }

    public void addLog(
            String actionType,
            String playerName,
            String oldCaptain,
            String newCaptain,
            int oldPrice,
            int newPrice,
            String reason) {

        AdminActionLogEntity log = new AdminActionLogEntity();

        log.setActionType(actionType);
        log.setPlayerName(playerName);
        log.setOldCaptain(oldCaptain);
        log.setNewCaptain(newCaptain);
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setReason(reason);
        log.setTimestamp(LocalDateTime.now());

        adminActionLogRepository.save(log);
    }

    public void addLog(String actionType, String details, String captainName, String reason) {
        AdminActionLogEntity log = new AdminActionLogEntity();
        log.setActionType(actionType);
        log.setPlayerName(details);
        log.setNewCaptain(captainName);
        log.setReason(reason);
        log.setTimestamp(LocalDateTime.now());
        adminActionLogRepository.save(log);
    }

    public List<AdminActionLogEntity> getLogs() {

        return adminActionLogRepository.findAll();
    }

    public void clearLogs() {
        adminActionLogRepository.deleteAll();
    }
}