package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.google.common.base.Supplier;

import java.util.List;

public interface ActiveObjectUpgradeManager
{
    /**
     * <p>Manages the upgrades for a given plugin.</p>
     * <p>Upgrade tasks are treated atomically. I.e the current version of the model is updated after each upgrade task
     * is completed. If an upgrade task fails (throws an exception) then sub-sequent upgrade tasks won't be run, and
     * when this method is called the next time it will resume from the 'same point' where it failed.</p>
     *
     * @param tableNamePrefix the prefix for table names
     * @param upgradeTasks the list of upgrade tasks to apply
     * @param ao an {@link ActiveObjects} supplier.
     */
    void upgrade(Prefix tableNamePrefix, List<ActiveObjectsUpgradeTask> upgradeTasks, Supplier<ActiveObjects> ao);
}
