package com.BidTech.auctionSystem.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * AuctionDbConfig — Spring configuration for the Auction service database.
 *
 * <p>This class sets up a dedicated {@link DataSource}, {@link EntityManagerFactory},
 * and {@link PlatformTransactionManager} for the {@code AUCTION.db} SQLite database.
 *
 * <p>The {@code @EnableJpaRepositories} annotation tells Spring Data JPA to use
 * {@code auctionEntityManagerFactory} for all repositories in the
 * {@code com.BidTech.auctionSystem.AuctionService} package. This ensures that
 * {@link com.BidTech.auctionSystem.AuctionService.AuctionRepository} and
 * {@link com.BidTech.auctionSystem.AuctionService.BidRepository} only ever
 * talk to {@code AUCTION.db}.
 *
 * <p>Connection settings are read from {@code application.properties} via
 * {@code @Value} injection.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.BidTech.auctionSystem.AuctionService",
        entityManagerFactoryRef = "auctionEntityManagerFactory",
        transactionManagerRef = "auctionTransactionManager"
)
public class AuctionDbConfig {

    @Value("${app.datasource.auction.url}")
    private String url;

    @Value("${app.datasource.auction.driver-class-name}")
    private String driver;

    @Value("${app.jpa.auction.dialect}")
    private String dialect;

    @Value("${app.jpa.auction.ddl-auto}")
    private String ddlAuto;

    @Bean
    public DataSource auctionDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driver);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean auctionEntityManagerFactory() {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(auctionDataSource());
        emf.setPackagesToScan("com.BidTech.auctionSystem.AuctionService");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean
    public PlatformTransactionManager auctionTransactionManager(
            @Qualifier("auctionEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}