package com.btc.btc_auction.config;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.TeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TeamDataLoader implements CommandLineRunner {

    private final TeamRepository teamRepository;

    public TeamDataLoader(
            TeamRepository teamRepository) {

        this.teamRepository = teamRepository;
    }

    @Override
    public void run(String... args) {

        if (teamRepository.count() == 0) {

            TeamEntity jit = new TeamEntity();

            jit.setCaptainName("Jit");

            jit.setPurse(10000);

            jit.setPlayersBought(0);

            jit.setPlayersLeft(10);

            TeamEntity pritam = new TeamEntity();

            pritam.setCaptainName("Pritam");

            pritam.setPurse(10000);

            pritam.setPlayersBought(0);

            pritam.setPlayersLeft(10);

            TeamEntity annanya = new TeamEntity();

            annanya.setCaptainName("Annanya");

            annanya.setPurse(10500);

            annanya.setPlayersBought(0);

            annanya.setPlayersLeft(10);

            TeamEntity dragleeoo = new TeamEntity();

            dragleeoo.setCaptainName("Dragleeoo");

            dragleeoo.setPurse(10000);

            dragleeoo.setPlayersBought(0);

            dragleeoo.setPlayersLeft(10);

            teamRepository.save(jit);

            teamRepository.save(pritam);

            teamRepository.save(annanya);

            teamRepository.save(dragleeoo);

            System.out.println("Season X Teams Loaded");
        }
    }
}