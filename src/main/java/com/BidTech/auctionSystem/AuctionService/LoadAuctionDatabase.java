package com.BidTech.auctionSystem.AuctionService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;

/*
 * LoadAuctionDatabase
 * -------------------
 * Preloads sample auctions into the database when the application starts.
 */

@Configuration
class LoadAuctionDatabase {

    @Bean
    CommandLineRunner initAuctionDatabase(AuctionRepository repository) {

        return args -> {
            repository.save(new Auction(1L, 50.0,
                    LocalDateTime.now().plusHours(1)));

            repository.save(new Auction(2L, 100.0,
                    LocalDateTime.now().plusHours(2)));
        };
    }
}