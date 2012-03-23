package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.StringLength;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public final class TestClobBackup extends AbstractTestTypeBackup
{
    private static String SMALL_CLOB = "Some small sample";

    // over 4000 bytes, as Oracle has issues with that.
    private static String LARGE_CLOB;

    static
    {
        int size = 8100;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size / 10; i++)
        {
            sb.append("0123456789#");
        }
        LARGE_CLOB = sb.append(size).toString();
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithSmallClob() throws Exception
    {
        testBackupWithValue(SMALL_CLOB);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithLargeClob() throws Exception
    {
        testBackupWithValue(LARGE_CLOB);
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final String value) throws Exception
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
        @StringLength(StringLength.UNLIMITED)
        public String getValue();

        @StringLength(StringLength.UNLIMITED)
        public void setValue(String value);
    }
}