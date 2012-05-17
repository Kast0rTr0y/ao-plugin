package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.confluence.hibernate.DialectExtractor;
import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.activeobjects.spi.ConnectionHandler;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.hibernate.PluginHibernateSessionFactory;
import com.google.common.collect.ImmutableMap;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.dialect.DB2Dialect;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.dialect.HSQLDialect;
import net.sf.hibernate.dialect.MySQLDialect;
import net.sf.hibernate.dialect.Oracle9Dialect;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.dialect.SQLServerDialect;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.*;

public final class ConfluenceDataSourceProvider extends AbstractDataSourceProvider
{
    private static final Map<Class<? extends Dialect>, DatabaseType> DIALECT_TO_DATABASE_MAPPING = ImmutableMap.<Class<? extends Dialect>, DatabaseType>builder()
            .put(HSQLDialect.class, DatabaseType.HSQL)
            .put(MySQLDialect.class, DatabaseType.MYSQL)
            .put(PostgreSQLDialect.class, DatabaseType.POSTGRESQL)
            .put(Oracle9Dialect.class, DatabaseType.ORACLE)
            .put(SQLServerDialect.class, DatabaseType.MS_SQL)
            .put(DB2Dialect.class, DatabaseType.DB2)
            .build();

    private final SessionFactoryDataSource dataSource;
    private final DialectExtractor dialectExtractor;

    public ConfluenceDataSourceProvider(PluginHibernateSessionFactory sessionFactory, DialectExtractor dialectExtractor)
    {
        this.dataSource = new SessionFactoryDataSource(checkNotNull(sessionFactory));
        this.dialectExtractor = checkNotNull(dialectExtractor);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        final Class<? extends Dialect> dialect = dialectExtractor.getDialect();
        if (dialect == null)
        {
            return DatabaseType.UNKNOWN;
        }
        for (Map.Entry<Class<? extends Dialect>, DatabaseType> entry : DIALECT_TO_DATABASE_MAPPING.entrySet())
        {
            if (entry.getKey().isAssignableFrom(dialect))
            {
                return entry.getValue();
            }
        }
        return super.getDatabaseType();
    }

    private static class SessionFactoryDataSource extends AbstractDataSource
    {
        private final PluginHibernateSessionFactory sessionFactory;

        public SessionFactoryDataSource(PluginHibernateSessionFactory sessionFactory)
        {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            final Session session = sessionFactory.getSession();
            try
            {
                return ConnectionHandler.newInstance(session.connection());
            }
            catch (HibernateException e)
            {
                throw new SQLException(e.getMessage());
            }
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException
        {
            throw new IllegalStateException("Not allowed to get a connection for non default username/password");
        }

        @Override
        public <T> T unwrap(Class<T> tClass) throws SQLException
        {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> aClass) throws SQLException
        {
            return false;
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

        // @Override Java 7 only
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
