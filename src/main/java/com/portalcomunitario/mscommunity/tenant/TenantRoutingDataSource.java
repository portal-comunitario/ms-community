package com.portalcomunitario.mscommunity.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    public void addDataSource(String tenant, DataSource dataSource) {
        targetDataSources.put(tenant, dataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    public boolean hasTenant(String tenant) {
        return targetDataSources.containsKey(tenant);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }
}
