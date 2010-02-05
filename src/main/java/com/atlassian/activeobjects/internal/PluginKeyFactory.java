package com.atlassian.activeobjects.internal;

import org.osgi.framework.Bundle;

public interface PluginKeyFactory
{
    String get(Bundle bundle);
}
