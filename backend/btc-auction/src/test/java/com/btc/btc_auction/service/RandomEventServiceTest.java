package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.RandomEventEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.enums.RandomEventType;
import com.btc.btc_auction.repository.RandomEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RandomEventServiceTest {

    @Mock
    private RandomEventRepository repository;

    @Mock
    private TeamService teamService;

    @Mock
    private AuctionEventService auctionEventService;

    @Mock
    private AuctionConfigService auctionConfigService;

    private RandomEventService service;
    private AuctionConfigEntity config;

    @BeforeEach
    void setUp() {
        config = new AuctionConfigEntity();
        lenient().when(auctionConfigService.getConfig()).thenReturn(config);
        service = new RandomEventService(repository, teamService, auctionEventService, auctionConfigService);
    }

    @Test
    void marketBoomMarksTheNextPlayerForAOneHundredPriceIncrease() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Sen");
        team.setPurse(500);

        RandomEventEntity event = new RandomEventEntity();
        event.setType(RandomEventType.MARKET_BOOM);
        event.setAmount(100);
        event.setCaptainName("Sen");

        service.applyEffect(event);

        assertEquals("__NEXT__", config.getMarketAdjustmentPlayer());
        assertEquals(100, config.getMarketAdjustment());
        assertEquals(500, team.getPurse());
    }

    @Test
    void marketCrashMarksTheNextPlayerForAOneHundredPriceReduction() {
        TeamEntity team = new TeamEntity();
        team.setCaptainName("Gappu");
        team.setPurse(500);

        RandomEventEntity event = new RandomEventEntity();
        event.setType(RandomEventType.MARKET_CRASH);
        event.setAmount(100);
        event.setCaptainName("Gappu");

        service.applyEffect(event);

        assertEquals("__NEXT__", config.getMarketAdjustmentPlayer());
        assertEquals(-100, config.getMarketAdjustment());
        assertEquals(500, team.getPurse());
    }

    @Test
    void rtmLockdownMarksOnlyTheNextPlayer() {
        RandomEventEntity event = new RandomEventEntity();
        event.setType(RandomEventType.RTM_LOCKDOWN);
        event.setAmount(0);

        service.applyEffect(event);

        assertEquals("__NEXT__", config.getRtmLockdownPlayer());
        verify(auctionConfigService).save(Objects.requireNonNull(config));
    }
}
