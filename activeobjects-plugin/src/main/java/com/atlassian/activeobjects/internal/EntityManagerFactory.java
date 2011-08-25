package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.EntityManager;

import javax.sql.DataSource;

/**
 * A factory to create new EntityManagers from a given data source.
 */
interface EntityManagerFactory
{
    /**
     * Creates a <em>new</em> entity manager using the given data source.
     *
     * @param dataSource the data source for which to create the entity manager
     * @param databaseType the type of database that the data source connects to.
     * @param configuration the configuration for this active objects instance
     * @return a new entity manager
     */
    EntityManager getEntityManager(DataSource dataSource, DatabaseType databaseType, String schema, ActiveObjectsConfiguration configuration);
}
