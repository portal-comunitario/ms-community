package com.portalcomunitario.mscommunity.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private static final String PUBLIC_SCHEMA = "public";

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    public TenantRoutingDataSource tenantRoutingDataSource() {
        DataSource publicDataSource = createDataSource(PUBLIC_SCHEMA);

        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(publicDataSource);
        routingDataSource.addDataSource(PUBLIC_SCHEMA, publicDataSource);
        return routingDataSource;
    }

    public DataSource createDataSource(String schema) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url + "?currentSchema=" + schema);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
