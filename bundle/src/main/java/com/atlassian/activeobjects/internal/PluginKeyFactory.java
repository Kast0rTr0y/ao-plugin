package com.atlassian.activeobjects.internal;

import org.osgi.framework.Bundle;

/**
 * Factory to create consitent plugin keys for bundles that use Active Objects.
 */
public interface PluginKeyFactory
{
    PluginKey get(Bundle bundle);
}
