package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * Creates a new instance of ActiveObjects given a dataSourceProvider
 */
public final class DataSourceProviderActiveObjectsFactory extends AbstractActiveObjectsFactory
{
    private final EntityManagerFactory entityManagerFactory;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;

    public DataSourceProviderActiveObjectsFactory(EntityManagerFactory entityManagerFactory, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate)
    {
        super(DataSourceType.APPLICATION);
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
        return new EntityManagedActiveObjects(entityManagerFactory.getEntityManager(dataSource, dataSourceProvider.getDatabaseType()), new SalTransactionManager(transactionTemplate));
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

    private static class ActiveObjectsDataSource implements DataSource
    {
        private final DataSource dataSource;

        ActiveObjectsDataSource(DataSource dataSource)
        {
            this.dataSource = dataSource;
        }

        public Connection getConnection() throws SQLException
        {
            return dataSource.getConnection();
        }

        public Connection getConnection(String username, String password) throws SQLException
        {
            throw new IllegalStateException("Not allowed to get a connection for non default username/password");
        }

        /**
         * Returns 0, indicating to use the default system timeout.
         */
        public int getLoginTimeout() throws SQLException
        {
            return 0;
        }

        /**
         * Setting a login timeout is not supported.
         */
        public void setLoginTimeout(int timeout) throws SQLException
        {
            throw new UnsupportedOperationException("setLoginTimeout");
        }

        /**
         * LogWriter methods are not supported.
         */
        public PrintWriter getLogWriter()
        {
            throw new UnsupportedOperationException("getLogWriter");
        }

        /**
         * LogWriter methods are not supported.
         */
        public void setLogWriter(PrintWriter pw) throws SQLException
        {
            throw new UnsupportedOperationException("setLogWriter");
        }
    }
}
