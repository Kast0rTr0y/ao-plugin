package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import com.google.common.base.Supplier;
import net.java.ao.RawEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} that implements the
 * basic contract for a single {@link com.atlassian.activeobjects.internal.DataSourceType}.
 */
abstract class AbstractActiveObjectsFactory implements ActiveObjectsFactory {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LOCK_TIMEOUT_PROPERTY = "ao-plugin.upgrade.task.lock.timeout";
    private static final String LOCK_PREFIX = "ao-plugin.upgrade.";
    protected static final int LOCK_TIMEOUT_SECONDS = Integer.getInteger(LOCK_TIMEOUT_PROPERTY, 300000);

    private final DataSourceType supportedDataSourceType;
    private final ActiveObjectUpgradeManager aoUpgradeManager;
    protected final TransactionTemplate transactionTemplate;
    private final ClusterLockService clusterLockService;

    AbstractActiveObjectsFactory(DataSourceType dataSourceType, ActiveObjectUpgradeManager aoUpgradeManager,
                                 TransactionTemplate transactionTemplate, ClusterLockService clusterLockService) {
        this.supportedDataSourceType = checkNotNull(dataSourceType);
        this.aoUpgradeManager = checkNotNull(aoUpgradeManager);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.clusterLockService = checkNotNull(clusterLockService);
    }

    @Override
    public final boolean accept(ActiveObjectsConfiguration configuration) {
        return supportedDataSourceType.equals(configuration.getDataSourceType());
    }

    @Override
    public final ActiveObjects create(final ActiveObjectsConfiguration configuration, final Tenant tenant) {
        if (!accept(configuration)) {
            throw new IllegalStateException(configuration + " is not supported. Did you can #accept(ActiveObjectConfiguration) before calling me?");
        }

        final String lockName = LOCK_PREFIX + configuration.getPluginKey().asString();
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new ActiveObjectsInitException("unable to acquire cluster lock named '" + lockName + "' after waiting " + LOCK_TIMEOUT_SECONDS + " seconds; note that this timeout may be adjusted via the system property '" + LOCK_TIMEOUT_PROPERTY + "'");
            }
        } catch (InterruptedException e) {
            throw new ActiveObjectsInitException("interrupted while trying to acquire cluster lock named '" + lockName + "'", e);
        }

        try {
            upgrade(configuration, tenant);

            final ActiveObjects ao = doCreate(configuration, tenant);
            final Set<Class<? extends RawEntity<?>>> entitiesToMigrate = configuration.getEntities();

            return transactionTemplate.execute(new TransactionCallback<ActiveObjects>() {
                @Override
                public ActiveObjects doInTransaction() {
                    logger.debug("Created active objects instance with configuration {}, now migrating entities {}",
                            configuration, entitiesToMigrate);
                    ao.migrate(asArray(entitiesToMigrate));
                    return ao;
                }
            });
        } finally {
            lock.unlock();
        }
    }

    private void upgrade(final ActiveObjectsConfiguration configuration, final Tenant tenant) {
        aoUpgradeManager.upgrade(configuration.getTableNamePrefix(), configuration.getUpgradeTasks(), new Supplier<ActiveObjects>() {
            @Override
            public ActiveObjects get() {
                return doCreate(configuration, tenant);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Class<? extends RawEntity<?>>[] asArray(Collection<Class<? extends RawEntity<?>>> classes) {
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * This has the same contract as {@link #create(ActiveObjectsConfiguration, com.atlassian.tenancy.api.Tenant)}
     * except that checking the configuration type has already been taken care of.
     *
     * @param configuration the configuration to work with
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     */
    protected abstract ActiveObjects doCreate(ActiveObjectsConfiguration configuration, Tenant tenant);
}
