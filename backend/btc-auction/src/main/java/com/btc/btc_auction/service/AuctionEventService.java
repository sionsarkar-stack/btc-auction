package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionEventEntity;
import com.btc.btc_auction.repository.AuctionEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionEventService {

    private final AuctionEventRepository auctionEventRepository;

    public AuctionEventService(
            AuctionEventRepository auctionEventRepository) {

        this.auctionEventRepository = auctionEventRepository;
    }

    public void logEvent(
            String eventType,
            String playerName,
            String captainName,
            Integer amount,
            String details) {

        AuctionEventEntity event = new AuctionEventEntity();

        event.setEventType(eventType);
        event.setPlayerName(playerName);
        event.setCaptainName(captainName);
        event.setAmount(amount);
        event.setDetails(details);
        event.setTimestamp(
                LocalDateTime.now());

        auctionEventRepository.save(event);
    }

    public List<AuctionEventEntity> getAllEvents() {

        return auctionEventRepository.findAll();
    }

    public AuctionEventEntity getLatestEvent() {

        List<AuctionEventEntity> events = auctionEventRepository.findAll();

        if (events.isEmpty()) {
            return null;
        }

        return events.get(
                events.size() - 1);
    }

    public void clearEvents() {

        auctionEventRepository.deleteAll();
    }
}