package com.btc.btc_auction.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuctionSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public AuctionSocketService(
            SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastRefresh() {

        messagingTemplate.convertAndSend(
                "/topic/auction",
                "refresh");
    }

}