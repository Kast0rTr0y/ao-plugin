package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.DatabaseProvider;
import net.java.ao.db.ClientDerbyDatabaseProvider;
import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.db.SQLServerDatabaseProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.JdbcDriverDatabaseProviderFactory}
 */

// TODO fix the test so that drivers are actually loaded and we can test with their real name
@RunWith(MockitoJUnitRunner.class)
public class JdbcDriverDatabaseProviderFactoryTest
{
    private static final String SOME_UNKOWN_DRIVER = "com.example.jdbc.SomeUnkownDriver";

    private DatabaseProviderFactory databaseProviderFactory;
    @Mock
    private DriverNameExtractor driverNameExtractor;

    @Before
    public void setUp() throws Exception
    {
        databaseProviderFactory = new JdbcDriverDatabaseProviderFactory(driverNameExtractor);
    }

    @After
    public void tearDown() throws Exception
    {
        databaseProviderFactory = null;
        driverNameExtractor = null;
    }

    @Test
    public void testGetDatabaseProviderForUnknownDriver() throws Exception
    {
        try
        {
            databaseProviderFactory.getDatabaseProvider(getMockDataSource(SOME_UNKOWN_DRIVER), DatabaseType.UNKNOWN, null);
            fail("Should have thrown " + DatabaseProviderNotFoundException.class.getName());
        }
        catch (DatabaseProviderNotFoundException e)
        {
            assertEquals(SOME_UNKOWN_DRIVER, e.getDriverClassName());
        }
    }

    @Test
    public void testGetDatabaseProviderForMySqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(MySQLDatabaseProvider.class, "com.mysql.jdbc.Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForMySqlDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(MySQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.MYSQL);
    }

    @Test
    public void testGetDatabaseProviderForClientDerbyDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(ClientDerbyDatabaseProvider.class, "org.apache.derby.jdbc.ClientDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForClientDerbyDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(ClientDerbyDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.DERBY_NETWORK);
    }

    @Test
    @Ignore
    public void testGetDatabaseProviderForEmbeddedDerbyDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(EmbeddedDerbyDatabaseProvider.class, "org.apache.derby.jdbc.EmbeddedDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForEmbeddedDerbyDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(EmbeddedDerbyDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.DERBY_EMBEDDED);
    }

    @Test
    public void testGetDatabaseProviderForOracleDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(OracleDatabaseProvider.class, "oracle.jdbc.OracleDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForOracleDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(OracleDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.ORACLE);
    }

    @Test
    public void testGetDatabaseProviderForPostgresDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(PostgreSQLDatabaseProvider.class, "org.postgresql.Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForPostgresDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(PostgreSQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.POSTGRESQL);
    }

    @Test
    public void testGetDatabaseProviderForMsSqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "com.microsoft.sqlserver.jdbc.SQLServerDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForMsSqlDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.MS_SQL);
    }

    @Test
    public void testGetDatabaseProviderForJtdsDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "net.sourceforge.jtds.jdbc.Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForHsqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(HSQLDatabaseProvider.class, "org.hsqldb.jdbcDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForHsqlDatabaseType() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(HSQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.HSQL);
    }

    private void testGetProviderOfTypeForDriverClassName(Class<? extends DatabaseProvider> providerClass, String driver, DatabaseType databaseType) throws Exception
    {
        final DataSource dataSource = getMockDataSource(driver);

        final DatabaseProvider provider = databaseProviderFactory.getDatabaseProvider(dataSource, databaseType, null);
        assertNotNull(provider);
        assertEquals(providerClass, provider.getClass());
    }

    private DataSource getMockDataSource(String driver) throws SQLException
    {
        final DataSource dataSource = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        final Statement statement = mock(Statement.class);

        when(driverNameExtractor.getDriverName(dataSource)).thenReturn(driver);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getIdentifierQuoteString()).thenReturn("");
        return dataSource;
    }
}
