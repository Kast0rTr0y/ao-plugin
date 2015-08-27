package com.atlassian.activeobjects.config;

import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import net.java.ao.RawEntity;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Set;

public interface ActiveObjectsConfigurationFactory {
    ActiveObjectsConfiguration getConfiguration(Bundle bundle, String namespace, Set<Class<? extends RawEntity<?>>> entities, List<ActiveObjectsUpgradeTask> upgradeTasks);
}
