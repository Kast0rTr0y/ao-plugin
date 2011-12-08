package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class TestBooleanBackup extends AbstractTestTypeBackup
{
    @Test
    @NonTransactional
    public void testSimpleEntityWithTrue() throws Exception
    {
        testBackupWithValue(true);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithFalse() throws Exception
    {
        testBackupWithValue(false);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Boolean value) throws Exception
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
        public Boolean getValue();

        public void setValue(Boolean value);
    }
}