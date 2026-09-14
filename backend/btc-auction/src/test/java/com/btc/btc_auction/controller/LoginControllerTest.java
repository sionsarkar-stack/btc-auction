package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.model.LoginRequest;
import com.btc.btc_auction.model.LoginResponse;
import com.btc.btc_auction.security.SessionAttributes;
import com.btc.btc_auction.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private HttpSession session;

    @Test
    void storesTheAuthenticatedIdentityInTheSession() {
        UserEntity user = new UserEntity();
        user.setUsername("Sen");
        user.setRole("CAPTAIN");
        LoginRequest request = new LoginRequest();
        request.setUsername("Sen");
        request.setPassword("secret");
        when(userService.login("Sen", "secret")).thenReturn(user);

        LoginController controller = new LoginController(userService);
        ResponseEntity<LoginResponse> response = controller.login(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        LoginResponse responseBody = Objects.requireNonNull(response.getBody());
        assertEquals("Sen", responseBody.getUsername());
        assertEquals("CAPTAIN", responseBody.getRole());
        verify(session).setAttribute(SessionAttributes.USERNAME, "Sen");
        verify(session).setAttribute(SessionAttributes.ROLE, "CAPTAIN");
    }

    @Test
    void doesNotCreateAnAuthenticatedResponseForInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("Sen");
        request.setPassword("incorrect");
        when(userService.login("Sen", "incorrect")).thenReturn(null);

        LoginController controller = new LoginController(userService);
        ResponseEntity<LoginResponse> response = controller.login(request, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }
}