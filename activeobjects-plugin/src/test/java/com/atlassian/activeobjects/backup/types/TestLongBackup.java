package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class TestLongBackup extends AbstractTestTypeBackup
{
    @Test
    public void testAutoIncrementId() throws Exception
    {
        testBackupType(new BackupType<Long>()
        {
            @Override
            public Class<? extends RawEntity<Long>> getEntityClass()
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
        testBackupWithValue(123l);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Long value) throws Exception
    {
        final AtomicInteger eId = new AtomicInteger(-1);
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
                SimpleEntity e = em.create(SimpleEntity.class);
                e.setValue(value);
                e.save();
                eId.set(e.getID());
            }

            @Override
            public void checkData(EntityManager em) throws Exception
            {
                assertEquals(value, em.get(SimpleEntity.class, eId.get()).getValue());
            }
        });
    }

    public static interface AutoIncrementId extends RawEntity<Long>
    {
        @PrimaryKey
        @AutoIncrement
        public Long getId();
    }

    public static interface SimpleEntity extends Entity
    {
        public Long getValue();

        public void setValue(Long value);
    }
}
