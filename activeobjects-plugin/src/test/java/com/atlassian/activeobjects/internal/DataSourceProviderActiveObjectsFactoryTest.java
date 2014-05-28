package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.PluginKey;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import com.atlassian.tenancy.api.Tenant;
import net.java.ao.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

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
    private DataSourceProviderActiveObjectsFactory activeObjectsFactory;

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
    
    @Mock 
    private TransactionSynchronisationManager transactionSynchronizationManager;

    @Mock
    private Tenant tenant;

    @Mock
    private ClusterLock clusterLock;

    @Before
    public void setUp()
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("com.example.plugin");
        PluginKey pluginKey = PluginKey.fromBundle(bundle);
        ClusterLockService clusterLockService = mock(ClusterLockService.class);
        when(clusterLockService.getLockForName(anyString())).thenReturn(clusterLock);
        when(configuration.getDataSourceType()).thenReturn(DataSourceType.APPLICATION);
        when(configuration.getPluginKey()).thenReturn(pluginKey);
        activeObjectsFactory = new DataSourceProviderActiveObjectsFactory(upgradeManager, entityManagerFactory,
                dataSourceProvider, transactionTemplate, clusterLockService);
        activeObjectsFactory.setTransactionSynchronizationManager(transactionSynchronizationManager);
        when(transactionTemplate.execute(Matchers.any(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return ((TransactionCallback<?>) invocation.getArguments()[0]).doInTransaction();
            }
        });
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
        when(dataSourceProvider.getDataSource(tenant)).thenReturn(null); // not really needed, but just to make the test clear
        try
        {
            activeObjectsFactory.create(configuration, tenant);
            fail("Should have thrown " + ActiveObjectsPluginException.class.getName());
        }
        catch (ActiveObjectsPluginException e)
        {
            verify(clusterLock).lock();
            verify(clusterLock).unlock();
        }
    }

    @Test
    public void testCreateWithNullDatabaseType() throws Exception
    {
        final DataSource dataSource = mock(DataSource.class);

        when(dataSourceProvider.getDataSource(tenant)).thenReturn(dataSource);
        when(dataSourceProvider.getDatabaseType(tenant)).thenReturn(null); // not really needed, but just to make the test clear
        try
        {
            activeObjectsFactory.create(configuration, tenant);
            fail("Should have thrown " + ActiveObjectsPluginException.class.getName());
        }
        catch (ActiveObjectsPluginException e)
        {
            verify(clusterLock).lock();
            verify(clusterLock).unlock();
        }
    }

    @Test
    public void testCreate() throws Exception
    {
        final DataSource dataSource = mock(DataSource.class);
        final EntityManager entityManager = mock(EntityManager.class);

        when(dataSourceProvider.getDataSource(tenant)).thenReturn(dataSource);
        when(entityManagerFactory.getEntityManager(anyDataSource(), anyDatabaseType(), anyString(), anyConfiguration())).thenReturn(entityManager);
        when(configuration.getDataSourceType()).thenReturn(DataSourceType.APPLICATION);
        when(dataSourceProvider.getDatabaseType(tenant)).thenReturn(DatabaseType.DERBY_EMBEDDED);

        assertNotNull(activeObjectsFactory.create(configuration, tenant));
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
