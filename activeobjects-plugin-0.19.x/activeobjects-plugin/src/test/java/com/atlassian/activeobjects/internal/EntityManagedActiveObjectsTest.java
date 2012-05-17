package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Sets;
import net.java.ao.DatabaseProvider;
import net.java.ao.DisposableDataSource;
import net.java.ao.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityManagedActiveObjectsTest
{
    private EntityManagedActiveObjects activeObjects;

    @Mock
    private EntityManager entityManager;
    @Mock
    private TransactionManager transactionManager;

    @Before
    public void setUp()
    {
        activeObjects = new EntityManagedActiveObjects(entityManager, transactionManager);
    }

    @Test
    public void testExecuteInTransaction() throws Exception
    {
        final DatabaseProvider databaseProvider = mockProvider();
        when(entityManager.getProvider()).thenReturn(databaseProvider);

        @SuppressWarnings({"unchecked"}) final TransactionCallback<Object> callback = mock(TransactionCallback.class);
        activeObjects.executeInTransaction(callback);

        verify(transactionManager).doInTransaction(callback);
    }

    private DatabaseProvider mockProvider() throws Exception
    {
        final DisposableDataSource disposableDataSource = mock(DisposableDataSource.class);
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(disposableDataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getIdentifierQuoteString()).thenReturn("");
        
        return new DatabaseProvider(disposableDataSource, null)
        {
            @Override
            protected Set<String> getReservedWords()
            {
                return Sets.newHashSet();
            }
        };
    }
}
