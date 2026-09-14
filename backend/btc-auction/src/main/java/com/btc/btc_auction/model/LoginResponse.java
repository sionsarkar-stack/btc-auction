package com.btc.btc_auction.model;

public class LoginResponse {

    private final String username;
    private final String role;

    public LoginResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}