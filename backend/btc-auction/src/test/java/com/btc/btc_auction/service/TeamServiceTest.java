package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository);
    }

    @Test
    void bidIncrementsUseFiftyUpToOneThousandAndOneHundredAbove() {
        assertEquals(true, teamService.isValidBidIncrement(1000));
        assertEquals(true, teamService.isValidBidIncrement(1100));
        assertEquals(false, teamService.isValidBidIncrement(125));
        assertEquals(false, teamService.isValidBidIncrement(1050));
    }

}
