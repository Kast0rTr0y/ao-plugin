package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.MySqlTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(MySqlTest.class)
public class TestMySqlActiveObjectBackup extends ActiveObjectsBackupDataSetup {

    @Test
    @NonTransactional
    public void testMySqlBackup() throws Exception {
        testBackup(MYSQL, MYSQL_DATA);
    }
}
