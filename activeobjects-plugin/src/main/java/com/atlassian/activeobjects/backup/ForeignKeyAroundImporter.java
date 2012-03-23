package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.NoOpAroundImporter;
import com.atlassian.dbexporter.node.NodeParser;
import com.google.common.base.Function;

import java.util.Collection;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.*;

public final class ForeignKeyAroundImporter extends NoOpAroundImporter
{
    private final ForeignKeyCreator foreignKeyCreator;

    public ForeignKeyAroundImporter(ForeignKeyCreator foreignKeyCreator)
    {
        this.foreignKeyCreator = checkNotNull(foreignKeyCreator);
    }

    @Override
    public void after(NodeParser node, ImportConfiguration configuration, Context context)
    {
        foreignKeyCreator.create(concat(transform(context.getAll(Table.class), getForeignKeysFunction())), configuration.getEntityNameProcessor());
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
