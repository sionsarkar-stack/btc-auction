package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ReverseTargetEntity;
import com.btc.btc_auction.repository.ReverseTargetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReverseTargetService {

    private final ReverseTargetRepository repository;

    public ReverseTargetService(
            ReverseTargetRepository repository) {

        this.repository = repository;

    }

    public String save(
            ReverseTargetEntity target) {

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