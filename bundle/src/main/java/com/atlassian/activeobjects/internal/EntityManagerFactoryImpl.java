package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfigurationFactory;
import com.atlassian.activeobjects.ao.PrefixedTableNameConverterFactory;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.EntityManager;
import net.java.ao.EntityManagerConfiguration;
import net.java.ao.SchemaConfiguration;
import net.java.ao.event.EventManagerImpl;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

import javax.sql.DataSource;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

public class EntityManagerFactoryImpl implements EntityManagerFactory
{
    private final DatabaseProviderFactory databaseProviderFactory;
    private final PrefixedTableNameConverterFactory tableNameConverterFactory;
    private final FieldNameConverter fieldNameConverter;
    private final PrefixedSchemaConfigurationFactory schemaConfigurationFactory;

    public EntityManagerFactoryImpl(DatabaseProviderFactory databaseProviderFactory, PrefixedTableNameConverterFactory tableNameConverterFactory, FieldNameConverter fieldNameConverter, PrefixedSchemaConfigurationFactory schemaConfigurationFactory)
    {
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.tableNameConverterFactory = checkNotNull(tableNameConverterFactory);
        this.fieldNameConverter = checkNotNull(fieldNameConverter);
        this.schemaConfigurationFactory = checkNotNull(schemaConfigurationFactory);
    }

    public EntityManager getEntityManager(DataSource dataSource, DatabaseType databaseType, ActiveObjectsConfiguration configuration)
    {
        final Prefix prefix = configuration.getTableNamePrefix();
        final TableNameConverter tableNameConverter = tableNameConverterFactory.getTableNameConverter(prefix);
        final SchemaConfiguration schemaConfiguration = schemaConfigurationFactory.getSchemaConfiguration(prefix);
        final DataSourceEntityManagerConfiguration entityManagerConfiguration = new DataSourceEntityManagerConfiguration(tableNameConverter, fieldNameConverter, schemaConfiguration);

        return new EntityManager(databaseProviderFactory.getDatabaseProvider(dataSource, databaseType), entityManagerConfiguration, new EventManagerImpl());
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
