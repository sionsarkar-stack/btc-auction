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

                if (userRepository.count() == 0) {

                        createUser(
                                        "auctioneer",
                                        "Sarkar",
                                        "ADMIN");

                        createUser(
                                        "Rimo",
                                        "nini",
                                        "CAPTAIN");

                        createUser(
                                        "Joy",
                                        "misti",
                                        "CAPTAIN");

                        createUser(
                                        "Sujay",
                                        "pritam",
                                        "CAPTAIN");

                        createUser(
                                        "Dragleeoo",
                                        "AMDSena",
                                        "CAPTAIN");

                        createUser(
                                        "viewer",
                                        "viewer",
                                        "VIEWER");

                        System.out.println(
                                        "Season X Users Loaded");
                }
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