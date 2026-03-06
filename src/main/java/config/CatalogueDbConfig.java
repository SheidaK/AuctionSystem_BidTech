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
        basePackages = "CatalogueService",
        entityManagerFactoryRef = "catalogueEntityManagerFactory",
        transactionManagerRef = "catalogueTransactionManager"
)
public class CatalogueDbConfig {

    @Value("${app.datasource.catalogue.url}")
    private String url;

    @Value("${app.datasource.catalogue.driver-class-name}")
    private String driver;

    @Value("${app.jpa.catalogue.dialect}")
    private String dialect;

    @Value("${app.jpa.catalogue.ddl-auto}")
    private String ddlAuto;

    @Bean
    public DataSource catalogueDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driver);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean catalogueEntityManagerFactory() {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(catalogueDataSource());
        emf.setPackagesToScan("CatalogueService");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean
    public PlatformTransactionManager catalogueTransactionManager(
            @Qualifier("catalogueEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
