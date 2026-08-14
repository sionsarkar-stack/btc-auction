package com.btc.btc_auction.config;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserDataLoader implements CommandLineRunner {

        private final UserRepository userRepository;

        public UserDataLoader(
                        UserRepository userRepository) {

                this.userRepository = userRepository;
        }

        @Override
        public void run(String... args) {

                createOrUpdateUser("auctioneer", "Sarkar", "ADMIN");
                createOrUpdateUser("Sen", "sagar", "CAPTAIN");
                createOrUpdateUser("Gappu", "gondhi", "CAPTAIN");
                createOrUpdateUser("Anirban", "raja", "CAPTAIN");
                createOrUpdateUser("Joy", "mistu", "CAPTAIN");
                createOrUpdateUser("viewer", "viewer", "VIEWER");
                System.out.println("BTC Season 11 users loaded");
        }

        private void createOrUpdateUser(String username, String password, String role) {
                UserEntity user = userRepository.findByUsername(username).orElseGet(UserEntity::new);
                user.setUsername(username);
                user.setPassword(password);
                user.setRole(role);
                userRepository.save(user);
        }

        private void createUser(
                        String username,
                        String password,
                        String role) {

                UserEntity user = new UserEntity();

                user.setUsername(username);

                user.setPassword(password);

                user.setRole(role);

                userRepository.save(user);
        }
}
