package com.atlassian.activeobjects.internal;

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
import java.sql.SQLException;

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
        final String driverClassName = "com.example.jdbc.SomeUnkownDriver";
        try
        {
            databaseProviderFactory.getDatabaseProvider(getMockDataSource(driverClassName));
            fail("Should have thrown " + DatabaseProviderNotFoundException.class.getName());
        }
        catch (DatabaseProviderNotFoundException e)
        {
            assertEquals(driverClassName, e.getDriverClassName());
        }
    }

    @Test
    public void testGetDatabaseProviderForMySqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(MySQLDatabaseProvider.class, "com.mysql.jdbc.Driver");
    }

    @Test
    public void testGetDatabaseProviderForClientDerbyDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(ClientDerbyDatabaseProvider.class, "org.apache.derby.jdbc.ClientDriver");
    }

    @Test
    @Ignore
    public void testGetDatabaseProviderForEmbeddedDerbyDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(EmbeddedDerbyDatabaseProvider.class, "org.apache.derby.jdbc.EmbeddedDriver");
    }

    @Test
    public void testGetDatabaseProviderForOracleDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(OracleDatabaseProvider.class, "oracle.jdbc.OracleDriver");
    }

    @Test
    public void testGetDatabaseProviderForPostgresDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(PostgreSQLDatabaseProvider.class, "org.postgresql.Driver");
    }

    @Test
    public void testGetDatabaseProviderForMsSqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    @Test
    public void testGetDatabaseProviderForJtdsDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "net.sourceforge.jtds.jdbc.Driver");
    }

    @Test
    public void testGetDatabaseProviderForHsqlDriver() throws Exception
    {
        testGetProviderOfTypeForDriverClassName(HSQLDatabaseProvider.class, "org.hsqldb.jdbcDriver");
    }

    private void testGetProviderOfTypeForDriverClassName(Class<? extends DatabaseProvider> providerClass, String driver) throws Exception
    {
        final DataSource dataSource = getMockDataSource(driver);

        final DatabaseProvider provider = databaseProviderFactory.getDatabaseProvider(dataSource);
        assertNotNull(provider);
        assertEquals(providerClass, provider.getClass());
    }

    private DataSource getMockDataSource(String driver) throws SQLException
    {
        final DataSource dataSource = mock(DataSource.class);
        when(driverNameExtractor.getDriverName(dataSource)).thenReturn(driver);
        return dataSource;
    }
}
