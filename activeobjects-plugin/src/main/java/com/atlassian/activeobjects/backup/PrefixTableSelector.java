package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.dbexporter.exporter.TableSelector;

import static com.google.common.base.Preconditions.*;

final class PrefixTableSelector implements TableSelector
{
    private final Prefix prefix;

    public PrefixTableSelector(Prefix prefix)
    {
        this.prefix = checkNotNull(prefix);
    }

    @Override
    public boolean accept(String tableName)
    {
        return prefix.isStarting(tableName, false);
    }
}
