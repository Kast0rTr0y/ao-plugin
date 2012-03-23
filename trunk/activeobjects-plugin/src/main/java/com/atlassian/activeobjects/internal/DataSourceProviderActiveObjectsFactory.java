package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.*;

/**
 * Creates a new instance of ActiveObjects given a dataSourceProvider
 */
public final class DataSourceProviderActiveObjectsFactory extends AbstractActiveObjectsFactory
{
    private final EntityManagerFactory entityManagerFactory;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;

    public DataSourceProviderActiveObjectsFactory(ActiveObjectUpgradeManager aoUpgradeManager, EntityManagerFactory entityManagerFactory, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate)
    {
        super(DataSourceType.APPLICATION, aoUpgradeManager);
        this.entityManagerFactory = checkNotNull(entityManagerFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
    }

    /**
     * Creates an {@link com.atlassian.activeobjects.external.ActiveObjects} using the
     * {@link com.atlassian.activeobjects.spi.DataSourceProvider}
     *
     * @param configuration the configuration of active objects
     * @return a new configured, ready to go ActiveObjects instance
     * @throws ActiveObjectsPluginException if the data source obtained from the {@link com.atlassian.activeobjects.spi.DataSourceProvider}
     * is {@code null}
     */
    @Override
    protected ActiveObjects doCreate(ActiveObjectsConfiguration configuration)
    {
        // the data source from the application
        final DataSource dataSource = getDataSource();
        return new EntityManagedActiveObjects(entityManagerFactory.getEntityManager(dataSource, dataSourceProvider.getDatabaseType(), dataSourceProvider.getSchema(), configuration), new SalTransactionManager(transactionTemplate));
    }

    private DataSource getDataSource()
    {
        final DataSource dataSource = dataSourceProvider.getDataSource();
        if (dataSource == null)
        {
            throw new ActiveObjectsPluginException("No data source defined in the application");
        }
        return new ActiveObjectsDataSource(dataSource);
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
