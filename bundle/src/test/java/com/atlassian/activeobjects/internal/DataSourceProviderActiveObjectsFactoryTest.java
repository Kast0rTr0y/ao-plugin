package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.sal.api.sql.DataSourceProvider;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import net.java.ao.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.sql.DataSource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
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
    private DataSourceProvider dataSourceProvider;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Before
    public void setUp()
    {
        activeObjectsFactory = new DataSourceProviderActiveObjectsFactory(entityManagerFactory, dataSourceProvider, transactionTemplate);
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
        try
        {
            activeObjectsFactory.create(null);
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
        when(entityManagerFactory.getEntityManager(dataSource)).thenReturn(entityManager);

        assertNotNull(activeObjectsFactory.create(null));
        verify(entityManagerFactory).getEntityManager(dataSource);
    }
}
