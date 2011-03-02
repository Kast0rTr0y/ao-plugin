package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.NoOpAroundImporter;
import com.atlassian.dbexporter.node.NodeParser;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Iterables.*;

/**
 * Updates the auto-increment sequences so that they start are the correct min value after some data has been 'manually'
 * imported into the database.
 */
public final class SequenceAroundImporter extends NoOpAroundImporter
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SequenceUpdater sequenceUpdater;

    public SequenceAroundImporter(SequenceUpdater sequenceUpdater)
    {
        this.sequenceUpdater = checkNotNull(sequenceUpdater);
    }

    @Override
    public void after(NodeParser node, ImportConfiguration configuration, Context context)
    {
        final DatabaseInformations.Database database = database(configuration.getDatabaseInformation());
        if (shouldRun(database))
        {
            doAfter(configuration, context);
        }
        else
        {
            logger.debug("Sequence importers not running for {}", database);
        }
    }

    private boolean shouldRun(DatabaseInformations.Database database)
    {
        return database.getType().equals(Database.Type.POSTGRES);
    }

    private void doAfter(ImportConfiguration configuration, Context context)
    {
        final EntityNameProcessor entityNameProcessor = configuration.getEntityNameProcessor();
        for (TableColumnPair tableColumnPair : concat(transform(context.getAll(Table.class), new AutoIncrementColumnIterableFunction())))
        {
            final String tableName = entityNameProcessor.tableName(tableColumnPair.table.getName());
            final String columnName = entityNameProcessor.columnName(tableColumnPair.column.getName());
            sequenceUpdater.update(tableName, columnName);
        }
    }

    private static class AutoIncrementColumnIterableFunction implements Function<Table, Iterable<TableColumnPair>>
    {
        @Override
        public Iterable<TableColumnPair> apply(final Table table)
        {
            return transform(filter(table.getColumns(), new IsAutoIncrementColumn()), new Function<Column, TableColumnPair>()
            {
                @Override
                public TableColumnPair apply(Column column)
                {
                    return new TableColumnPair(table, column);
                }
            });
        }
    }

    private static class IsAutoIncrementColumn implements Predicate<Column>
    {
        @Override
        public boolean apply(Column column)
        {
            return column.isAutoIncrement();
        }
    }

    private static class TableColumnPair
    {
        final Table table;
        final Column column;

        public TableColumnPair(Table table, Column column)
        {
            this.table = checkNotNull(table);
            this.column = checkNotNull(column);
        }
    }
}
