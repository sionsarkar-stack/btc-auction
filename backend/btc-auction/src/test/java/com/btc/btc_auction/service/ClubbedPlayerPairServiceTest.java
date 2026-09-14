package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.repository.ClubbedPlayerPairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubbedPlayerPairServiceTest {

    @Mock
    private ClubbedPlayerPairRepository repository;

    private ClubbedPlayerPairService service;

    @BeforeEach
    void setUp() {
        service = new ClubbedPlayerPairService(repository);
    }

    @Test
    void returnsThePairedPlayerAndMarksThePairActive() {
        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne("A");
        pair.setPlayerTwo("E");
        pair.setActive(true);
        when(repository.findAllByActiveTrue()).thenReturn(List.of(pair));

        assertTrue(service.isPaired("A"));
        assertEquals("E", service.getPairedPlayer("A"));
        assertEquals("A", service.getPairedPlayer("E"));
    }

    @Test
    void deletesPairById() {
        service.deletePair(10L);
        verify(repository).deleteById(10L);
    }
}
