package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfigurationFactory;
import com.atlassian.activeobjects.ao.PrefixedTableNameConverterFactory;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.sql.DataSource;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.EntityManagerFactoryImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityManagerFactoryImplTest
{
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private DatabaseProviderFactory databaseProviderFactory;
    @Mock
    private PrefixedTableNameConverterFactory tableNameConverterFactory;

    @Mock
    private TableNameConverter tableNameConverter;

    @Mock
    private FieldNameConverter fieldNameConverter;

    @Mock
    private PrefixedSchemaConfigurationFactory schemaConfigurationFactory;

    @Mock
    private SchemaConfiguration schemaConfiguration;

    @Before
    public void setUp() throws Exception
    {
        when(tableNameConverterFactory.getTableNameConverter(anyPrefix())).thenReturn(tableNameConverter);
        when(schemaConfigurationFactory.getSchemaConfiguration(anyPrefix())).thenReturn(schemaConfiguration);
        entityManagerFactory = new EntityManagerFactoryImpl(databaseProviderFactory, tableNameConverterFactory, fieldNameConverter, schemaConfigurationFactory);
    }

    @After
    public void tearDown() throws Exception
    {
        entityManagerFactory = null;
        databaseProviderFactory = null;
    }

    @Test
    public void testGetEntityManager() throws Exception
    {
        final DataSource dataSource = mock(DataSource.class);
        final DatabaseType databaseType = DatabaseType.UNKNOWN;
        final DatabaseProvider databaseProvider = mock(DatabaseProvider.class);
        final ActiveObjectsConfiguration configuration = mock(ActiveObjectsConfiguration.class);

        when(databaseProviderFactory.getDatabaseProvider(dataSource, databaseType)).thenReturn(databaseProvider);
        assertNotNull(entityManagerFactory.getEntityManager(dataSource, databaseType, configuration));

        verify(databaseProviderFactory).getDatabaseProvider(dataSource, databaseType);
    }

    private static Prefix anyPrefix()
    {
        return Mockito.any();
    }
}
