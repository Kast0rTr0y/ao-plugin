package com.atlassian.activeobjects.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.tenancy.api.Tenant;
import net.java.ao.EntityManager;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

/**
 * Creates a new instance of ActiveObjects given a dataSourceProvider
 */
public final class DataSourceProviderActiveObjectsFactory extends AbstractActiveObjectsFactory
{
    private final EntityManagerFactory entityManagerFactory;
    private final TenantAwareDataSourceProvider tenantAwareDataSourceProvider;
    
    private TransactionSynchronisationManager transactionSynchronizationManager;

    public DataSourceProviderActiveObjectsFactory(ActiveObjectUpgradeManager aoUpgradeManager, 
            EntityManagerFactory entityManagerFactory, TenantAwareDataSourceProvider tenantAwareDataSourceProvider,
            TransactionTemplate transactionTemplate, ClusterLockService clusterLockService)
    {
        super(DataSourceType.APPLICATION, aoUpgradeManager,transactionTemplate, clusterLockService);
        this.entityManagerFactory = checkNotNull(entityManagerFactory);
        this.tenantAwareDataSourceProvider = checkNotNull(tenantAwareDataSourceProvider);
    }
    
    public void setTransactionSynchronizationManager(TransactionSynchronisationManager transactionSynchronizationManager)
    {
        this.transactionSynchronizationManager = transactionSynchronizationManager;
    }
    
    /**
     * Creates an {@link com.atlassian.activeobjects.external.ActiveObjects} using the
     * {@link com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider}
     *
     * @param configuration the configuration of active objects
     * @return a new configured, ready to go ActiveObjects instance
     * @throws ActiveObjectsPluginException if the data source obtained from the {@link com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider}
     * is {@code null}
     */
    @Override
    protected ActiveObjects doCreate(final ActiveObjectsConfiguration configuration, final Tenant tenant)
    {
        return transactionTemplate.execute(new TransactionCallback<ActiveObjects>()
        {
            @Override
            public ActiveObjects doInTransaction()
            {
                final DataSource dataSource = getDataSource(tenant);
                final DatabaseType dbType = getDatabaseType(tenant);
                final EntityManager entityManager = entityManagerFactory.getEntityManager(dataSource, dbType, tenantAwareDataSourceProvider.getSchema(tenant), configuration);
                return new EntityManagedActiveObjects(entityManager, 
                        new SalTransactionManager(transactionTemplate, entityManager, transactionSynchronizationManager), dbType);
            }
        });
    }

    private DataSource getDataSource(final Tenant tenant)
    {
        final DataSource dataSource = tenantAwareDataSourceProvider.getDataSource(tenant);
        if (dataSource == null)
        {
            throw new ActiveObjectsPluginException("No data source defined in the application");
        }
        return new ActiveObjectsDataSource(dataSource);
    }

    private DatabaseType getDatabaseType(final Tenant tenant)
    {
        final DatabaseType databaseType = tenantAwareDataSourceProvider.getDatabaseType(tenant);
        if (databaseType == null)
        {
            throw new ActiveObjectsPluginException("No database type defined in the application");
        }
        return databaseType;
    }

    public static class ActiveObjectsDataSource implements DataSource
    {
        private final DataSource dataSource;

        ActiveObjectsDataSource(DataSource dataSource)
        {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            return dataSource.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException
        {
            throw new IllegalStateException("Not allowed to get a connection for non default username/password");
        }

        /**
         * Returns 0, indicating to use the default system timeout.
         */
        @Override
        public int getLoginTimeout() throws SQLException
        {
            return 0;
        }

        /**
         * Setting a login timeout is not supported.
         */
        @Override
        public void setLoginTimeout(int timeout) throws SQLException
        {
            throw new UnsupportedOperationException("setLoginTimeout");
        }

        /**
         * LogWriter methods are not supported.
         */
        @Override
        public PrintWriter getLogWriter()
        {
            throw new UnsupportedOperationException("getLogWriter");
        }

        /**
         * LogWriter methods are not supported.
         */
        @Override
        public void setLogWriter(PrintWriter pw) throws SQLException
        {
            throw new UnsupportedOperationException("setLogWriter");
        }

        @Override
        public <T> T unwrap(Class<T> tClass) throws SQLException
        {
            throw new UnsupportedOperationException("unwrap");
        }

        @Override
        public boolean isWrapperFor(Class<?> aClass) throws SQLException
        {
            throw new UnsupportedOperationException("isWrapperFor");
        }

        // @Override Java 7 only
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
