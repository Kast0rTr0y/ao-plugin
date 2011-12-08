package com.atlassian.activeobjects.backup.types;

import com.atlassian.activeobjects.backup.AbstractTestActiveObjectsBackup;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

abstract class AbstractTestTypeBackup extends AbstractTestActiveObjectsBackup
{
    final void testBackupType(BackupType<?> bt) throws Exception
    {
        entityManager.migrate(bt.getEntityClass());

        bt.createData(entityManager);
        entityManager.flushAll();
        bt.checkData(entityManager);

        final String backup = save();
        logger.debug("\n" + backup);

        entityManager.migrate(); // emptying the DB
        entityManager.flushAll();

        restore(backup);
        restore(backup);

        entityManager.migrate(bt.getEntityClass());
        bt.checkData(entityManager);
    }

    static interface BackupType<K>
    {
        Class<? extends RawEntity<K>> getEntityClass();

        void createData(EntityManager em) throws Exception;

        void checkData(EntityManager em) throws Exception;
    }
}
