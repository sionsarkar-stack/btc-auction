package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TrustedCaptainEntity;
import com.btc.btc_auction.repository.TrustedCaptainRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TrustedCaptainService {

    private final TrustedCaptainRepository repository;

    public TrustedCaptainService(
            TrustedCaptainRepository repository) {

        this.repository = repository;
    }

    public void save(
            TrustedCaptainEntity trustedCaptain) {

        repository.save(trustedCaptain);
    }

    public TrustedCaptainEntity getByCaptain(
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

    public List<TrustedCaptainEntity> getAll() {

        return repository.findAll();
    }
}