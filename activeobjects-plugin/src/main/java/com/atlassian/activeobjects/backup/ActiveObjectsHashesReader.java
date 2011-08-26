package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;

public final class ActiveObjectsHashesReader
{
    private final static Prefix AO_PREFIX = new SimplePrefix(ActiveObjectModuleDescriptor.AO_TABLE_PREFIX);

    public String getHash(String tableName)
    {
        if (tableName == null)
        {
            return null;
        }

        if (!AO_PREFIX.isStarting(tableName, false))
        {
            throw new IllegalStateException("Table " + tableName + " is not an ActiveObjects table!");
        }

        return extractHash(removeAoPrefix(tableName));
    }

    private String removeAoPrefix(String tableName)
    {
        return tableName.substring(tableName.indexOf('_') + 1, tableName.length());
    }

    private String extractHash(String tableNameWithNoAoPrefix)
    {
        return tableNameWithNoAoPrefix.substring(0, tableNameWithNoAoPrefix.indexOf('_'));
    }
}