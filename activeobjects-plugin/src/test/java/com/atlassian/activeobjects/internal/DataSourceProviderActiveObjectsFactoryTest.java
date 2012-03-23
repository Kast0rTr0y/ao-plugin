package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import net.java.ao.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.sql.DataSource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.DataSourceProviderActiveObjectsFactory}
 */
@RunWith(MockitoJUnitRunner.class)
public class DataSourceProviderActiveObjectsFactoryTest
{
    private ActiveObjectsFactory activeObjectsFactory;

    @Mock
    private ActiveObjectUpgradeManager upgradeManager;

    @Mock
    private DataSourceProvider dataSourceProvider;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ActiveObjectsConfiguration configuration;

    @Before
    public void setUp()
    {
        activeObjectsFactory = new DataSourceProviderActiveObjectsFactory(upgradeManager, entityManagerFactory, dataSourceProvider, transactionTemplate);
    }

    @After
    public void tearDown()
    {
        activeObjectsFactory = null;
        entityManagerFactory = null;
        dataSourceProvider = null;
    }

    @Test
    public void testCreateWithNullDataSource() throws Exception
    {
        when(dataSourceProvider.getDataSource()).thenReturn(null); // not really needed, but just to make the test clear
        when(configuration.getDataSourceType()).thenReturn(DataSourceType.APPLICATION);
        try
        {
            activeObjectsFactory.create(configuration);
            fail("Should have thrown " + ActiveObjectsPluginException.class.getName());
        }
        catch (ActiveObjectsPluginException e)
        {
            // ignored
        }
    }

    @Test
    public void testCreateWithNonNullDataSource() throws Exception
    {
        final DataSource dataSource = mock(DataSource.class);
        final EntityManager entityManager = mock(EntityManager.class);

        when(dataSourceProvider.getDataSource()).thenReturn(dataSource);
        when(entityManagerFactory.getEntityManager(anyDataSource(), anyDatabaseType(), anyString(), anyConfiguration())).thenReturn(entityManager);
        when(configuration.getDataSourceType()).thenReturn(DataSourceType.APPLICATION);

        assertNotNull(activeObjectsFactory.create(configuration));
        verify(entityManagerFactory).getEntityManager(anyDataSource(), anyDatabaseType(), anyString(), anyConfiguration());
    }

    private static DataSource anyDataSource()
    {
        return Mockito.anyObject();
    }

    private static DatabaseType anyDatabaseType()
    {
        return Mockito.anyObject();
    }

    private static ActiveObjectsConfiguration anyConfiguration()
    {
        return Mockito.anyObject();
    }
}
