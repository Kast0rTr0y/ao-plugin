package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.AroundImporter;
import com.atlassian.dbexporter.importer.ForeignKeyCreator;
import com.atlassian.dbexporter.xml.NodeParser;
import com.google.common.base.Function;

import java.util.Collection;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.*;

public final class ForeignKeyAroundImporter implements AroundImporter
{
    private final ForeignKeyCreator foreignKeyCreator;

    public ForeignKeyAroundImporter(ForeignKeyCreator foreignKeyCreator)
    {
        this.foreignKeyCreator = checkNotNull(foreignKeyCreator);
    }

    public void before(NodeParser node, Context context)
    {
        // nothing
    }

    public void after(NodeParser node, Context context)
    {
        foreignKeyCreator.create(concat(transform(context.getAll(Table.class), getForeignKeysFunction())), context);
    }

    private Function<Table, Collection<ForeignKey>> getForeignKeysFunction()
    {
        return new Function<Table, Collection<ForeignKey>>()
        {
            public Collection<ForeignKey> apply(Table from)
            {
                return from.getForeignKeys();
            }
        };
    }
}
