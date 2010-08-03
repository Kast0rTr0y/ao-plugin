package com.atlassian.activeobjects.internal;

import net.java.ao.EntityManager;
import net.java.ao.EntityManagerConfiguration;
import net.java.ao.SchemaConfiguration;
import net.java.ao.event.EventManagerImpl;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

import javax.sql.DataSource;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class EntityManagerFactoryImpl implements EntityManagerFactory
{
    private final DatabaseProviderFactory databaseProviderFactory;
    private final TableNameConverter tableNameConverter;
    private final FieldNameConverter fieldNameConverter;
    private final SchemaConfiguration schemaConfiguration;

    public EntityManagerFactoryImpl(DatabaseProviderFactory databaseProviderFactory, TableNameConverter tableNameConverter, FieldNameConverter fieldNameConverter, SchemaConfiguration schemaConfiguration)
    {
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.tableNameConverter = checkNotNull(tableNameConverter);
        this.fieldNameConverter = checkNotNull(fieldNameConverter);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
    }

    public EntityManager getEntityManager(DataSource dataSource)
    {
        return new EntityManager(databaseProviderFactory.getDatabaseProvider(dataSource), new DataSourceEntityManagerConfiguration(), new EventManagerImpl());
    }

    private class DataSourceEntityManagerConfiguration implements EntityManagerConfiguration
    {
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
