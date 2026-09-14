package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.repository.UserRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    public UserEntity login(
            String username,
            String password) {

        return userRepository
                .findByUsername(username)
                .filter(user -> user.getPassword()
                        .equals(password))
                .orElse(null);
    }

    public void saveUser(
            @NonNull UserEntity user) {

        userRepository.save(user);
    }

    public List<UserEntity> getCaptains() {
        return userRepository.findAll().stream()
                .filter(user -> "CAPTAIN".equalsIgnoreCase(user.getRole()))
                .toList();
    }

    public UserEntity getUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public void deleteUser(String username) {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
    }
}