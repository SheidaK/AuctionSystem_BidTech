package com.BidTech.auctionSystem.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.BidTech.auctionSystem.payment.repository",
        entityManagerFactoryRef = "paymentEntityManagerFactory",
        transactionManagerRef = "paymentTransactionManager"
)
public class PaymentDbConfig {

    @Bean(name = "paymentDataSource")
    public DataSource paymentDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:sqlite:PAYMENT.db")
                .driverClassName("org.sqlite.JDBC")
                .build();
    }

    @Bean(name = "paymentEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean paymentEntityManagerFactory(
            @Qualifier("paymentDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);

        em.setPackagesToScan("com.BidTech.auctionSystem.payment.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        return em;
    }

    @Bean(name = "paymentTransactionManager")
    public PlatformTransactionManager paymentTransactionManager(
            @Qualifier("paymentEntityManagerFactory") EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}