package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;

import javax.sql.DataSource;

/**
 * Compatibility implementation which calls {@link com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider}
 * with the current tenant.
 *
 * Will throw {@link java.lang.IllegalStateException} if invoked when there is no tenant present.
 */
public class DataSourceProviderImpl implements DataSourceProvider
{
    private final TenantAwareDataSourceProvider tenantAwareDataSourceProvider;

    private final TenantProvider tenantProvider;

    public DataSourceProviderImpl(final TenantAwareDataSourceProvider tenantAwareDataSourceProvider, final TenantProvider tenantProvider)
    {
        this.tenantAwareDataSourceProvider = tenantAwareDataSourceProvider;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public DataSource getDataSource()
    {
        Tenant tenant = tenantProvider.getCurrentTenant();
        if (tenant == null)
        {
            throw new IllegalStateException("tenant / dataSource unavailable");
        }
        return tenantAwareDataSourceProvider.getDataSource(tenant);
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        Tenant tenant = tenantProvider.getCurrentTenant();
        if (tenant == null)
        {
            throw new IllegalStateException("tenant / databaseType unavailable");
        }
        return tenantAwareDataSourceProvider.getDatabaseType(tenant);
    }

    @Override
    public String getSchema()
    {
        Tenant tenant = tenantProvider.getCurrentTenant();
        if (tenant == null)
        {
            throw new IllegalStateException("tenant / schema unavailable");
        }
        return tenantAwareDataSourceProvider.getSchema(tenant);
    }
}
