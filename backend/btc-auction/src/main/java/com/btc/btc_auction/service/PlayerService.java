package com.btc.btc_auction.service;

import com.btc.btc_auction.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    private final List<Player> players = new ArrayList<>();

    public PlayerService() {

        players.add(new Player(
                "Sushovan Da",
                "Z",
                false,
                0,
                ""));

        players.add(new Player(
                "Ujjwal",
                "Z",
                false,
                0,
                ""));

        players.add(new Player(
                "Sawon",
                "Z",
                false,
                0,
                ""));
    }

    public List<Player> getPlayers() {
        return players;
    }

    // ADD HERE
    public List<Player> getUnsoldPlayers() {

        return players.stream()
                .filter(player -> !player.isSold())
                .toList();
    }

    public Player getPlayer(String name) {

        return players.stream()
                .filter(player ->
                        player.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}