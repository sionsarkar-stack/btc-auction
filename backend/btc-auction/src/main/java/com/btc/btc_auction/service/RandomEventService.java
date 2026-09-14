package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.RandomEventEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.RandomEventType;
import com.btc.btc_auction.repository.RandomEventRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RandomEventService {

    private final RandomEventRepository repository;
    private final TeamService teamService;
    private final AuctionEventService auctionEventService;
    private final AuctionConfigService auctionConfigService;

    private static final Random RNG = new Random();

    public RandomEventService(RandomEventRepository repository, TeamService teamService,
            AuctionEventService auctionEventService) {
        this(repository, teamService, auctionEventService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RandomEventService(RandomEventRepository repository, TeamService teamService,
            AuctionEventService auctionEventService, AuctionConfigService auctionConfigService) {
        this.repository = repository;
        this.teamService = teamService;
        this.auctionEventService = auctionEventService;
        this.auctionConfigService = auctionConfigService;
    }

    public List<RandomEventEntity> getActiveEvents() {
        List<RandomEventEntity> activeEvents = repository.findAllByActiveTrue();
        List<RandomEventType> existingTypes = activeEvents.stream()
                .map(event -> event.getType())
                .toList();

        for (RandomEventType type : RandomEventType.values()) {
            if (!existingTypes.contains(type)) {
                RandomEventEntity event = new RandomEventEntity();
                event.setType(type);
                event.setTitle(type.name().replace("_", " ").toLowerCase());
                event.setDescription(buildDescription(type));
                event.setAmount(defaultAmount(type));
                event.setActive(true);
                activeEvents.add(repository.save(event));
            }
        }

        return activeEvents;
    }

    public void clearEvents() {
        repository.deleteAll();
    }

    public List<RandomEventEntity> generateAllEvents() {
        clearEvents();

        List<RandomEventEntity> events = new ArrayList<>();
        for (RandomEventType type : RandomEventType.values()) {
            RandomEventEntity event = new RandomEventEntity();
            event.setType(type);
            event.setTitle(type.name().replace("_", " ").toLowerCase());
            event.setDescription(buildDescription(type));
            event.setAmount(defaultAmount(type));
            event.setCaptainName(null);
            event.setActive(true);
            repository.save(event);
            events.add(event);
        }

        return events;
    }

    public RandomEventEntity triggerRandomEvent() {
        List<RandomEventEntity> active = getActiveEvents();
        if (active.isEmpty()) {
            generateAllEvents();
            active = getActiveEvents();
        }

        RandomEventEntity selected = active.get(RNG.nextInt(active.size()));
        applyEffect(selected);
        return selected;
    }

    public void applyEffect(RandomEventEntity event) {
        if (event == null) {
            return;
        }

        if (event.getType() == RandomEventType.RTM_LOCKDOWN) {
            if (auctionConfigService != null) {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                config.setRtmLockdownPlayer("__NEXT__");
                auctionConfigService.save(config);
            }
            auctionEventService.logEvent(
                    event.getType().name(),
                    null,
                    null,
                    event.getAmount(),
                    "RTM cannot be used on the next player.");
            return;
        }

        if (event.getType() == RandomEventType.MARKET_BOOM
                || event.getType() == RandomEventType.MARKET_CRASH) {
            if (auctionConfigService != null) {
                AuctionConfigEntity config = auctionConfigService.getConfig();
                int adjustment = event.getType() == RandomEventType.MARKET_BOOM
                        ? (event.getAmount() == null ? 100 : event.getAmount())
                        : -(event.getAmount() == null ? 100 : event.getAmount());
                config.setMarketAdjustmentPlayer("__NEXT__");
                config.setMarketAdjustment(adjustment);
                auctionConfigService.save(config);
            }
            auctionEventService.logEvent(
                    event.getType().name(),
                    null,
                    null,
                    event.getAmount(),
                    event.getDescription());
            return;
        }

        TeamEntity target = null;
        if (event.getCaptainName() != null && !event.getCaptainName().isBlank()) {
            target = teamService.getTeam(event.getCaptainName());
            if (target == null) {
                List<TeamEntity> teams = teamService.getAllTeams();
                target = teams.stream()
                        .filter(team -> team.getCaptainName() != null
                                && team.getCaptainName().equalsIgnoreCase(event.getCaptainName()))
                        .findFirst()
                        .orElse(null);
            }
        } else {
            List<TeamEntity> teams = teamService.getAllTeams();
            if (!teams.isEmpty()) {
                target = teams.get(RNG.nextInt(teams.size()));
            }
        }

        if (target == null) {
            auctionEventService.logEvent(
                    event.getType().name(),
                    null,
                    null,
                    event.getAmount(),
                    event.getDescription());
            return;
        }

        switch (event.getType()) {
            case MARKET_BOOM, MARKET_CRASH -> {
                return;
            }
            case RTM_LOCKDOWN -> {
                return;
            }
            case SILENT_AUCTION -> {
                target.setPurse(target.getPurse() + (event.getAmount() == null ? 0 : event.getAmount()));
                teamService.saveTeam(target);
            }
            case VALUE_BET -> {
                return;
            }
            default -> {
            }
        }

        auctionEventService.logEvent(
                event.getType().name(),
                null,
                target.getCaptainName(),
                event.getAmount(),
                event.getDescription());
    }

    private String buildDescription(RandomEventType type) {
        return switch (type) {
            case MARKET_BOOM -> "Next player's final price increases by 100 points.";
            case MARKET_CRASH -> "When SOLD is called, the player's final price decreases by 100 points.";
            case RTM_LOCKDOWN -> "RTM cannot be used on the next player.";
            case SILENT_AUCTION -> "No live bidding for the next player. All captains submit one secret bid.";
            case VALUE_BET ->
                "Before bidding, captains predict the final price. The closest prediction earns +200.";
        };
    }

    private Integer defaultAmount(RandomEventType type) {
        return switch (type) {
            case MARKET_BOOM -> 100;
            case MARKET_CRASH -> 100;
            case RTM_LOCKDOWN -> 0;
            case SILENT_AUCTION -> 0;
            case VALUE_BET -> 200;
        };
    }
}
