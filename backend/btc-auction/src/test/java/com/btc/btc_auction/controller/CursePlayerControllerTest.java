package com.btc.btc_auction.controller;

import com.btc.btc_auction.dto.CurseSelectionDto;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.CursePlayerService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursePlayerControllerTest {

    @Mock
    private CursePlayerService cursePlayerService;
    @Mock
    private HttpSession session;

    private CursePlayerController controller;

    @BeforeEach
    void setUp() {
        controller = new CursePlayerController(cursePlayerService);
    }

    @Test
    void adminCanReadAllCurseSelections() {
        List<CurseSelectionDto> selections = List.of(new CurseSelectionDto(1L, "Alpha", "Player A"));
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("ADMIN");
        when(cursePlayerService.getAllSelections()).thenReturn(selections);

        var response = controller.getAdminSelections(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(selections, response.getBody());
        verify(cursePlayerService).getAllSelections();
    }

    @Test
    void captainCannotReadAllCurseSelections() {
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");

        var response = controller.getAdminSelections(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(cursePlayerService, never()).getAllSelections();
    }
}
