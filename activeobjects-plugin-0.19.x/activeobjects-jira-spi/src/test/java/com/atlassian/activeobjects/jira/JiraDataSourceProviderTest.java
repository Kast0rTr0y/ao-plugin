package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.jira.ofbiz.OfBizConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;

import java.sql.Connection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Testing {@link com.atlassian.activeobjects.jira.JiraDataSourceProviderTest} */
@RunWith(MockitoJUnitRunner.class)
public class JiraDataSourceProviderTest
{
    private JiraDataSourceProvider dataSourceProvider;

    @Mock
    private OfBizConnectionFactory ofBizConnectionFactory;

    @Mock
    private JiraDatabaseTypeExtractor jiraDatabaseTypeExtractor;

    @Before
    public void setUp() throws Exception
    {
        dataSourceProvider = new JiraDataSourceProvider(ofBizConnectionFactory, jiraDatabaseTypeExtractor);
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
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.CLOUDSCAPE);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.FIREBIRD);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.H2);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.SAP_DB);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.SAP_DB_7_6);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.UNKNOWN, DatabaseTypeFactory.SYBASE);
    }

    @Test
    public void testGetDb2DatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.DB2, DatabaseTypeFactory.DB2);
    }

    @Test
    public void testGetHsqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.HSQL, DatabaseTypeFactory.HSQL);
    }

    @Test
    public void testGetMySqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.MYSQL, DatabaseTypeFactory.MYSQL);
    }

    @Test
    public void testGetPostgreSqlDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.POSTGRESQL, DatabaseTypeFactory.POSTGRES);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.POSTGRESQL, DatabaseTypeFactory.POSTGRES_7_2);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.POSTGRESQL, DatabaseTypeFactory.POSTGRES_7_3);
    }

    @Test
    public void testGetOracleDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.ORACLE, DatabaseTypeFactory.ORACLE_8I);
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.ORACLE, DatabaseTypeFactory.ORACLE_10G);
    }

    @Test
    public void testGetSqlServerDatabaseType() throws Exception
    {
        assertDatabaseTypeFromJiraDatabaseType(DatabaseType.MS_SQL, DatabaseTypeFactory.MSSQL);
    }

    private void assertDatabaseTypeFromJiraDatabaseType(DatabaseType databaseType, org.ofbiz.core.entity.jdbc.dbtype.DatabaseType value) throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(ofBizConnectionFactory.getConnection()).thenReturn(connection);
        when(jiraDatabaseTypeExtractor.getDatabaseType(connection)).thenReturn(value);
        assertEquals(databaseType, dataSourceProvider.getDatabaseType());
    }
}
