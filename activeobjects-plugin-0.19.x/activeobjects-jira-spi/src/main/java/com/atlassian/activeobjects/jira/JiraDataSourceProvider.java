package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.jira.ofbiz.OfBizConnectionFactory;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Suppliers.*;

public final class JiraDataSourceProvider extends AbstractDataSourceProvider
{
    private static final Map<org.ofbiz.core.entity.jdbc.dbtype.DatabaseType, DatabaseType> DB_TYPE_TO_DB_TYPE = ImmutableMap.<org.ofbiz.core.entity.jdbc.dbtype.DatabaseType, DatabaseType>builder()
            .put(DatabaseTypeFactory.DB2, DatabaseType.DB2)
            .put(DatabaseTypeFactory.HSQL, DatabaseType.HSQL)
            .put(DatabaseTypeFactory.MSSQL, DatabaseType.MS_SQL)
            .put(DatabaseTypeFactory.MYSQL, DatabaseType.MYSQL)
            .put(DatabaseTypeFactory.ORACLE_10G, DatabaseType.ORACLE)
            .put(DatabaseTypeFactory.ORACLE_8I, DatabaseType.ORACLE)
            .put(DatabaseTypeFactory.POSTGRES, DatabaseType.POSTGRESQL)
            .put(DatabaseTypeFactory.POSTGRES_7_2, DatabaseType.POSTGRESQL)
            .put(DatabaseTypeFactory.POSTGRES_7_3, DatabaseType.POSTGRESQL)
            .build();

    private final OfBizConnectionFactory connectionFactory;
    private final JiraDatabaseTypeExtractor databaseTypeExtractor;
    private final DataSource ds;

    public JiraDataSourceProvider(OfBizConnectionFactory connectionFactory, JiraDatabaseTypeExtractor databaseTypeExtractor)
    {
        this.connectionFactory = checkNotNull(connectionFactory);
        this.databaseTypeExtractor = checkNotNull(databaseTypeExtractor);
        this.ds = new OfBizDataSource(connectionFactory);
    }

    public DataSource getDataSource()
    {
        return ds;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return memoize(new Supplier<DatabaseType>()
        {
            @Override
            public DatabaseType get()
            {
                Connection connection = null;
                try
                {
                    connection = ds.getConnection();
                    final DatabaseType type = DB_TYPE_TO_DB_TYPE.get(databaseTypeExtractor.getDatabaseType(connection));
                    return type != null ? type : DatabaseType.UNKNOWN;
                }
                catch (SQLException e)
                {
                    throw new IllegalStateException("Could not get database type", e);
                }
                finally
                {
                    closeQuietly(connection);
                }
            }
        }).get();
    }

    @Override
    public String getSchema()
    {
        return connectionFactory.getDatasourceInfo().getSchemaName();
    }

    private static void closeQuietly(Connection connection)
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                throw new IllegalStateException("There was an exception closing a database connection", e);
            }
        }
    }

    private static class OfBizDataSource extends AbstractDataSource
    {
        private final OfBizConnectionFactory connectionFactory;

        public OfBizDataSource(OfBizConnectionFactory connectionFactory)
        {
            this.connectionFactory = checkNotNull(connectionFactory);
        }

        public Connection getConnection() throws SQLException
        {
            return connectionFactory.getConnection();
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
