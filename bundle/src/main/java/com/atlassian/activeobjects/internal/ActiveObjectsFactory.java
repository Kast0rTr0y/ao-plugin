package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Factory to create instances of {@link com.atlassian.activeobjects.external.ActiveObjects}.
 */
public interface ActiveObjectsFactory
{
    /**
     * Creates a new instance of {@link com.atlassian.activeobjects.external.ActiveObjects} each time it is called.
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     */
    ActiveObjects create();
}
