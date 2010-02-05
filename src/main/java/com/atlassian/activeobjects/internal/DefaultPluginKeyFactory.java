package com.atlassian.activeobjects.internal;

import org.osgi.framework.Bundle;

public class DefaultPluginKeyFactory implements PluginKeyFactory
{
    public String get(Bundle bundle)
    {
        String prefix = bundle.getSymbolicName().substring(0, Math.min(4, bundle.getSymbolicName().length()));
        prefix += bundle.getSymbolicName().hashCode();
        return prefix;
    }
}
