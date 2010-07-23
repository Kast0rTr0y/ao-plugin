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

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public final class JdbcDriverDatabaseProviderFactory implements DatabaseProviderFactory
{
    private DriverClassNameExtractor driverClassNameExtractor;

    public JdbcDriverDatabaseProviderFactory(DriverClassNameExtractor driverClassNameExtractor)
    {
        this.driverClassNameExtractor = checkNotNull(driverClassNameExtractor);
    }

    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
    {
        final String driverClassName = getDriverClassName(dataSource);

        for (DatabaseProviderFactoryEnum dbProviderFactory : DatabaseProviderFactoryEnum.values())
        {
            if (dbProviderFactory.accept(driverClassName))
            {
                return dbProviderFactory.getDatabaseProvider(dataSource);
            }
        }
        throw new DatabaseProviderNotFoundException(driverClassName);
    }

    private String getDriverClassName(DataSource dataSource)
    {
        return driverClassNameExtractor.getDriverClassName(dataSource);
    }

    private enum DatabaseProviderFactoryEnum implements DatabaseProviderFactory
    {
        MYSQL("com.mysql.jdbc.Driver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new MySQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        DERBY_NETWORK("org.apache.derby.jdbc.ClientDriver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new ClientDerbyDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        DERBY_EMBEDDED("org.apache.derby.jdbc.EmbeddedDriver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new EmbeddedDerbyDatabaseProvider(getDisposableDataSource(dataSource), "a-fake-uri"); // TODO handle the URI issue
                    }
                },
        ORACLE("oracle.jdbc.OracleDriver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new OracleDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        POSTGRESQL("org.postgresql.Driver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new PostgreSQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new SQLServerDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        MSSQL_JTDS("net.sourceforge.jtds.jdbc.Driver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new SQLServerDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                },
        HSQLDB("org.hsqldb.jdbcDriver")
                {
                    public DatabaseProvider getDatabaseProvider(DataSource dataSource)
                    {
                        return new HSQLDatabaseProvider(getDisposableDataSource(dataSource));
                    }
                };

        private final String driverClassName;

        DatabaseProviderFactoryEnum(String driverClassName)
        {
            this.driverClassName = checkNotNull(driverClassName);
        }

        boolean accept(String otherDriverClassName)
        {
            return this.driverClassName.equals(otherDriverClassName);
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
