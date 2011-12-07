package com.atlassian.activeobjects.backup;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.PrimaryKey;
import org.junit.Test;

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

    public static interface AutoIncrementId extends RawEntity<Long>
    {
        @PrimaryKey
        @AutoIncrement
        public Long getId();
    }
}
