package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import net.java.ao.DatabaseProvider;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Sets.*;

public final class ActiveObjectsHashesReader
{
    private final static Prefix AO_PREFIX = new SimplePrefix(ActiveObjectModuleDescriptor.AO_TABLE_PREFIX);

    private final ActiveObjectsTableNamesReader tableNamesReader;

    public ActiveObjectsHashesReader(ActiveObjectsTableNamesReader tableNamesReader)
    {
        this.tableNamesReader = checkNotNull(tableNamesReader);
    }

    public Iterable<String> getHashes(DatabaseProvider databaseProvider, PrefixedSchemaConfigurationFactory schemaConfigurationFactory)
    {
        final Iterable<String> tableNames = tableNamesReader.getTableNames(databaseProvider, schemaConfigurationFactory.getSchemaConfiguration(AO_PREFIX));
        return newHashSet(extractHashes(removeAoPrefixes(filterNonAoTables(tableNames))));
    }

    private Iterable<String> extractHashes(Iterable<String> tableNamesWithNoAoPrefix)
    {
        return Iterables.transform(tableNamesWithNoAoPrefix, new Function<String, String>()
        {
            @Override
            public String apply(String tableName)
            {
                return tableName.substring(0, tableName.indexOf('_'));
            }
        });
    }

    private Iterable<String> filterNonAoTables(Iterable<String> names)
    {
        return Iterables.filter(names, new Predicate<String>()
        {
            @Override
            public boolean apply(String input)
            {
                return AO_PREFIX.isStarting(input, false);
            }
        });
    }

    private Iterable<String> removeAoPrefixes(Iterable<String> tableNames)
    {
        return Iterables.transform(tableNames, new Function<String, String>()
        {
            @Override
            public String apply(String tableName)
            {
                return tableName.substring(tableName.indexOf('_') + 1, tableName.length());
            }
        });
    }
}
