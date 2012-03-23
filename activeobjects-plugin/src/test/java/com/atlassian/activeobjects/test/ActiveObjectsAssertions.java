package com.atlassian.activeobjects.test;

import java.io.File;
import java.io.FilenameFilter;

import static junit.framework.Assert.*;

public final class ActiveObjectsAssertions
{
    private ActiveObjectsAssertions()
    {
    }

    public static void assertDatabaseExists(File baseDir, final String dbPrefix)
    {
        assertDatabaseExists(baseDir, dbPrefix, 1);
    }

    public static void assertDatabaseExists(File baseDir, String path, final String dbPrefix)
    {
        assertDatabaseExists(new File(baseDir, path), dbPrefix, 1);
    }

    static void assertDatabaseDoesNotExists(File baseDir, String path, final String dbPrefix)
    {
        assertDatabaseExists(new File(baseDir, path), dbPrefix, 0);
    }

    private static void assertDatabaseExists(File dbDir, final String dbPrefix, int expected)
    {
        File[] files = dbDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(dbPrefix);
            }
        });

        if (files == null)
        {
            files = new File[0];
        }

        assertEquals("Didn't find the expected # of file in directory '" + dbDir.getAbsolutePath() + "'", expected, files.length);
    }
}
