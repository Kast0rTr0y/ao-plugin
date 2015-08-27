package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.HsqlTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(HsqlTest.class)
public class TestHsqlActiveObjectBackup extends ActiveObjectsBackupDataSetup {

    @Test
    @NonTransactional
    public void testHsqlBackup() throws Exception {
        testBackup(HSQL, HSQL_DATA);
    }

    @Test
    @NonTransactional
    public void testHsqlEmptyBackup() throws Exception {
        String xmlBackup = read(HSQL_EMPTY);
        checkXmlBackup(xmlBackup, HSQL_DATA);
        restore(xmlBackup);
    }
}
