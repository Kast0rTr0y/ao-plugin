package com.atlassian.activeobjects.backup;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

abstract class AbstractTestTypeBackup extends AbstractTestActiveObjectsBackup
{
    final void testBackupType(BackupType<Long> bt) throws Exception
    {
        entityManager.migrate(bt.getEntityClass());

        bt.createData(entityManager);
        bt.checkData(entityManager);


        final String backup = save();
        logger.debug("\n" + backup);

        entityManager.migrate(); // emptying the DB

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
