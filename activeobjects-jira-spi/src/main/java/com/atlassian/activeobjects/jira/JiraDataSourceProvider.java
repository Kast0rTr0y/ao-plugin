package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.google.common.collect.ImmutableMap;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericEntityException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JiraDataSourceProvider extends AbstractDataSourceProvider
{
    private static final Map<com.atlassian.jira.configurator.config.DatabaseType, DatabaseType> DB_TYPE_TO_DB_TYPE = ImmutableMap.<com.atlassian.jira.configurator.config.DatabaseType, DatabaseType>builder()
            .put(com.atlassian.jira.configurator.config.DatabaseType.HSQL, DatabaseType.HSQL)
            .put(com.atlassian.jira.configurator.config.DatabaseType.MY_SQL, DatabaseType.MYSQL)
            .put(com.atlassian.jira.configurator.config.DatabaseType.ORACLE, DatabaseType.ORACLE)
            .put(com.atlassian.jira.configurator.config.DatabaseType.POSTGRES, DatabaseType.POSTGRESQL)
            .put(com.atlassian.jira.configurator.config.DatabaseType.SQL_SERVER, DatabaseType.MS_SQL)
            .build();

    private final DataSource ds;
    private final JiraDatabaseTypeExtractor databaseTypeExtractor;

    public JiraDataSourceProvider(JiraDatabaseTypeExtractor databaseTypeExtractor)
    {
        this.ds = new OfBizDataSource("defaultDS");
        this.databaseTypeExtractor = checkNotNull(databaseTypeExtractor);
    }

    public DataSource getDataSource()
    {
        return ds;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        final DatabaseType databaseType = DB_TYPE_TO_DB_TYPE.get(databaseTypeExtractor.getDatabaseType());
        return databaseType != null ? databaseType : super.getDatabaseType();
    }

    private static class OfBizDataSource extends AbstractDataSource
    {
        private final String helperName;

        public OfBizDataSource(String helperName)
        {
            this.helperName = helperName;
        }

        public Connection getConnection() throws SQLException
        {
            try
            {
                return ConnectionFactory.getConnection(helperName);
            }
            catch (GenericEntityException e)
            {
                throw new SQLException(e.getMessage());
            }
        }

        public Connection getConnection(String username, String password) throws SQLException
        {
            throw new IllegalStateException("Not allowed to get a connection for non default username/password");
        }
    }

    private static abstract class AbstractDataSource implements DataSource
    {
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

        public <T> T unwrap(Class<T> tClass) throws SQLException
        {
            throw new UnsupportedOperationException("unwrap");
        }

        public boolean isWrapperFor(Class<?> aClass) throws SQLException
        {
            throw new UnsupportedOperationException("isWrapperFor");
        }
    }
}
