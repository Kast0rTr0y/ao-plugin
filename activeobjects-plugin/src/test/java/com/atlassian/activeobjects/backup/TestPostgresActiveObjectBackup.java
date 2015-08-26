package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.PostgresTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(PostgresTest.class)
public class TestPostgresActiveObjectBackup extends ActiveObjectsBackupDataSetup {
    @Test
    @NonTransactional
    public void testPostgresBackup() throws Exception {
        testBackup(POSTGRES, POSTGRES_DATA);
    }
}
