package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.SecretTargetEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.SecretTargetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecretTargetService {
    private final SecretTargetRepository repository;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final AuctionEventService eventService;
    private final AuctionConfigService configService;

    public SecretTargetService(SecretTargetRepository repository, PlayerService playerService,
            TeamService teamService, AuctionEventService eventService, AuctionConfigService configService) {
        this.repository = repository;
        this.playerService = playerService;
        this.teamService = teamService;
        this.eventService = eventService;
        this.configService = configService;
    }

    public String save(SecretTargetEntity target) {
        if (configService.getConfig().isAuctionStarted()) {
            return "Secret targets must be selected before the auction starts.";
        }
        if (target.getCaptainName() == null || target.getCaptainName().isBlank()
                || target.getPlayerOne() == null || target.getPlayerOne().isBlank()
                || target.getPlayerTwo() == null || target.getPlayerTwo().isBlank()) {
            return "Captain and both target players are required.";
        }
        if (target.getPlayerOne().equalsIgnoreCase(target.getPlayerTwo())) {
            return "Select two different target players.";
        }
        if (teamService.getTeam(target.getCaptainName()) == null
                || playerService.getPlayer(target.getPlayerOne()) == null
                || playerService.getPlayer(target.getPlayerTwo()) == null) {
            return "Captain or target player was not found.";
        }
        if (repository.findByCaptainName(target.getCaptainName()).isPresent()) {
            return "Secret targets already submitted.";
        }
        target.setSettled(false);
        repository.save(target);
        return "Secret targets saved.";
    }

    public SecretTargetEntity getByCaptain(String captainName) {
        return repository.findByCaptainName(captainName).orElse(null);
    }

    public List<SecretTargetEntity> getAll() { return repository.findAll(); }

    public void settleAll() {
        for (SecretTargetEntity target : repository.findAll()) {
            if (target.isSettled()) continue;
            PlayerEntity first = playerService.getPlayer(target.getPlayerOne());
            PlayerEntity second = playerService.getPlayer(target.getPlayerTwo());
            int purchased = (first != null && first.isSold() ? 1 : 0) + (second != null && second.isSold() ? 1 : 0);
            int adjustment = purchased == 2 ? 400 : purchased == 1 ? 50 : -200;
            TeamEntity team = teamService.getTeam(target.getCaptainName());
            if (team != null) {
                team.setPurse(team.getPurse() + adjustment);
                teamService.saveTeam(team);
                eventService.logEvent("SECRET_TARGET_SETTLED", target.getPlayerOne() + ", " + target.getPlayerTwo(),
                        target.getCaptainName(), adjustment, purchased + " of 2 targets purchased");
            }
            target.setSettled(true);
            repository.save(target);
        }
    }

    public void deleteAll() { repository.deleteAll(); }
}
