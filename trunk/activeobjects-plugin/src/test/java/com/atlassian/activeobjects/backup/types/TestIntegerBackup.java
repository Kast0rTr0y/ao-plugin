package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.0
 */
public class TestIntegerBackup extends AbstractTestTypeBackup
{
    @Test
    public void testAutoIncrementId() throws Exception
    {
        testBackupType(new BackupType<Integer>()
        {
            @Override
            public Class<? extends RawEntity<Integer>> getEntityClass()
            {
                return AutoIncrementId.class;
            }

            @Override
            public void createData(EntityManager em) throws Exception
            {
                em.create(AutoIncrementId.class);
            }

            @Override
            public void checkData(EntityManager em) throws Exception
            {
                assertEquals(1, em.find(AutoIncrementId.class).length);
            }
        });
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithValue() throws Exception
    {
        testBackupWithValue(123);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Integer value) throws Exception
    {
        final AtomicInteger e1Id = new AtomicInteger(-1);
        testBackupType(new BackupType<Integer>()
        {
            @Override
            public Class<? extends RawEntity<Integer>> getEntityClass()
            {
                return SimpleEntity.class;
            }

            @Override
            public void createData(EntityManager em) throws Exception
            {
                SimpleEntity e1 = em.create(SimpleEntity.class);
                e1.setValue(value);
                e1.save();
                e1Id.set(e1.getID());
            }

            @Override
            public void checkData(EntityManager em) throws Exception
            {
                assertEquals(value, em.get(SimpleEntity.class, e1Id.get()).getValue());
            }
        });
    }

    public static interface AutoIncrementId extends RawEntity<Integer>
    {
        @PrimaryKey
        @AutoIncrement
        public Integer getId();
    }

    public static interface SimpleEntity extends Entity
    {
        public Integer getValue();

        public void setValue(Integer value);
    }
}
