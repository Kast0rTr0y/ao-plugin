package com.atlassian.confluence.plugin.spi.activeobjects;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.orm.hibernate.SessionFactoryUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConfluenceDataSourceProvider implements DataSourceProvider
{
    private static final Logger log = Logger.getLogger(ConfluenceDataSourceProvider.class);

    private SessionFactoryDataSource dataSource;

    public ConfluenceDataSourceProvider(SessionFactory sessionFactory)
    {
        this.dataSource = new SessionFactoryDataSource(sessionFactory);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    private static class SessionFactoryDataSource extends AbstractDataSource
    {
        private final SessionFactory sessionFactory;

        public SessionFactoryDataSource(SessionFactory sessionFactory)
        {
            this.sessionFactory = sessionFactory;
        }

        public Connection getConnection() throws SQLException
        {
            Session session = SessionFactoryUtils.getSession(sessionFactory, true);
            if (!SessionFactoryUtils.isSessionTransactional(session, sessionFactory))
            {
                log.info("Created new database session for ActiveObjects. This might not be what you want.");
            }
            try
            {
                return session.connection();
            }
            catch (HibernateException e)
            {
                throw new SQLException(e);
            }
        }

        public Connection getConnection(String username, String password) throws SQLException
        {
            return null;
        }

        public <T> T unwrap(Class<T> clazz) throws SQLException
        {
            if (isWrapperFor(clazz))
            {
                return (T) sessionFactory;
            }
            return null;
        }

        public boolean isWrapperFor(Class<?> clazz) throws SQLException
        {
            return SessionFactory.class.isAssignableFrom(clazz);
        }
    }
}
