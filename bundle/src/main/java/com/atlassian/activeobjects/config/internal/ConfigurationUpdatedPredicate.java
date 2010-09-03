package com.atlassian.activeobjects.config.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * A predicate to tell whether a configuration change has affected a particular instance of active object or not.
 */
public interface ConfigurationUpdatedPredicate
{
    boolean matches(ActiveObjects activeObjects, ActiveObjectsConfiguration configuration);
}
