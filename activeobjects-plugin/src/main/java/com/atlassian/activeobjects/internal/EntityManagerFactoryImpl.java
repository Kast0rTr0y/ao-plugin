package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.EntityManager;
import net.java.ao.EntityManagerConfiguration;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;

import javax.sql.DataSource;

import static com.google.common.base.Preconditions.*;

public final class EntityManagerFactoryImpl implements EntityManagerFactory
{
    private final DatabaseProviderFactory databaseProviderFactory;

    public EntityManagerFactoryImpl(DatabaseProviderFactory databaseProviderFactory)
    {
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
    }

    public EntityManager getEntityManager(DataSource dataSource, DatabaseType databaseType, String schema, ActiveObjectsConfiguration configuration)
    {
        final DataSourceEntityManagerConfiguration entityManagerConfiguration =
                new DataSourceEntityManagerConfiguration(
                        configuration.getNameConverters(),
                        configuration.getSchemaConfiguration());

        return new EntityManager(databaseProviderFactory.getDatabaseProvider(dataSource, databaseType, schema), entityManagerConfiguration);
    }

    private static class DataSourceEntityManagerConfiguration implements EntityManagerConfiguration
    {
        private final NameConverters nameConverters;
        private final SchemaConfiguration schemaConfiguration;

        DataSourceEntityManagerConfiguration(NameConverters nameConverters, SchemaConfiguration schemaConfiguration)
        {
            this.nameConverters = nameConverters;
            this.schemaConfiguration = schemaConfiguration;
        }

        @Override
        public boolean useWeakCache()
        {
            return true;
        }

        @Override
        public NameConverters getNameConverters()
        {
            return nameConverters;
        }

        @Override
        public SchemaConfiguration getSchemaConfiguration()
        {
            return schemaConfiguration;
        }
    }
}
