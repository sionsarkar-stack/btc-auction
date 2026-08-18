package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ReverseTargetEntity;
import com.btc.btc_auction.repository.ReverseTargetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReverseTargetService {

    private final ReverseTargetRepository repository;
    private final AuctionConfigService configService;
    private final AdminActionLogService adminActionLogService;

    public ReverseTargetService(
            ReverseTargetRepository repository, AuctionConfigService configService,
            AdminActionLogService adminActionLogService) {

        this.repository = repository;
        this.configService = configService;
        this.adminActionLogService = adminActionLogService;

    }

    public String save(
            ReverseTargetEntity target) {

        if (configService.getConfig().isAuctionStarted()) {
            return "Reverse targets must be selected before the auction starts.";
        }

        if (target.getCaptainName() == null ||
                target.getCaptainName().isBlank() ||
                target.getRivalCaptain() == null ||
                target.getRivalCaptain().isBlank() ||
                target.getPlayerName() == null ||
                target.getPlayerName().isBlank()) {

            return "All fields are required.";
        }

        if (target.getCaptainName()
                .equalsIgnoreCase(target.getRivalCaptain())) {

            return "You cannot select yourself.";
        }

        if (repository.findByCaptainName(
                target.getCaptainName()).isPresent()) {

            return "Reverse Target already submitted.";
        }

        repository.save(target);

        adminActionLogService.addLog("REVERSE_TARGET_SUBMITTED",
                target.getPlayerName(),
                target.getCaptainName(),
                "Reverse target on " + target.getRivalCaptain());

        return "Reverse Target saved.";
    }

    public ReverseTargetEntity getByCaptain(
            String captainName) {

        return repository.findByCaptainName(
                captainName)
                .orElse(null);

    }

    public List<ReverseTargetEntity> getAll() {

        return repository.findAll();

    }

    public void deleteAll() {

        repository.deleteAll();

    }

}
