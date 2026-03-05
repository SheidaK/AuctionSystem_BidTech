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
        basePackages = "com.BidTech.auctionSystem.IAMService",
        entityManagerFactoryRef = "iamEntityManagerFactory",
        transactionManagerRef = "iamTransactionManager"
)
public class IamDbConfig {

    @Value("${app.datasource.iam.url}")
    private String url;

    @Value("${app.datasource.iam.driver-class-name}")
    private String driver;

    @Value("${app.jpa.iam.dialect}")
    private String dialect;

    @Value("${app.jpa.iam.ddl-auto}")
    private String ddlAuto;

    @Bean
    public DataSource iamDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driver);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean iamEntityManagerFactory() {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(iamDataSource());
        emf.setPackagesToScan("com.BidTech.auctionSystem.IAMService");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean
    public PlatformTransactionManager iamTransactionManager(
            @Qualifier("iamEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}