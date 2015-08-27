package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;

import javax.sql.DataSource;

/**
 * Compatibility implementation which calls {@link com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider}
 * with the current tenant.
 *
 * Will throw {@link java.lang.IllegalStateException} if invoked when there is no tenant present.
 */
public class DataSourceProviderImpl implements DataSourceProvider {
    private final TenantAwareDataSourceProvider tenantAwareDataSourceProvider;

    private final TenantContext tenantContext;

    public DataSourceProviderImpl(final TenantAwareDataSourceProvider tenantAwareDataSourceProvider, final TenantContext tenantContext) {
        this.tenantAwareDataSourceProvider = tenantAwareDataSourceProvider;
        this.tenantContext = tenantContext;
    }

    @Override
    public DataSource getDataSource() {
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("tenant / dataSource unavailable");
        }
        return tenantAwareDataSourceProvider.getDataSource(tenant);
    }

    @Override
    public DatabaseType getDatabaseType() {
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("tenant / databaseType unavailable");
        }
        return tenantAwareDataSourceProvider.getDatabaseType(tenant);
    }

    @Override
    public String getSchema() {
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("tenant / schema unavailable");
        }
        return tenantAwareDataSourceProvider.getSchema(tenant);
    }
}
