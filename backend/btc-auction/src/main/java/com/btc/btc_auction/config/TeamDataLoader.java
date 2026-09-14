package com.btc.btc_auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TeamDataLoader implements CommandLineRunner {

    @Override
    public void run(String... args) {

        System.out.println("BTC Season 12 teams are managed by the admin console");
    }
}
