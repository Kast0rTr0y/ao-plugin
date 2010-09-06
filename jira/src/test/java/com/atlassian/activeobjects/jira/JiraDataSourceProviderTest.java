package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.DatabaseType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.jira.JiraDataSourceProviderTest}
 */
@RunWith(MockitoJUnitRunner.class)
public class JiraDataSourceProviderTest
{

    private JiraDataSourceProvider dataSourceProvider;

    @Mock
    private JiraDatabaseTypeExtractor databaseTypeExtractor;

    @Before
    public void setUp() throws Exception
    {
        dataSourceProvider = new JiraDataSourceProvider(databaseTypeExtractor);
    }

    @After
    public void tearDown() throws Exception
    {
        dataSourceProvider = null;
    }

    @Test
    public void testGetUnknownDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, null);
    }

    @Test
    public void testGetHsqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.HSQL, com.atlassian.jira.configurator.config.DatabaseType.HSQL);
    }

    @Test
    public void testGetMySqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.MYSQL, com.atlassian.jira.configurator.config.DatabaseType.MY_SQL);
    }

    @Test
    public void testGetPostgreSqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.POSTGRES, com.atlassian.jira.configurator.config.DatabaseType.POSTGRES);
    }

    @Test
    public void testGetOracleDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.ORACLE, com.atlassian.jira.configurator.config.DatabaseType.ORACLE);
    }

    @Test
    public void testGetSqlServerDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.MS_SQL, com.atlassian.jira.configurator.config.DatabaseType.SQL_SERVER);
    }

    private void assertDatabaseTypeFromJiraDatabaseType(DatabaseType databaseType, com.atlassian.jira.configurator.config.DatabaseType value)
    {
        when(databaseTypeExtractor.getDatabaseType()).thenReturn(value);
        assertEquals(databaseType, dataSourceProvider.getDatabaseType());
    }
}
