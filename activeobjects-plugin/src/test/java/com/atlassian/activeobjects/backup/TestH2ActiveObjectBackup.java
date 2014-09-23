package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.junit.H2Test;
import com.atlassian.activeobjects.junit.HsqlTest;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(H2Test.class)
public class TestH2ActiveObjectBackup extends ActiveObjectsBackupDataSetup
{

    @Test
    @NonTransactional
    public void testH2Backup() throws Exception
    {
        testBackup(H2, H2_DATA);
    }
}
