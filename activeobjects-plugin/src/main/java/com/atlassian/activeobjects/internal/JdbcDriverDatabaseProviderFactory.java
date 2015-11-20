package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.DatabaseProvider;
import net.java.ao.Disposable;
import net.java.ao.DisposableDataSource;
import net.java.ao.builder.DelegatingDisposableDataSourceHandler;
import net.java.ao.db.ClientDerbyDatabaseProvider;
import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.H2DatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.MsJdbcSQLServerDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.NuoDBDatabaseProvider;
import net.java.ao.db.NuoDBDisposableDataSourceHandler;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.db.SQLServerDatabaseProvider;

import javax.sql.DataSource;

import static com.atlassian.activeobjects.ao.ConverterUtils.toLowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

public final class JdbcDriverDatabaseProviderFactory implements DatabaseProviderFactory {
    private final DriverNameExtractor driverNameExtractor;

    public JdbcDriverDatabaseProviderFactory(DriverNameExtractor driverNameExtractor) {
        this.driverNameExtractor = checkNotNull(driverNameExtractor);
    }

    public DatabaseProvider getDatabaseProvider(DataSource dataSource, DatabaseType databaseType, String schema) {
        final String driverName = getDriverName(dataSource);
        for (DatabaseProviderFactoryEnum dbProviderFactory : DatabaseProviderFactoryEnum.values()) {
            if (dbProviderFactory.accept(databaseType, driverName)) {
                return dbProviderFactory.getDatabaseProvider(dataSource, schema);
            }
        }
        throw new DatabaseProviderNotFoundException(driverName);
    }

    private String getDriverName(DataSource dataSource) {
        return driverNameExtractor.getDriverName(dataSource);
    }

    /**
     * TODO fix, this is actually not tested and needs to be. This is possibly quite fragile as well, as I don't know
     * yet what happens to the driver name when using connection pooling, but I'd bet it changes to something that doesn't
     * give much information
     */
    private static enum DatabaseProviderFactoryEnum {
        MYSQL(DatabaseType.MYSQL, "mysql", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new MySQLDatabaseProvider(getDisposableDataSource(dataSource));
            }
        },
        DERBY_NETWORK(DatabaseType.DERBY_NETWORK, "derby", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new ClientDerbyDatabaseProvider(getDisposableDataSource(dataSource));
            }
        },
        DERBY_EMBEDDED(DatabaseType.DERBY_EMBEDDED, "derby", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new EmbeddedDerbyDatabaseProvider(getDisposableDataSource(dataSource), "a-fake-uri"); // TODO handle the URI issue
            }
        },
        ORACLE(DatabaseType.ORACLE, "oracle", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new OracleDatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        POSTGRESQL(DatabaseType.POSTGRESQL, "postgres", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new PostgreSQLDatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        MSSQL(DatabaseType.MS_SQL, "sqlserver", true) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new MsJdbcSQLServerDatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        MSSQL_JTDS(DatabaseType.MS_SQL, "jtds", true) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new SQLServerDatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        HSQLDB(DatabaseType.HSQL, "hsql", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new HSQLDatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        H2(DatabaseType.H2, "h2", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new H2DatabaseProvider(getDisposableDataSource(dataSource), schema);
            }
        },
        NUODB(DatabaseType.NUODB, "nuodb", false) {
            @Override
            public DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema) {
                return new NuoDBDatabaseProvider(NuoDBDisposableDataSourceHandler.newInstance(dataSource), schema);
            }
        };

        private final DatabaseType databaseType;
        private final String driverName;
        private final boolean needsDatabaseTypeAndDriverName;

        DatabaseProviderFactoryEnum(DatabaseType databaseType, String driverName, boolean needsDatabaseTypeAndDriverName) {
            this.databaseType = checkNotNull(databaseType);
            this.driverName = checkNotNull(driverName);
            this.needsDatabaseTypeAndDriverName = needsDatabaseTypeAndDriverName;
        }

        boolean accept(DatabaseType otherDatabaseType, String otherDriverName) {
            final boolean acceptDatabaseType = acceptDatabaseType(otherDatabaseType);
            final boolean acceptDriverName = acceptDriverName(otherDriverName);
            if (needsDatabaseTypeAndDriverName) {
                return acceptDatabaseType && acceptDriverName;
            } else {
                return acceptDatabaseType || acceptDriverName;
            }
        }

        private boolean acceptDatabaseType(DatabaseType otherDatabaseType) {
            return databaseType.equals(otherDatabaseType);
        }

        private boolean acceptDriverName(String otherDriverName) {
            return otherDriverName != null && toLowerCase(otherDriverName).contains(this.driverName);
        }

        // apparently useless, I know, but the compiler complains if not there
        public abstract DatabaseProvider getDatabaseProvider(DataSource dataSource, String schema);
    }

    private static DisposableDataSource getDisposableDataSource(final DataSource dataSource) {
        return DelegatingDisposableDataSourceHandler.newInstance(dataSource, new Disposable() {
            @Override
            public void dispose() {
            }
        });
    }
}