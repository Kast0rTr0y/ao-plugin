package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public final class TestDoubleBackup extends AbstractTestTypeBackup
{
    @Test
    @NonTransactional
    public void testSimpleEntityWithNegativeValue() throws Exception
    {
        testBackupWithValue(-1.8d);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithZero() throws Exception
    {
        testBackupWithValue(0d);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithPositiveValue() throws Exception
    {
        testBackupWithValue(1.2d);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Double value) throws Exception
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
                final Double actual = em.get(SimpleEntity.class, eId.get()).getValue();
                assertEquals(value, actual);
            }
        });
    }

    public static interface SimpleEntity extends Entity
    {
        public Double getValue();

        public void setValue(Double value);
    }
}