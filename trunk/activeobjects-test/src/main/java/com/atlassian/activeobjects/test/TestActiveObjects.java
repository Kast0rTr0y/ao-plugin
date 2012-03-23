package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.internal.EntityManagedActiveObjects;
import com.atlassian.activeobjects.internal.TransactionManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.EntityManager;

public final class TestActiveObjects extends EntityManagedActiveObjects
{
    public TestActiveObjects(EntityManager entityManager)
    {
        super(entityManager, new TransactionManager()
        {
            public <T> T doInTransaction(TransactionCallback<T> callback)
            {
                return callback.doInTransaction();
            }
        });
    }
}
