package com.atlassian.activeobjects.fecru;

import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.cenqua.crucible.hibernate.DBType;
import com.cenqua.crucible.hibernate.SessionConnectionProvider;
import com.google.common.collect.ImmutableMap;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public final class FecruDataSourceProvider extends AbstractDataSourceProvider {
    private static final Map<DBType, DatabaseType> DBTYPE_TO_DATABASE_MAPPING = ImmutableMap.<DBType, DatabaseType>builder()
            .put(DBType.HSQL, DatabaseType.HSQL)
            .put(DBType.MYSQL, DatabaseType.MYSQL)
            .put(DBType.POSTGRESQL, DatabaseType.POSTGRESQL)
            .put(DBType.ORACLE, DatabaseType.ORACLE)
            .put(DBType.SQLSERVER2005, DatabaseType.MS_SQL)
            .put(DBType.SQLSERVER2008, DatabaseType.MS_SQL) // SQL Server 2008 is not supported by active objects according to the documentation
            .build();

    private final SessionFactoryDataSource dataSource;
    private final SessionConnectionProvider sessionConnectionProvider;

    public FecruDataSourceProvider(SessionConnectionProvider sessionConnectionProvider) {
        this.sessionConnectionProvider = sessionConnectionProvider;
        this.dataSource = new SessionFactoryDataSource(this.sessionConnectionProvider);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DatabaseType getDatabaseType() {
        DBType dbType = sessionConnectionProvider.getDBType();

        DatabaseType databaseType = DBTYPE_TO_DATABASE_MAPPING.get(dbType);

        if (databaseType != null) {
            return databaseType;
        }

        return super.getDatabaseType();
    }

    private static class SessionFactoryDataSource extends AbstractDataSource {

        private final SessionConnectionProvider sessionConnectionProvider;

        public SessionFactoryDataSource(SessionConnectionProvider sessionConnectionProvider) {
            this.sessionConnectionProvider = sessionConnectionProvider;
        }

        public Connection getConnection() throws SQLException {
            return sessionConnectionProvider.getConnection();
        }

        public Connection getConnection(String username, String password) throws SQLException {
            throw new IllegalStateException("Not allowed to get a connection for non default username/password");
        }

        public <T> T unwrap(Class<T> tClass) throws SQLException {
            return null;
        }

        public boolean isWrapperFor(Class<?> aClass) throws SQLException {
            return false;
        }
    }

    private static abstract class AbstractDataSource implements DataSource {
        /**
         * Returns 0, indicating to use the default system timeout.
         */
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        /**
         * Setting a login timeout is not supported.
         */
        public void setLoginTimeout(int timeout) throws SQLException {
            throw new UnsupportedOperationException("setLoginTimeout");
        }

        /**
         * LogWriter methods are not supported.
         */
        public PrintWriter getLogWriter() {
            throw new UnsupportedOperationException("getLogWriter");
        }

        /**
         * LogWriter methods are not supported.
         */
        public void setLogWriter(PrintWriter pw) throws SQLException {
            throw new UnsupportedOperationException("setLogWriter");
        }
    }
}
