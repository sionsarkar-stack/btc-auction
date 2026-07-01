package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.ForbiddenPickEntity;
import com.btc.btc_auction.repository.ForbiddenPickRepository;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ForbiddenPickService {

    private final ForbiddenPickRepository repository;

    public ForbiddenPickService(
            ForbiddenPickRepository repository) {

        this.repository = repository;
    }

    public void save(
            @NonNull ForbiddenPickEntity forbiddenPick) {

        repository.save(forbiddenPick);
    }

    public ForbiddenPickEntity getByCaptain(
            String captainName) {

        return repository.findByCaptainName(
                captainName).orElse(null);
    }

    public void deleteByCaptain(
            String captainName) {

        repository.deleteByCaptainName(
                captainName);
    }

    public void deleteAll() {

        repository.deleteAll();
    }

    public List<ForbiddenPickEntity> getAll() {

        return repository.findAll();
    }
}