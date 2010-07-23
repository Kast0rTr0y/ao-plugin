package com.atlassian.activeobjects.internal;

import net.java.ao.EntityManager;
import net.java.ao.builder.EntityManagerBuilder;

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
        super(EntityManagerBuilder.url(getUri(dbDirectory)).username(USER_NAME).password(PASSWORD).auto().useWeakCache().build());
    }

    private static String getUri(File dbDirectory)
    {
        return "jdbc:hsqldb:file:" + dbDirectory.getAbsolutePath() + "/db;hsqldb.default_table_type=cached";
    }
}