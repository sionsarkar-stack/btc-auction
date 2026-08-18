package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AdminActionLogEntity;
import com.btc.btc_auction.service.AdminActionLogService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    public AdminActionLogController(
            AdminActionLogService adminActionLogService) {

        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping("/api/admin/logs")
    public List<AdminActionLogEntity> getLogs() {

        return adminActionLogService.getLogs();
    }

    @PostMapping("/api/admin/logs/clear")
    public String clearLogs() {

        adminActionLogService.clearLogs();
        return "Admin logs cleared.";
    }
}