package com.BidTech.auctionSystem.AuctionService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LoadAuctionDatabase — seeds the auction database with sample data on startup.
 *
 * <p>This {@code @Configuration} class defines a {@link CommandLineRunner} bean that
 * Spring Boot executes automatically after the application context is fully loaded.
 *
 * <p><b>Warning:</b> This class calls {@code repository.deleteAll()} before inserting
 * sample data, which means all existing auction records are wiped on every restart.
 * This is intentional for development/demo purposes but should be removed or guarded
 * before deploying to a production environment.
 *
 * <p>Two sample auctions are created:
 * <ul>
 *   <li>Auction for item ID 1, starting at $50.00</li>
 *   <li>Auction for item ID 2, starting at $100.00</li>
 * </ul>
 */
@Configuration
class LoadAuctionDatabase {

    /**
     * Creates and returns a {@link CommandLineRunner} that seeds the auction database.
     *
     * @param repository the auction repository used to save sample data
     * @return a runner that deletes all auctions and inserts two sample auctions
     */
    @Bean
    CommandLineRunner initAuctionDatabase(AuctionRepository repository) {
        return args -> {
            // Clear existing data (development only — remove for production)
            repository.deleteAll();

            // Seed two sample auctions
            repository.save(new Auction(1L, 50.0));
            repository.save(new Auction(2L, 100.0));
        };
    }
}