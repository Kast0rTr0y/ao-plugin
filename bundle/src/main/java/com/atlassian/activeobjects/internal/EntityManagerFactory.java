package com.atlassian.activeobjects.internal;

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
     * @return a new entity manager
     */
    EntityManager getEntityManager(DataSource dataSource);
}
