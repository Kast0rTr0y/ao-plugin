package com.atlassian.activeobjects.internal;

import net.java.ao.DatabaseProvider;
import net.java.ao.builder.DelegatingDisposableDataSource;
import net.java.ao.db.ClientDerbyDatabaseProvider;
import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.db.SQLServerDatabaseProvider;

import javax.sql.DataSource;
import java.util.Locale;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public final class JdbcDriverDatabaseProviderFactory implements DatabaseProviderFactory
{
    private DriverNameExtractor driverNameExtractor;

    public JdbcDriverDatabaseProviderFactory(DriverNameExtractor driverNameExtractor)
    {
        this.driverNameExtractor = checkNotNull(driverNameExtractor);
    }

    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
    {
        final String driverName = getDriverName(dataSource);

        for (DatabaseProviderFactoryEnum dbProviderFactory : DatabaseProviderFactoryEnum.values())
        {
            if (dbProviderFactory.accept(driverName))
            {
                return dbProviderFactory.getDatabaseProvider(dataSource);
            }
        }
        throw new DatabaseProviderNotFoundException(driverName);
    }

    private String getDriverName(DataSource dataSource)
    {
        return driverNameExtractor.getDriverName(dataSource);
    }

    /**
     * TODO fix, this is actually not tested and needs to be. This is possibly quite fragile as well, as I don't know
     * yet what happens to the driver name when using connection pooling, but I'd bet it changes to something that doesn't
     * give much information
     */
    private enum DatabaseProviderFactoryEnum implements DatabaseProviderFactory
    {
        MYSQL("mysql")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new MySQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        DERBY_NETWORK("derby")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new ClientDerbyDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        DERBY_EMBEDDED("derby")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new EmbeddedDerbyDatabaseProvider(getDisposableDataSource(dataSource), "a-fake-uri"); // TODO handle the URI issue
                    }
                },
        ORACLE("oracle")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new OracleDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        POSTGRESQL("postgres")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new PostgreSQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        MSSQL("sqlserver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new SQLServerDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        MSSQL_JTDS("jtds")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new SQLServerDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        HSQLDB("hsql")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new HSQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                };

        private final String driverName;

        DatabaseProviderFactoryEnum(String driverName)
        {
            this.driverName = checkNotNull(driverName);
        }

        boolean accept(String otherDriverName)
        {
            return otherDriverName != null && otherDriverName.toLowerCase(Locale.ENGLISH).contains(this.driverName);
        }

        // apparently useless, I know, but the compiler complains if not there
        public abstract DatabaseProvider getDatabaseProvider(DataSource dataSource);
    }

    private static DelegatingDisposableDataSource getDisposableDataSource(final DataSource dataSource)
    {
        return new DelegatingDisposableDataSource(dataSource)
        {
            public void dispose()
            {
                // do nothing
            }
        };
    }
}
