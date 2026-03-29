package com.BidTech.auctionSystem;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AuctionSystemApplication — the entry point for the BidTech Auction System.
 *
 * <p>This class bootstraps the entire Spring Boot application. The
 * {@code @SpringBootApplication} annotation is a convenience shortcut that
 * combines three annotations:
 * <ul>
 *   <li>{@code @Configuration} — marks this class as a source of bean definitions</li>
 *   <li>{@code @EnableAutoConfiguration} — tells Spring Boot to auto-configure beans
 *       based on the classpath (e.g., JPA, Web MVC, Actuator)</li>
 *   <li>{@code @ComponentScan} — scans the {@code com.BidTech.auctionSystem} package
 *       and all sub-packages for Spring components</li>
 * </ul>
 *
 * <p>On startup, Spring Boot will:
 * <ol>
 *   <li>Initialise all four database configurations (IAM, Catalogue, Auction, Payment)</li>
 *   <li>Create or update database tables via Hibernate DDL</li>
 *   <li>Run {@code CommandLineRunner} beans to seed sample data</li>
 *   <li>Start the embedded Tomcat server on port 8080</li>
 * </ol>
 */
@SpringBootApplication
@EnableRabbit

public class AuctionSystemApplication {

	/**
	 * Main method — JVM entry point.
	 *
	 * @param args command-line arguments passed to the application.
	 *             Spring Boot supports overriding any property via
	 *             {@code --key=value} arguments (e.g., {@code --server.port=9090}).
	 */
	public static void main(String[] args) {
		SpringApplication.run(AuctionSystemApplication.class, args);
	}

}
