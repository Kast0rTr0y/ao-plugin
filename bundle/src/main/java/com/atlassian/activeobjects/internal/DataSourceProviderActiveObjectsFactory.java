package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.sql.DataSourceProvider;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * Creates a new instance of ActiveObjects given a dataSourceProvider
 */
public final class DataSourceProviderActiveObjectsFactory implements ActiveObjectsFactory
{
    private final EntityManagerFactory entityManagerFactory;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;

    public DataSourceProviderActiveObjectsFactory(EntityManagerFactory entityManagerFactory, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate)
    {
        this.entityManagerFactory = checkNotNull(entityManagerFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
    }

    /**
     * Creates an {@link com.atlassian.activeobjects.external.ActiveObjects} using the
     * {@link com.atlassian.sal.api.sql.DataSourceProvider}
     *
     * @param pluginKey the plugin key of the current plugin
     * @return a new configured, ready to go ActiveObjects instance
     * @throws ActiveObjectsPluginException if the data source obtained from the {@link com.atlassian.sal.api.sql.DataSourceProvider}
     *                                      is {@code null}
     */
    public ActiveObjects create(PluginKey pluginKey)
    {
        // the data source from the application
        final DataSource dataSource = getDataSource();
        return new EntityManagedActiveObjects(entityManagerFactory.getEntityManager(dataSource), new SalTransactionManager(transactionTemplate));
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
            return new UncloseableConnection(dataSource.getConnection());
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

    private static class UncloseableConnection implements Connection
    {
        private final Connection connection;

        UncloseableConnection(final Connection connection)
        {
            this.connection = connection;
        }

        public Statement createStatement() throws SQLException
        {
            return connection.createStatement();
        }

        public PreparedStatement prepareStatement(String sql) throws SQLException
        {
            return connection.prepareStatement(sql);
        }

        public CallableStatement prepareCall(String sql) throws SQLException
        {
            return connection.prepareCall(sql);
        }

        public String nativeSQL(String sql) throws SQLException
        {
            return connection.nativeSQL(sql);
        }

        public void setAutoCommit(boolean autoCommit) throws SQLException
        {
            connection.setAutoCommit(autoCommit);
        }

        public boolean getAutoCommit() throws SQLException
        {
            return connection.getAutoCommit();
        }

        public void commit() throws SQLException
        {
//                connection.commit();
        }

        public void rollback() throws SQLException
        {
//                connection.rollback();
        }

        public void close() throws SQLException
        {
            // do nothing
        }

        public boolean isClosed() throws SQLException
        {
            return false;
        }

        public DatabaseMetaData getMetaData() throws SQLException
        {
            return connection.getMetaData();
        }

        public void setReadOnly(boolean readOnly) throws SQLException
        {
            connection.setReadOnly(readOnly);
        }

        public boolean isReadOnly() throws SQLException
        {
            return connection.isReadOnly();
        }

        public void setCatalog(String catalog) throws SQLException
        {
            connection.setCatalog(catalog);
        }

        public String getCatalog() throws SQLException
        {
            return connection.getCatalog();
        }

        public void setTransactionIsolation(int level) throws SQLException
        {
            connection.setTransactionIsolation(level);
        }

        public int getTransactionIsolation() throws SQLException
        {
            return connection.getTransactionIsolation();
        }

        public SQLWarning getWarnings() throws SQLException
        {
            return connection.getWarnings();
        }

        public void clearWarnings() throws SQLException
        {
            connection.clearWarnings();
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return connection.createStatement(resultSetType, resultSetConcurrency);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        public Map<String, Class<?>> getTypeMap() throws SQLException
        {
            return connection.getTypeMap();
        }

        public void setTypeMap(Map<String, Class<?>> map) throws SQLException
        {
            connection.setTypeMap(map);
        }

        public void setHoldability(int holdability) throws SQLException
        {
            connection.setHoldability(holdability);
        }

        public int getHoldability() throws SQLException
        {
            return connection.getHoldability();
        }

        public Savepoint setSavepoint() throws SQLException
        {
            return connection.setSavepoint();
        }

        public Savepoint setSavepoint(String name) throws SQLException
        {
            return connection.setSavepoint(name);
        }

        public void rollback(Savepoint savepoint) throws SQLException
        {
            connection.rollback(savepoint);
        }

        public void releaseSavepoint(Savepoint savepoint) throws SQLException
        {
            connection.releaseSavepoint(savepoint);
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
        {
            return connection.prepareStatement(sql, autoGeneratedKeys);
        }

        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
        {
            return connection.prepareStatement(sql, columnIndexes);
        }

        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
        {
            return connection.prepareStatement(sql, columnNames);
        }
    }
}
