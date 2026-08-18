package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceCsvImportTest {

    @Mock
    private PlayerRepository playerRepository;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository);
    }

    @Test
    void importCsv_handlesQuotedValuesAndInvalidBasePriceGracefully() throws Exception {
        when(playerRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = """
                name,seed,basePrice
                "Miller, Ryan",ICON,800
                "Doe, John",PRO,not-a-number
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "players.csv",
                "text/csv",
                csv.getBytes());

        assertDoesNotThrow(() -> playerService.importCsv(file));
        verify(playerRepository, times(2)).save(any(PlayerEntity.class));
    }
}
