package com.BidTech.auctionSystem.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.beans.factory.annotation.Value;

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