package com.atlassian.activeobjects.internal;

import net.java.ao.EntityManager;
import net.java.ao.EntityManagerConfiguration;
import net.java.ao.event.EventManagerImpl;

import javax.sql.DataSource;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class EntityManagerFactoryImpl implements EntityManagerFactory
{
    private final DatabaseProviderFactory databaseProviderFactory;

    public EntityManagerFactoryImpl(DatabaseProviderFactory databaseProviderFactory)
    {
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
    }

    public EntityManager getEntityManager(DataSource dataSource)
    {
        return new EntityManager(databaseProviderFactory.getDatabaseProvider(dataSource), new WeakCacheEntityManagerConfiguration(), new EventManagerImpl());
    }

    private static class WeakCacheEntityManagerConfiguration implements EntityManagerConfiguration
    {
        public boolean useWeakCache()
        {
            return true;
        }
    }
}
