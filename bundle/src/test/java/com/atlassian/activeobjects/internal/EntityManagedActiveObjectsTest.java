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
import java.sql.Driver;
import java.sql.SQLException;
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

    @Before
    public void setUp()
    {
        activeObjects = new EntityManagedActiveObjects(entityManager, "test.plugin.key")
        {
        };
    }

    @Test
    public void testExecuteInTransaction() throws Exception
    {
        final Connection connection = mock(Connection.class);
        final DatabaseProvider databaseProvider = getDatabaseProvider(connection);

        when(entityManager.getProvider()).thenReturn(databaseProvider);

        @SuppressWarnings({"unchecked"}) final TransactionCallback<Object> callback = mock(TransactionCallback.class);
        activeObjects.executeInTransaction(callback);

        verify(callback).doInTransaction(Mockito.<TransactionStatus>anyObject());
    }

    ///CLOVER:OFF
    private DatabaseProvider getDatabaseProvider(final Connection connection)
    {
        return new DatabaseProvider("", "", "")
        {
            @Override
            public Class<? extends Driver> getDriverClass() throws ClassNotFoundException
            {
                return null;
            }

            @Override
            protected Set<String> getReservedWords()
            {
                return null;
            }

            @Override
            protected Connection getConnectionImpl() throws SQLException
            {
                return connection;
            }
        };
    }
}
