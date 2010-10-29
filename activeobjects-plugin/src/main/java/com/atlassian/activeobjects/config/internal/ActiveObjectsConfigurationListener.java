package com.atlassian.activeobjects.config.internal;

public interface ActiveObjectsConfigurationListener
{
    void onConfigurationUpdated(ConfigurationUpdatedPredicate predicate);
}
