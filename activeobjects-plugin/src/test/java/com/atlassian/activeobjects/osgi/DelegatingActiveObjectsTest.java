package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Suppliers;
import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

import static org.mockito.Mockito.*;

/**
 * The main reason for this tests is to ensure that we use the  supplier.
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingActiveObjectsTest
{
    private ActiveObjects activeObjects;

    @Mock
    private ActiveObjects delegateActiveObjects;

    @Before
    public void setUp() throws Exception
    {
        activeObjects = new DelegatingActiveObjects(Suppliers.ofInstance(delegateActiveObjects));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMigrate() throws Exception
    {
        activeObjects.migrate(AnEntity.class);
        verify(delegateActiveObjects).migrate(AnEntity.class);
    }

    @Test
    public void testFlushAll() throws Exception
    {
        activeObjects.flushAll();
        verify(delegateActiveObjects).flushAll();
    }

    @Test
    public void testFlush() throws Exception
    {
        activeObjects.flush();
        verify(delegateActiveObjects).flush();
    }

    @Test
    public void testGetEntityKey() throws Exception
    {
        final Integer key = 1;
        activeObjects.get(AnEntity.class, key);

        verify(delegateActiveObjects).get(AnEntity.class, key);
    }

    @Test
    public void testGetEntityKeys() throws Exception
    {
        final Integer key1 = 1;
        final Integer key2 = 2;
        activeObjects.get(AnEntity.class, key1, key2);
        verify(delegateActiveObjects).get(AnEntity.class, key1, key2);
    }

    @Test
    public void testCreateEntityMap() throws Exception
    {
        final HashMap<String, Object> aMap = new HashMap<String, Object>();
        activeObjects.create(AnEntity.class, aMap);
        verify(delegateActiveObjects).create(AnEntity.class, aMap);
    }

    @Test
    public void testCreateEntityDBParams() throws Exception
    {
        final DBParam dbParam = new DBParam("field", "value");
        activeObjects.create(AnEntity.class, dbParam);
        verify(delegateActiveObjects).create(AnEntity.class, dbParam);
    }

    @Test
    public void testDelete() throws Exception
    {
        final AnEntity entity = new AnEntity();
        activeObjects.delete(entity);
        verify(delegateActiveObjects).delete(entity);
    }

    @Test
    public void testFindClass() throws Exception
    {
        activeObjects.find(AnEntity.class);
        verify(delegateActiveObjects).find(AnEntity.class);
    }

    @Test
    public void testFindClassCriteriaObjects() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final String criteria = "criteria";
        final Object param = new Object();

        activeObjects.find(type, criteria, param);
        verify(delegateActiveObjects).find(type, criteria, param);
    }

    @Test
    public void testFindClassQuery() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final Query query = Query.select();

        activeObjects.find(type, query);
        verify(delegateActiveObjects).find(type, query);
    }

    @Test
    public void testFindClassStringQuery() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final String field = "field";
        final Query query = Query.select();

        activeObjects.find(type, field, query);
        verify(delegateActiveObjects).find(type, field, query);
    }

    @Test
    public void testFindWithSQL() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final String keyField = "field";
        final String sql = "sql";
        final Object param = new Object();

        activeObjects.findWithSQL(type, keyField, sql, param);
        verify(delegateActiveObjects).findWithSQL(type, keyField, sql, param);
    }

    @Test
    public void testCountClass() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;

        activeObjects.count(type);
        verify(delegateActiveObjects).count(type);
    }

    @Test
    public void testCountClassStringObjects() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final String criteria = "criteria";
        final Object param = new Object();

        activeObjects.count(type, criteria, param);
        verify(delegateActiveObjects).count(type, criteria, param);
    }

    @Test
    public void testCountClassQuery() throws Exception
    {
        final Class<AnEntity> type = AnEntity.class;
        final Query query = Query.select();

        activeObjects.count(type, query);
        verify(delegateActiveObjects).count(type, query);
    }

    @Test
    public void testExecuteInTransaction() throws Exception
    {
        @SuppressWarnings({"unchecked"}) final TransactionCallback<Object> callback = mock(TransactionCallback.class);
        activeObjects.executeInTransaction(callback);
        verify(delegateActiveObjects).executeInTransaction(callback);
    }

    ///CLOVER:OFF

    private static class AnEntity implements RawEntity<Integer>
    {
        public void init()
        {

        }

        public void save()
        {

        }

        public EntityManager getEntityManager()
        {
            return null;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
        }

        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
        }

        public <X extends RawEntity<Integer>> Class<X> getEntityType()
        {
            return null;
        }
    }
}
