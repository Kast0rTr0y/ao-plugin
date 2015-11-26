package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.DatabaseProvider;
import net.java.ao.db.ClientDerbyDatabaseProvider;
import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.MsJdbcSQLServerDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.db.SQLServerDatabaseProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.JdbcDriverDatabaseProviderFactory}
 */

@RunWith(MockitoJUnitRunner.class)
public class JdbcDriverDatabaseProviderFactoryTest {
    private static final String SOME_UNKOWN_DRIVER = "some unknown driver";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DatabaseProviderFactory databaseProviderFactory;
    @Mock
    private DriverNameExtractor driverNameExtractor;

    @Before
    public void setUp() throws Exception {
        databaseProviderFactory = new JdbcDriverDatabaseProviderFactory(driverNameExtractor);
    }

    @After
    public void tearDown() throws Exception {
        databaseProviderFactory = null;
        driverNameExtractor = null;
    }

    @Test
    public void testGetDatabaseProviderForUnknownDriver() throws Exception {
        expectedException.expect(DatabaseProviderNotFoundException.class);
        databaseProviderFactory.getDatabaseProvider(getMockDataSource(SOME_UNKOWN_DRIVER), DatabaseType.UNKNOWN, null);
    }

    @Test
    public void testGetDatabaseProviderForMySqlDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(MySQLDatabaseProvider.class, "MySQL-AB JDBC Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForMySqlDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(MySQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.MYSQL);
    }

    @Test
    public void testGetDatabaseProviderForClientDerbyDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(ClientDerbyDatabaseProvider.class, "Apache Derby Embedded JDBC Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForClientDerbyDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(ClientDerbyDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.DERBY_NETWORK);
    }

    @Test
    @Ignore
    public void testGetDatabaseProviderForEmbeddedDerbyDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(EmbeddedDerbyDatabaseProvider.class, "org.apache.derby.jdbc.EmbeddedDriver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForEmbeddedDerbyDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(EmbeddedDerbyDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.DERBY_EMBEDDED);
    }

    @Test
    public void testGetDatabaseProviderForOracleDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(OracleDatabaseProvider.class, "Oracle JDBC driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForOracleDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(OracleDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.ORACLE);
    }

    @Test
    public void testGetDatabaseProviderForPostgresDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(PostgreSQLDatabaseProvider.class, "PostgreSQL Native Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForPostgresDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(PostgreSQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.POSTGRESQL);
    }

    @Test
    public void testGetDatabaseProviderForMsSqlDriverMsSqlDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(MsJdbcSQLServerDatabaseProvider.class, "Microsoft JDBC Driver 4.2 for SQL Server", DatabaseType.MS_SQL);
    }

    @Test
    public void testGetDatabaseProviderForJtdsDriverMsSqlDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "jTDS Type 4 JDBC Driver for MS SQL Server and Sybase", DatabaseType.MS_SQL);
    }

    @Test
    public void testGetDatabaseProviderForUnknownDriverMsSqlDatabaseType() throws Exception {
        expectedException.expect(DatabaseProviderNotFoundException.class);
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.MS_SQL);
    }

    @Test
    public void testGetDatabaseProviderForMsSqlDriverUnknownDatabaseType() throws Exception {
        expectedException.expect(DatabaseProviderNotFoundException.class);
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "Microsoft JDBC Driver 4.2 for SQL Server", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForJtdsDriverUnknownDatabaseType() throws Exception {
        expectedException.expect(DatabaseProviderNotFoundException.class);
        testGetProviderOfTypeForDriverClassName(SQLServerDatabaseProvider.class, "jTDS Type 4 JDBC Driver for MS SQL Server and Sybase", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForHsqlDriver() throws Exception {
        testGetProviderOfTypeForDriverClassName(HSQLDatabaseProvider.class, "HSQL Database Engine Driver", DatabaseType.UNKNOWN);
    }

    @Test
    public void testGetDatabaseProviderForHsqlDatabaseType() throws Exception {
        testGetProviderOfTypeForDriverClassName(HSQLDatabaseProvider.class, SOME_UNKOWN_DRIVER, DatabaseType.HSQL);
    }

    private void testGetProviderOfTypeForDriverClassName(Class<? extends DatabaseProvider> providerClass, String driver, DatabaseType databaseType) throws Exception {
        final DataSource dataSource = getMockDataSource(driver);

        final DatabaseProvider provider = databaseProviderFactory.getDatabaseProvider(dataSource, databaseType, null);
        assertNotNull(provider);
        assertEquals(providerClass, provider.getClass());
    }

    private DataSource getMockDataSource(String driver) throws SQLException {
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
