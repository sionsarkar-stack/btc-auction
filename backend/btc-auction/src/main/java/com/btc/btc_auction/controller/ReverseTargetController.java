package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.ReverseTargetEntity;
import com.btc.btc_auction.service.ReverseTargetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reverse-target")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class ReverseTargetController {

    private final ReverseTargetService reverseTargetService;

    public ReverseTargetController(
            ReverseTargetService reverseTargetService) {

        this.reverseTargetService = reverseTargetService;

    }

    @PostMapping
    public String save(
            @RequestBody ReverseTargetEntity target) {

        return reverseTargetService.save(target);

    }

    @GetMapping
    public List<ReverseTargetEntity> getAll() {

        return reverseTargetService.getAll();

    }

    @GetMapping("/{captainName}")
    public ReverseTargetEntity getCaptainTarget(
            @PathVariable String captainName) {

        return reverseTargetService.getByCaptain(
                captainName);

    }

}