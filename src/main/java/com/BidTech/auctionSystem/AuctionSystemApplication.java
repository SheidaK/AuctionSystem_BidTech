package com.BidTech.auctionSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"IAMService", "CatalogueService", "com.BidTech.auctionSystem"})
@EnableJpaRepositories(basePackages = {"IAMService", "CatalogueService"})
@org.springframework.boot.persistence.autoconfigure.EntityScan({"IAMService", "CatalogueService"})
public class AuctionSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionSystemApplication.class, args);
	}

}