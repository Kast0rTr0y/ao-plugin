package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.HsqlTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(HsqlTest.class)
public class TestOracleActiveObjectBackup extends ActiveObjectsBackupDataSetup
{

    @Test
    @NonTransactional
    public void testOracleBackup() throws Exception
    {
        testBackup(ORACLE, ORACLE_DATA);
    }

    @Test
    @NonTransactional
    public void testLegacyOracleBackup() throws Exception
    {
        testBackup(LEGACY_ORACLE, LEGACY_ORACLE_DATA);
    }

}
