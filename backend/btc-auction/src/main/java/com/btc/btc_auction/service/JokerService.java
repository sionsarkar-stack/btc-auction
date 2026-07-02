package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.JokerEntity;
import com.btc.btc_auction.enums.JokerType;
import com.btc.btc_auction.repository.JokerRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class JokerService {

    private final JokerRepository repository;

    private final AuctionEventService auctionEventService;

    public JokerService(
            JokerRepository repository,
            AuctionEventService auctionEventService) {

        this.repository = repository;
        this.auctionEventService = auctionEventService;
    }

    public List<JokerEntity> getAll() {

        return repository.findAll();

    }

    public List<JokerEntity> getCaptainJokers(
            String captainName) {

        return repository.findByCaptainName(
                captainName);

    }

    public JokerEntity save(
            @NonNull JokerEntity joker) {

        return repository.save(joker);

    }

    public void deleteAll() {

        repository.deleteAll();

    }

    public String useJoker(
            String captainName,
            JokerType jokerType) {

        JokerEntity joker = repository.findByCaptainNameAndJokerType(
                captainName,
                jokerType)
                .orElse(null);

        if (joker == null) {

            return "Joker not found";

        }

        if (joker.isUsed()) {

            return "Joker already used";

        }

        joker.setUsed(true);

        joker.setUsedAt(
                LocalDateTime.now());

        repository.save(joker);

        String effect = switch (jokerType) {

            case VETO ->
                "VETO is handled manually.";

            case BID_BLOCK ->
                "BID BLOCK will be implemented.";

            case STEAL_BID ->
                "BID STEAL will be implemented.";

            case LAST_STRIKE ->
                "LAST STRIKE is handled manually.";

        };

        auctionEventService.logEvent(
                "JOKER_USED",
                jokerType.name(),
                captainName,
                0,
                captainName + " activated " + jokerType.name());

        return jokerType.name()
                + " activated.\n"
                + effect;

    }

    public void resetAll() {

        List<JokerEntity> jokers = repository.findAll();

        for (JokerEntity joker : jokers) {

            joker.setUsed(false);

            joker.setUsedAt(null);

            repository.save(joker);

        }

    }

    public void assignRandomJokers() {

        repository.deleteAll();

        List<String> captains = List.of(
                "Rimo",
                "Sujay",
                "Nantu",
                "Joy");

        List<JokerType> jokers = new ArrayList<>(
                List.of(JokerType.values()));

        Collections.shuffle(jokers);

        for (int i = 0; i < captains.size(); i++) {

            JokerEntity joker = new JokerEntity();

            joker.setCaptainName(
                    captains.get(i));

            joker.setJokerType(
                    jokers.get(i));

            joker.setUsed(false);

            repository.save(joker);

        }

    }

}