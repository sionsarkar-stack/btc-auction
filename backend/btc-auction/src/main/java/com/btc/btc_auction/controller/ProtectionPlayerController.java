package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.ProtectionPlayerEntity;
import com.btc.btc_auction.service.ProtectionPlayerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/protection-players")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:8080" })
public class ProtectionPlayerController {

    private final ProtectionPlayerService service;

    public ProtectionPlayerController(ProtectionPlayerService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProtectionPlayerEntity> getAll() {
        return service.getAll();
    }

    @GetMapping("/{captainName}")
    public List<ProtectionPlayerEntity> getByCaptain(@PathVariable String captainName) {
        return service.getByCaptain(captainName);
    }

    @PostMapping
    public String save(@RequestBody Map<String, String> request) {
        return service.saveProtection(request.get("captainName"), request.get("playerName"));
    }

    @PostMapping("/auto-select")
    public String autoSelectForTesting() {
        return service.autoSelectForTesting();
    }
}