package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.SqlServerTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlServerTest.class)
public class TestSqlServerActiveObjectBackup extends ActiveObjectsBackupDataSetup {
    @Test
    @NonTransactional
    public void testSqlServerBackup() throws Exception {
        testBackup(SQLSERVER, SQL_SERVER_DATA);
    }

}
