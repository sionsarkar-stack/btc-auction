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

            TeamEntity rimo = new TeamEntity();

            rimo.setCaptainName("Rimo");

            rimo.setPurse(10000);

            rimo.setPlayersBought(0);

            rimo.setPlayersLeft(10);

            TeamEntity sujay = new TeamEntity();

            sujay.setCaptainName("Sujay");

            sujay.setPurse(9400);

            sujay.setPlayersBought(0);

            sujay.setPlayersLeft(10);

            TeamEntity nantu = new TeamEntity();

            nantu.setCaptainName("Nantu");

            nantu.setPurse(9300);

            nantu.setPlayersBought(0);

            nantu.setPlayersLeft(10);

            TeamEntity joy = new TeamEntity();

            joy.setCaptainName("Joy");

            joy.setPurse(10000);

            joy.setPlayersBought(0);

            joy.setPlayersLeft(10);

            teamRepository.save(rimo);

            teamRepository.save(sujay);

            teamRepository.save(nantu);

            teamRepository.save(joy);

            System.out.println("Season X Teams Loaded");
        }
    }
}