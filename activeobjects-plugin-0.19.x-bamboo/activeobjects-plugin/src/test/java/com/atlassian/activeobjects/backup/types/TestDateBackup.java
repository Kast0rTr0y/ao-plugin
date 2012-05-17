package com.atlassian.activeobjects.backup.types;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class TestDateBackup extends AbstractTestTypeBackup
{
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE = "2011-11-27 15:47:31";

    @Test
    @NonTransactional
    public void testSimpleEntityWithLegalDate() throws Exception
    {
        testBackupWithValue(new SimpleDateFormat(DATE_FORMAT).parse(DATE));
    }

    @Test
    @NonTransactional
    public void testSimpleEntityWithNull() throws Exception
    {
        testBackupWithValue(null);
    }

    private void testBackupWithValue(final Date value) throws Exception
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
                final Date actual = em.get(SimpleEntity.class, eId.get()).getValue();
                assertEquals(formatDate(value), formatDate(actual));
            }

            private String formatDate(Date date)
            {
                return date == null ? null : new SimpleDateFormat(DATE_FORMAT).format(date);
            }
        });
    }

    public static interface SimpleEntity extends Entity
    {
        public Date getValue();

        public void setValue(Date value);
    }
}