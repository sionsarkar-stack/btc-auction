package com.btc.btc_auction.service;

import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.CurrentAuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class CurrentAuctionServicePersistenceTest {

    @Autowired
    private CurrentAuctionRepository repository;

    @BeforeEach
    void clearAuction() {
        repository.deleteAll();
    }

    @Test
    void restoresTheActiveAuctionForANewServiceInstance() {
        CurrentAuctionService firstInstance = new CurrentAuctionService(repository);
        firstInstance.loadPersistedAuction();
        firstInstance.setCurrentAuction(new Auction("Rohit Sharma", "A", 450, "Sen", 300));

        CurrentAuctionService restartedInstance = new CurrentAuctionService(repository);
        restartedInstance.loadPersistedAuction();
        Auction restoredAuction = restartedInstance.getCurrentAuction();

        assertEquals("Rohit Sharma", restoredAuction.getCurrentPlayer());
        assertEquals("A", restoredAuction.getSeed());
        assertEquals(450, restoredAuction.getCurrentBid());
        assertEquals("Sen", restoredAuction.getLeader());
        assertEquals(300, restoredAuction.getBasePrice());
    }
}