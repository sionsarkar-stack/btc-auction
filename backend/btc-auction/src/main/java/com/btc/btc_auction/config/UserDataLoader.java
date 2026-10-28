package com.btc.btc_auction.config;

import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserDataLoader implements CommandLineRunner {

        private final UserRepository userRepository;
        private final String adminUsername;
        private final String adminPassword;
        private final String viewerPassword;

        public UserDataLoader(
                        UserRepository userRepository,
                        @Value("${btc.auction.admin.username:auctioneer}") String adminUsername,
                        @Value("${btc.auction.admin.password}") String adminPassword,
                        @Value("${btc.auction.viewer.password}") String viewerPassword) {

                this.userRepository = userRepository;
                this.adminUsername = adminUsername;
                this.adminPassword = adminPassword;
                this.viewerPassword = viewerPassword;
        }

        @Override
        public void run(String... args) {
                if (adminPassword.isBlank() || viewerPassword.isBlank()) {
                        throw new IllegalStateException("Admin and viewer passwords must be configured.");
                }

                createOrUpdateUser(adminUsername, adminPassword, "ADMIN");
                createOrUpdateUser("viewer", viewerPassword, "VIEWER");
                System.out.println("BTC Season 12 admin and viewer users loaded");
        }

        private void createOrUpdateUser(String username, String password, String role) {
                UserEntity user = userRepository.findByUsername(username).orElseGet(UserEntity::new);
                user.setUsername(username);
                user.setPassword(password);
                user.setRole(role);
                userRepository.save(user);
        }
}
