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

            TeamEntity sen = new TeamEntity();

            sen.setCaptainName("Sen");

            sen.setPurse(5000);

            sen.setPlayersBought(0);

            sen.setPlayersLeft(9);

            TeamEntity gappu = new TeamEntity();

            gappu.setCaptainName("Gappu");

            gappu.setPurse(5300);

            gappu.setPlayersBought(0);

            gappu.setPlayersLeft(9);

            TeamEntity anirban = new TeamEntity();

            anirban.setCaptainName("Anirban");

            anirban.setPurse(5300);

            anirban.setPlayersBought(0);

            anirban.setPlayersLeft(9);

            TeamEntity joy = new TeamEntity();

            joy.setCaptainName("Joy");

            joy.setPurse(5300);

            joy.setPlayersBought(0);

            joy.setPlayersLeft(9);

            teamRepository.save(sen);

            teamRepository.save(gappu);

            teamRepository.save(anirban);

            teamRepository.save(joy);

            System.out.println("BTC Season 11 Teams Loaded");
        }
    }
}
