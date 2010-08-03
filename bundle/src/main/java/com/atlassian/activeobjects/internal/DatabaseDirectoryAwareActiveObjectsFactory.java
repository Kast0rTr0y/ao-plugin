package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.ApplicationProperties;
import net.java.ao.EntityManager;
import net.java.ao.builder.EntityManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public final class DatabaseDirectoryAwareActiveObjectsFactory implements ActiveObjectsFactory
{
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ApplicationProperties applicationProperties;
    private final DatabaseConfiguration configuration;

    public DatabaseDirectoryAwareActiveObjectsFactory(ApplicationProperties applicationProperties, DatabaseConfiguration configuration)
    {
        this.applicationProperties = checkNotNull(applicationProperties);
        this.configuration = checkNotNull(configuration);
    }

    public ActiveObjects create(PluginKey pluginKey)
    {
        final File dbDir = getDatabaseDirectory(getDatabasesDirectory(getHomeDirectory()), pluginKey);
        final EntityManager entityManager = getEntityManager(dbDir);
        return new DatabaseDirectoryAwareEntityManagedActiveObjects(entityManager, new EntityManagedTransactionManager(entityManager));
    }

    private EntityManager getEntityManager(File dbDirectory)
    {
        return EntityManagerBuilder.url(getUri(dbDirectory)).username(USER_NAME).password(PASSWORD).auto().useWeakCache().build();
    }

    private static String getUri(File dbDirectory)
    {
        return "jdbc:hsqldb:file:" + dbDirectory.getAbsolutePath() + "/db;hsqldb.default_table_type=cached";
    }

    private File getDatabaseDirectory(File databasesDirectory, PluginKey pluginKey)
    {
        final File dbDir = new File(databasesDirectory, pluginKey.toString());
        if (!dbDir.exists() && !dbDir.mkdir())
        {
            throw new ActiveObjectsPluginException("Could not create database directory for plugin <" + pluginKey + "> at  <" + dbDir.getAbsolutePath() + ">");
        }

        log.debug("Database directory {} initialised", dbDir);

        return dbDir;
    }

    private File getDatabasesDirectory(File home)
    {
        String path = configuration.getBaseDirectory();
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        final File dbDirectory = new File(home, path);

        if (dbDirectory.exists() && dbDirectory.isFile())
        {
            throw new ActiveObjectsPluginException("Database directory already exists, but is a file, at <" + dbDirectory.getPath() + ">");
        }

        if (!dbDirectory.exists() && !dbDirectory.mkdirs())
        {
            throw new ActiveObjectsPluginException("Could not create directory for database at <" + dbDirectory.getPath() + ">");
        }

        log.debug("ActiveObjects databases directory {} initialized", dbDirectory.getAbsolutePath());

        return dbDirectory;
    }

    private File getHomeDirectory()
    {
        final File home = applicationProperties.getHomeDirectory();
        if (home == null)
        {
            throw new ActiveObjectsPluginException("Home directory undefined!");
        }
        if (!home.exists() || !home.isDirectory())
        {
            throw new ActiveObjectsPluginException("The ActiveObjects plugin couldn't find a home directory at <" + home.getAbsolutePath() + ">");
        }
        return home;
    }

    private static final class DatabaseDirectoryAwareEntityManagedActiveObjects extends EntityManagedActiveObjects implements DatabaseDirectoryAware
    {
        DatabaseDirectoryAwareEntityManagedActiveObjects(EntityManager entityManager, TransactionManager transactionManager)
        {
            super(entityManager, transactionManager);
        }
    }
}
