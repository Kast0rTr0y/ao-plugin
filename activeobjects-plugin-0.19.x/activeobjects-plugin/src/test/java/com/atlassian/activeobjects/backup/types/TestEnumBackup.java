package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public final class TestEnumBackup extends AbstractTestTypeBackup
{
    @Test
    @NonTransactional
    public void testSimpleEntityWithValueOne() throws Exception
    {
        testBackupWithValue(Value.ONE);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithValueTwo() throws Exception
    {
        testBackupWithValue(Value.TWO);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Value value) throws Exception
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

    public static interface SimpleEntity extends Entity
    {
        public Value getValue();

        public void setValue(Value value);
    }

    public static enum Value
    {
        ONE, TWO
    }
}