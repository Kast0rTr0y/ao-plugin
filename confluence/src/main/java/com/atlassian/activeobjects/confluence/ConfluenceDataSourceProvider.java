package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.hibernate.PluginHibernateSessionFactory;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

public final class ConfluenceDataSourceProvider extends AbstractDataSourceProvider
{
    private final SessionFactoryDataSource dataSource;

    public ConfluenceDataSourceProvider(PluginHibernateSessionFactory sessionFactory)
    {
        this.dataSource = new SessionFactoryDataSource(sessionFactory);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    private static class SessionFactoryDataSource extends AbstractDataSource
    {
        private final PluginHibernateSessionFactory sessionFactory;

        public SessionFactoryDataSource(PluginHibernateSessionFactory sessionFactory)
        {
            this.sessionFactory = sessionFactory;
        }

        public Connection getConnection() throws SQLException
        {
            final Session session = sessionFactory.getSession();
            try
            {
                return new UncloseableConnection(session.connection());
            }
            catch (HibernateException e)
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
    }
}
