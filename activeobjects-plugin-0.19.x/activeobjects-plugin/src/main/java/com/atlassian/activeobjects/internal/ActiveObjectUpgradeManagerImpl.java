package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class ActiveObjectUpgradeManagerImpl implements ActiveObjectUpgradeManager
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ModelVersionManager versionManager;

    public ActiveObjectUpgradeManagerImpl(ModelVersionManager versionManager)
    {
        this.versionManager = checkNotNull(versionManager);
    }

    @Override
    public void upgrade(Prefix tableNamePrefix, List<ActiveObjectsUpgradeTask> upgradeTasks, final Supplier<ActiveObjects> ao)
    {
        ModelVersion currentModelVersion = versionManager.getCurrent(tableNamePrefix);

        logger.info("Starting upgrade of data model, current version is {}", currentModelVersion);

        for (final ActiveObjectsUpgradeTask task : sortAndVerify(upgradeTasks))
        {
            if (currentModelVersion.isOlderThan(task.getModelVersion()))
            {
                currentModelVersion = upgrade(tableNamePrefix, task, ao.get(), currentModelVersion);
            }
        }
        logger.info("Finished upgrading, model is up to date at version {}", currentModelVersion);
    }

    private List<ActiveObjectsUpgradeTask> sortAndVerify(List<ActiveObjectsUpgradeTask> upgradeTasks)
    {
        return verify(sort(upgradeTasks));
    }

    List<ActiveObjectsUpgradeTask> verify(List<ActiveObjectsUpgradeTask> sorted)
    {
        ModelVersion mv = null;
        for (ActiveObjectsUpgradeTask task : sorted)
        {
            if (mv != null && mv.isSame(task.getModelVersion()))
            {
                throw new IllegalStateException("There are more than one upgrade tasks with model version " + mv);
            }
            mv = task.getModelVersion();
        }
        return sorted;
    }

    private List<ActiveObjectsUpgradeTask> sort(List<ActiveObjectsUpgradeTask> upgradeTasks)
    {
        final List<ActiveObjectsUpgradeTask> tasks = newArrayList(upgradeTasks);
        Collections.sort(tasks, new ActiveObjectsUpgradeTaskComparator());
        return ImmutableList.copyOf(tasks);
    }

    private ModelVersion upgrade(final Prefix tableNamePrefix, final ActiveObjectsUpgradeTask task, final ActiveObjects activeObjects, final ModelVersion currentModelVersion)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<ModelVersion>()
        {
            @Override
            public ModelVersion doInTransaction()
            {
                logger.debug("Upgrading data model with task {}, current version of model is {}", task.getClass().getName(), currentModelVersion);
                task.upgrade(currentModelVersion, activeObjects);

                final ModelVersion updatedVersion = task.getModelVersion();
                versionManager.update(tableNamePrefix, updatedVersion);
                logger.debug("Upgraded data model to version {}", updatedVersion);
                return updatedVersion;
            }
        });
    }

    private static class ActiveObjectsUpgradeTaskComparator implements Comparator<ActiveObjectsUpgradeTask>
    {
        @Override
        public int compare(ActiveObjectsUpgradeTask o1, ActiveObjectsUpgradeTask o2)
        {
            return o1.getModelVersion().compareTo(o2.getModelVersion());
        }
    }
}
