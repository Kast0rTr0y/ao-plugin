package com.atlassian.activeobjects.internal;

import net.java.ao.DatabaseProvider;
import net.java.ao.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public enum DatabaseProviders {

    MYSQL(MySQLDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            return meta.getDatabaseProductName().contains("MySQL");
        }
    },

    NETWORK_DERBY(ClientDerbyDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },
    
    ORACLE_THIN(OracleDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },

    ORACLE_OCI(OracleDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },

    POSTGRESQL(PostgreSQLDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },

    MS_SQL_SERVER(SQLServerDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },

    JTDS_MS_SQL_SERVER(JTDSSQLServerDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    },

    NETWORK_HSQLDB(HSQLDatabaseProvider.class) {
        protected boolean matches(DatabaseMetaData meta) throws SQLException {
            throw new UnsupportedOperationException(); // TODO: Implement me!!
        }
    };

    //

    private static final Logger log = LoggerFactory.getLogger( DatabaseProvider.class );

    private Class<? extends DatabaseProvider> provider;

    private DatabaseProviders(Class<? extends DatabaseProvider> provider) {
        this.provider = provider;
    }

    protected abstract boolean matches(DatabaseMetaData databaseMetaData) throws SQLException;

    private DatabaseProvider createInstance(String uri, String username, String password) {
        DatabaseProvider back = null;

        try {
            Constructor<? extends DatabaseProvider> constructor = provider.getDeclaredConstructor(String.class, String.class, String.class);
            constructor.setAccessible(true);

            back = constructor.newInstance(uri, username, password);
        } catch (SecurityException e) {
            log.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (InstantiationException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            log.error(e.getMessage(), e);
        }

        return back;
    }

    private DatabaseProvider createProvider(DataSource dataSource) {
        DatabaseProvider databaseProvider = createInstance(null, null, null);
        databaseProvider = new DataSourceDatabaseProviderWrapper(dataSource, databaseProvider);
        return databaseProvider;
    }

    public static DatabaseProvider getProviderFromDataSource(DataSource dataSource) throws SQLException {
        DatabaseMetaData metadata = dataSource.getConnection().getMetaData();

        for (DatabaseProviders each : values()) {
            if (each.matches( metadata )) return each.createProvider( dataSource );
        }

        throw new IllegalArgumentException("Unable to find a DatabaseProvider for: "+ metadata);
    }

}
