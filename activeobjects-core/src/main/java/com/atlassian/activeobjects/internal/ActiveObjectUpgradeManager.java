package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.google.common.base.Supplier;

import java.util.List;

public interface ActiveObjectUpgradeManager
{
    void upgrade(Prefix tableNamePrefix, List<ActiveObjectsUpgradeTask> upgradeTasks, Supplier<ActiveObjects> ao);
}
