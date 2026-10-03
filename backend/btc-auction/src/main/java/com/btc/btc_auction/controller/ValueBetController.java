package com.btc.btc_auction.controller;

import com.btc.btc_auction.service.ValueBetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:8080"
})
public class ValueBetController {

    private final ValueBetService valueBetService;

    public ValueBetController(ValueBetService valueBetService) {
        this.valueBetService = valueBetService;
    }

    @PostMapping("/api/value-bet/submit")
    public String submitPrediction(@RequestBody Map<String, Object> request) {
        String playerName = (String) request.get("playerName");
        String captainName = (String) request.get("captainName");
        Object rawPrediction = request.get("predictedPrice");
        if (playerName == null || captainName == null || rawPrediction == null) {
            return "Player, captain, and prediction are required.";
        }

        try {
            return valueBetService.submitPrediction(
                    playerName,
                    captainName,
                    Integer.parseInt(rawPrediction.toString()));
        } catch (NumberFormatException exception) {
            return "Prediction must be a valid number.";
        }
    }

    @GetMapping("/api/value-bet/{playerName}/{captainName}")
    public boolean hasSubmittedPrediction(
            @PathVariable String playerName,
            @PathVariable String captainName) {
        return valueBetService.hasSubmittedPrediction(playerName, captainName);
    }
}