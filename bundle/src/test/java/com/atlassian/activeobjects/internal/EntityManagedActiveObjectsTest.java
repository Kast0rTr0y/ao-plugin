package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.TransactionCallback;
import com.atlassian.activeobjects.external.TransactionStatus;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityManagedActiveObjectsTest
{
    private EntityManagedActiveObjects activeObjects;

    @Mock
    private EntityManager entityManager;

    @Before
    public void setUp()
    {
        activeObjects = new EntityManagedActiveObjects(entityManager)
        {
        };
    }

    @Test
    public void testExecuteInTransaction() throws Exception
    {
        final DatabaseProvider databaseProvider = mock(DatabaseProvider.class);
        final Connection connection = mock(Connection.class);

        when(entityManager.getProvider()).thenReturn(databaseProvider);
        when(databaseProvider.getConnection()).thenReturn(connection);

        @SuppressWarnings({"unchecked"}) final TransactionCallback<Object> callback = mock(TransactionCallback.class);
        activeObjects.executeInTransaction(callback);

        verify(callback).doInTransaction(Mockito.<TransactionStatus>anyObject());
    }
}
