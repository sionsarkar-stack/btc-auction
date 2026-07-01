package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.service.AuctionConfigService;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class AuctionConfigController {

    private final AuctionConfigService service;

    public AuctionConfigController(
            AuctionConfigService service) {

        this.service = service;
    }

    @GetMapping("/api/config")
    public AuctionConfigEntity getConfig() {

        return service.getConfig();
    }

    @PostMapping("/api/config")
    public AuctionConfigEntity saveConfig(
            @RequestBody @NonNull AuctionConfigEntity config) {

        return service.save(config);
    }
}