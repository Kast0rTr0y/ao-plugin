package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.ao.PrefixedSchemaConfigurationFactory;
import com.atlassian.activeobjects.ao.PrefixedTableNameConverterFactory;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.sal.api.ApplicationProperties;
import net.java.ao.EntityManager;
import net.java.ao.builder.EntityManagerBuilder;
import net.java.ao.schema.FieldNameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

public final class DatabaseDirectoryAwareActiveObjectsFactory extends AbstractActiveObjectsFactory
{
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ApplicationProperties applicationProperties;
    private final DatabaseConfiguration dbConfiguration;
    private final PrefixedTableNameConverterFactory tableNameConverterFactory;
    private final FieldNameConverter fieldNameConverter;
    private final PrefixedSchemaConfigurationFactory schemaConfigurationFactory;

    public DatabaseDirectoryAwareActiveObjectsFactory(ApplicationProperties applicationProperties, DatabaseConfiguration dbConfiguration, PrefixedTableNameConverterFactory tableNameConverterFactory, FieldNameConverter fieldNameConverter, PrefixedSchemaConfigurationFactory schemaConfigurationFactory)
    {
        super(DataSourceType.HSQLDB);
        this.applicationProperties = checkNotNull(applicationProperties);
        this.dbConfiguration = checkNotNull(dbConfiguration);
        this.tableNameConverterFactory = checkNotNull(tableNameConverterFactory);
        this.fieldNameConverter = checkNotNull(fieldNameConverter);
        this.schemaConfigurationFactory = checkNotNull(schemaConfigurationFactory);
    }

    @Override
    protected ActiveObjects doCreate(ActiveObjectsConfiguration configuration)
    {
        final File dbDir = getDatabaseDirectory(getDatabasesDirectory(getHomeDirectory()), configuration.getPluginKey());
        final EntityManager entityManager = getEntityManager(dbDir, configuration);
        return new DatabaseDirectoryAwareEntityManagedActiveObjects(entityManager, new EntityManagedTransactionManager(entityManager));
    }

    private EntityManager getEntityManager(File dbDirectory, ActiveObjectsConfiguration configuration)
    {
        final Prefix prefix = configuration.getTableNamePrefix();
        return EntityManagerBuilder.url(getUri(dbDirectory)).username(USER_NAME).password(PASSWORD).auto()
                .useWeakCache()
                .tableNameConverter(tableNameConverterFactory.getTableNameConverter(prefix))
                .fieldNameConverter(fieldNameConverter)
                .schemaConfiguration(schemaConfigurationFactory.getSchemaConfiguration(prefix))
                .build();
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
        String path = dbConfiguration.getBaseDirectory();
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
