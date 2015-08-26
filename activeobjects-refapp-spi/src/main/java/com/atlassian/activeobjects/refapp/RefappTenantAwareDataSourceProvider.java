package com.atlassian.activeobjects.refapp;

import com.atlassian.activeobjects.spi.AbstractTenantAwareDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.tenancy.api.Tenant;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class RefappTenantAwareDataSourceProvider extends AbstractTenantAwareDataSourceProvider {
    private final DataSource dataSource;

    public RefappTenantAwareDataSourceProvider(DataSource dataSource) {
        this.dataSource = checkNotNull(dataSource);
    }

    @Nonnull
    @Override
    public DataSource getDataSource(@Nonnull final Tenant tenant) {
        return dataSource;
    }

    @Nonnull
    @Override
    public DatabaseType getDatabaseType(@Nonnull final Tenant tenant) {
        return DatabaseType.HSQL;
    }
}
