package com.btc.btc_auction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BtcAuctionApplication {

	public static void main(String[] args) {
		SpringApplication.run(BtcAuctionApplication.class, args);
	}

}
