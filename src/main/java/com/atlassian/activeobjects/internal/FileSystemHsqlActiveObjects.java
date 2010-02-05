package com.atlassian.activeobjects.internal;

import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

import java.io.File;

/**
 * Simply delegates back to the underlying {@link EntityManager}
 */
public class FileSystemHsqlActiveObjects extends EntityManagedActiveObjects implements DatabaseDirectoryAware
{
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "";

    public FileSystemHsqlActiveObjects(File dbDirectory)
    {
        super(new EntityManager(getDatabaseProvider(getUri(dbDirectory), USER_NAME, PASSWORD), true));
    }

    private static DatabaseProvider getDatabaseProvider(String uri, String userName, String password)
    {
        return DatabaseProvider.getInstance(uri, userName, password, true);
    }

    private static String getUri(File dbDirectory)
    {
        return "jdbc:hsqldb:file:" + dbDirectory.getAbsolutePath() + "/db;hsqldb.default_table_type=cached";
    }
}