package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.RandomEventEntity;
import com.btc.btc_auction.service.RandomEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class RandomEventController {

    private final RandomEventService randomEventService;

    public RandomEventController(RandomEventService randomEventService) {
        this.randomEventService = randomEventService;
    }

    @GetMapping("/api/random-events")
    public List<RandomEventEntity> getActiveEvents() {
        return randomEventService.getActiveEvents();
    }

    @PostMapping("/api/random-events/setup")
    public String setupRandomEvents() {
        randomEventService.generateAllEvents();
        return "Random event library initialized.";
    }

    @PostMapping("/api/random-events/trigger")
    public String triggerRandomEvent() {
        return "Random events are assigned automatically before each wheel spin.";
    }
}