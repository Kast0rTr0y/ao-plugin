package com.atlassian.activeobjects.internal;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>Represents a key used throughout the ActiveObjects plugin to store information
 * about configuration etc. against each plugin using an {@link com.atlassian.activeobjects.external.ActiveObjects}
 * service.</p>
 * In that respect the {@link #toString()} method is important here and should be changed lightly, as it can
 * be used for keys in the database for example.</p>
 * <p>So are {@link #equals(Object)} and {@link #hashCode()} as this class can be used and IS used as
 * key in Maps and other such collections.</p>
 */
public final class PluginKey
{
    private final String bundleSymbolicName;

    PluginKey(String bundleSymbolicName)
    {
        this.bundleSymbolicName = checkNotNull(bundleSymbolicName);
    }

    public static PluginKey fromBundle(Bundle bundle)
    {
        checkNotNull(bundle);
        return new PluginKey(bundle.getSymbolicName());
    }

    @Override
    public String toString()
    {
        return bundleSymbolicName; //  TODO: FIX ???
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 11).append(bundleSymbolicName).toHashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (o == this)
        {
            return true;
        }
        if (o.getClass() != getClass())
        {
            return false;
        }

        final PluginKey pluginKey = (PluginKey) o;
        return new EqualsBuilder().append(bundleSymbolicName, pluginKey.bundleSymbolicName).isEquals();
    }
}
