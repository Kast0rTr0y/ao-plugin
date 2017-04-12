package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.SqlServerTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlServerTest.class)
public class TestSqlServerActiveObjectBackup extends ActiveObjectsBackupDataSetup {
    @Test
    @NonTransactional
    public void testJtdcSqlServerBackup() throws Exception {
        testBackup(SQLSERVER_JTDS, SQL_SERVER_DATA_JTDS);
    }

    @Test
    @NonTransactional
    public void testMsjdbcSqlServerBackup() throws Exception {
        testBackup(SQLSERVER_MSJDBC, SQL_SERVER_DATA_MSJDBC);
    }

}
