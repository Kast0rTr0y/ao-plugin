package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.confluence.hibernate.DialectExtractor;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.hibernate.PluginHibernateSessionFactory;
import net.sf.hibernate.dialect.DB2390Dialect;
import net.sf.hibernate.dialect.DB2400Dialect;
import net.sf.hibernate.dialect.DB2Dialect;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.dialect.HSQLDialect;
import net.sf.hibernate.dialect.MySQLDialect;
import net.sf.hibernate.dialect.Oracle9Dialect;
import net.sf.hibernate.dialect.OracleDialect;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.dialect.SQLServerDialect;
import net.sf.hibernate.dialect.SQLServerIntlDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

/**
 * Testing {@link com.atlassian.activeobjects.confluence.ConfluenceDataSourceProvider}
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfluenceDataSourceProviderTest
{
    private ConfluenceDataSourceProvider dataSourceProvider;

    @Mock
    private PluginHibernateSessionFactory sessionFactory;

    @Mock
    private DialectExtractor dialectExtrator;

    @Before
    public void setUp() throws Exception
    {
        dataSourceProvider = new ConfluenceDataSourceProvider(sessionFactory, dialectExtrator);
    }

    @After
    public void tearDown() throws Exception
    {
        dataSourceProvider = null;
    }

    @Test
    public void testGetUnknownDatabaseTypeWithNullDialect() throws Exception
    {
        assertDatabaseTypeForDialect(DatabaseType.UNKNOWN, null);
    }

    @Test
    public void testGetHsqlDatabaseTypeWithHsqlDialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.HSQL, HSQLDialect.class);
    }

    @Test
    public void testGetMySqlDatabaseTypeWithMySqlDialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.MYSQL, com.atlassian.hibernate.dialect.MySQLDialect.class);
        assertDatabaseTypeForDialect(DatabaseType.MYSQL, MySQLDialect.class);
    }

    @Test
    public void testGetPostgresDatabaseTypeWithPostgresDialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.POSTGRES, PostgreSQLDialect.class);
    }

    @Test
    public void testGetOracleDatabaseTypeWithOracleDialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.ORACLE, OracleDialect.class);
        assertDatabaseTypeForDialect(DatabaseType.ORACLE, Oracle9Dialect.class);
    }

    @Test
    public void testGetMsSqlDatabaseTypeWithMsSqlDialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.MS_SQL, SQLServerDialect.class);
        assertDatabaseTypeForDialect(DatabaseType.MS_SQL, SQLServerIntlDialect.class);
    }

    @Test
    public void testGetDB2DatabaseTypeWithDB2Dialect()
    {
        assertDatabaseTypeForDialect(DatabaseType.DB2, com.atlassian.hibernate.dialect.DB2Dialect.class);
        assertDatabaseTypeForDialect(DatabaseType.DB2, DB2Dialect.class);
        assertDatabaseTypeForDialect(DatabaseType.DB2, DB2390Dialect.class);
        assertDatabaseTypeForDialect(DatabaseType.DB2, DB2400Dialect.class);
    }

    private void assertDatabaseTypeForDialect(DatabaseType databaseType, Class<? extends Dialect> dialect)
    {
        doReturn(dialect).when(dialectExtrator).getDialect();
        assertEquals(databaseType, dataSourceProvider.getDatabaseType());
    }
}
