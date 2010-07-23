package com.atlassian.activeobjects.internal;

import org.osgi.framework.Bundle;

public final class PluginKeyFactoryImpl implements PluginKeyFactory
{
    public PluginKey get(Bundle bundle)
    {
        return new PluginKey(bundle.getSymbolicName());
    }
}
