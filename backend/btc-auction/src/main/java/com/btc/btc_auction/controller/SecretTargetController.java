package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.SecretTargetEntity;
import com.btc.btc_auction.service.SecretTargetService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secret-targets")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:8080" })
public class SecretTargetController {
    private final SecretTargetService service;
    public SecretTargetController(SecretTargetService service) { this.service = service; }
    @PostMapping public String save(@RequestBody SecretTargetEntity target) { return service.save(target); }
    @GetMapping public List<SecretTargetEntity> getAll() { return service.getAll(); }
    @GetMapping("/{captainName}") public SecretTargetEntity getByCaptain(@PathVariable String captainName) {
        return service.getByCaptain(captainName);
    }
}
