package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.AdminActionLogEntity;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.AdminActionLogService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActionLogControllerTest {

    @Mock
    private AdminActionLogService adminActionLogService;
    @Mock
    private HttpSession session;

    private AdminActionLogController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminActionLogController(adminActionLogService);
    }

    @Test
    void returnsLogsForAnAuthenticatedAdmin() {
        AdminActionLogEntity log = new AdminActionLogEntity();
        List<AdminActionLogEntity> logs = List.of(log);
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("auctioneer");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("ADMIN");
        when(adminActionLogService.getLogs()).thenReturn(logs);

        ResponseEntity<List<AdminActionLogEntity>> response = controller.getLogs(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(logs, Objects.requireNonNull(response.getBody()));
    }

    @Test
    void rejectsLogAccessForANonAdmin() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("Sen");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("CAPTAIN");

        ResponseEntity<List<AdminActionLogEntity>> response = controller.getLogs(session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(adminActionLogService);
    }

    @Test
    void clearsLogsForAnAuthenticatedAdmin() {
        when(session.getAttribute(SessionAttributes.USERNAME)).thenReturn("auctioneer");
        when(session.getAttribute(SessionAttributes.ROLE)).thenReturn("ADMIN");

        ResponseEntity<String> response = controller.clearLogs(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Admin logs cleared.", response.getBody());
        verify(adminActionLogService).clearLogs();
    }
}