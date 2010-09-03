package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.schema.TableNameConverter;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

public final class DelegatingPrefixedTableNameConverterFactory implements PrefixedTableNameConverterFactory
{
    private final TableNameConverter delegate;

    public DelegatingPrefixedTableNameConverterFactory(TableNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    public TableNameConverter getTableNameConverter(Prefix prefix)
    {
        return new PrefixedTableNameConverter(checkNotNull(prefix), delegate);
    }
}
