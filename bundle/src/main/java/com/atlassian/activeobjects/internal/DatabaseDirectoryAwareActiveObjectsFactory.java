package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class DatabaseDirectoryAwareActiveObjectsFactory implements ActiveObjectsFactory
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String pluginKey;
    private final ApplicationProperties applicationProperties;
    private final DatabaseConfiguration configuration;

    public DatabaseDirectoryAwareActiveObjectsFactory(String pluginKey, ApplicationProperties applicationProperties, DatabaseConfiguration configuration)
    {
        this.pluginKey = checkNotNull(pluginKey);
        this.applicationProperties = checkNotNull(applicationProperties);
        this.configuration = checkNotNull(configuration);
    }

    public ActiveObjects create()
    {
        final File dbDir = getDatabaseDirectory(getDatabasesDirectory(getHomeDirectory()));
        return new FileSystemHsqlActiveObjects(dbDir, pluginKey);
    }

    private File getDatabaseDirectory(File databasesDirectory)
    {
        final File dbDir = new File(databasesDirectory, pluginKey);
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
}
