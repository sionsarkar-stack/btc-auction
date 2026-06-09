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

            TeamEntity dinda = new TeamEntity();
            dinda.setCaptainName("Dinda");
            dinda.setPurse(5000);
            dinda.setPlayersBought(0);
            dinda.setPlayersLeft(9);

            TeamEntity boni = new TeamEntity();
            boni.setCaptainName("Boni");
            boni.setPurse(6300);
            boni.setPlayersBought(0);
            boni.setPlayersLeft(9);

            TeamEntity swapneel = new TeamEntity();
            swapneel.setCaptainName("Swapneel");
            swapneel.setPurse(6200);
            swapneel.setPlayersBought(0);
            swapneel.setPlayersLeft(9);

            TeamEntity swaswata = new TeamEntity();
            swaswata.setCaptainName("Swaswata");
            swaswata.setPurse(6200);
            swaswata.setPlayersBought(0);
            swaswata.setPlayersLeft(9);

            teamRepository.save(dinda);
            teamRepository.save(boni);
            teamRepository.save(swapneel);
            teamRepository.save(swaswata);

            System.out.println("Teams Loaded");
        }
    }
}