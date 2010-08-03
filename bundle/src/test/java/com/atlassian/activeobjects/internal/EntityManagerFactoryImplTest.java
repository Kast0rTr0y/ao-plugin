package com.atlassian.activeobjects.internal;

import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
    private TableNameConverter tableNameConverter;
    @Mock
    private FieldNameConverter fieldNameConverter;
    @Mock
    private SchemaConfiguration schemaConfiguration;

    @Before
    public void setUp() throws Exception
    {
        entityManagerFactory = new EntityManagerFactoryImpl(databaseProviderFactory, tableNameConverter, fieldNameConverter, schemaConfiguration);
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
        final DatabaseProvider databaseProvider = mock(DatabaseProvider.class);

        when(databaseProviderFactory.getDatabaseProvider(dataSource)).thenReturn(databaseProvider);
        assertNotNull(entityManagerFactory.getEntityManager(dataSource));

        verify(databaseProviderFactory).getDatabaseProvider(dataSource);
    }
}
