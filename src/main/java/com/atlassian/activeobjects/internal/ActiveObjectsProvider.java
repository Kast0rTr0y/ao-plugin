package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Provides a pooled instance of the {@link com.atlassian.activeobjects.external.ActiveObjects} database accessor.  Multiple calls for the same identifier
 * will return the same {@link com.atlassian.activeobjects.external.ActiveObjects} instance, as long as there is at least one strongly-held reference.
 *
 * It is recommended clients use the {@link com.atlassian.activeobjects.external.ActiveObjects} service directly as it will choose a consistent key
 * automatically.
 */
public interface ActiveObjectsProvider
{
    ActiveObjects createActiveObjects(String pluginKey);
}
