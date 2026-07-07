package com.portalcomunitario.mscommunity.tenant;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Provisiona el schema de un tenant: corre las migraciones de community y registra el datasource al vuelo. */
@Service
public class TenantProvisioner {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioner.class);

    private final DataSourceConfig dataSourceConfig;
    private final TenantRoutingDataSource routingDataSource;

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    public TenantProvisioner(DataSourceConfig dataSourceConfig, TenantRoutingDataSource routingDataSource) {
        this.dataSourceConfig = dataSourceConfig;
        this.routingDataSource = routingDataSource;
    }

    public synchronized void provision(String schema) {
        Flyway.configure()
                .dataSource(url, username, password)
                .schemas(schema)
                .table("flyway_schema_history_community")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load()
                .migrate();
        routingDataSource.addDataSource(schema, dataSourceConfig.createDataSource(schema));
        log.info("Tenant '{}' provisionado (community)", schema);
    }
}
