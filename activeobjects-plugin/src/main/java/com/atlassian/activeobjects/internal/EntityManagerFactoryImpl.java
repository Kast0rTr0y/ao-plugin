package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.EntityManager;
import net.java.ao.EntityManagerConfiguration;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

import javax.sql.DataSource;

import static com.google.common.base.Preconditions.*;

public class EntityManagerFactoryImpl implements EntityManagerFactory
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
                        configuration.getTableNameConverter(),
                        configuration.getFieldNameConverter(),
                        configuration.getSchemaConfiguration());

        return new EntityManager(databaseProviderFactory.getDatabaseProvider(dataSource, databaseType, schema), entityManagerConfiguration);
    }

    private static class DataSourceEntityManagerConfiguration implements EntityManagerConfiguration
    {
        private final TableNameConverter tableNameConverter;
        private final FieldNameConverter fieldNameConverter;
        private final SchemaConfiguration schemaConfiguration;

        DataSourceEntityManagerConfiguration(TableNameConverter tableNameConverter, FieldNameConverter fieldNameConverter, SchemaConfiguration schemaConfiguration)
        {
            this.tableNameConverter = tableNameConverter;
            this.fieldNameConverter = fieldNameConverter;
            this.schemaConfiguration = schemaConfiguration;
        }

        public boolean useWeakCache()
        {
            return true;
        }

        public TableNameConverter getTableNameConverter()
        {
            return tableNameConverter;
        }

        public FieldNameConverter getFieldNameConverter()
        {
            return fieldNameConverter;
        }

        public SchemaConfiguration getSchemaConfiguration()
        {
            return schemaConfiguration;
        }
    }
}
