package com.atlassian.dbexporter;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;
import static java.util.Collections.*;

public final class Table
{
    private final String name;
    private final List<Column> columns;
    private final Collection<ForeignKey> foreignKeys;

    public Table(String name, List<Column> columns, Collection<ForeignKey> foreignKeys)
    {
        this.name = checkNotNull(name);
        this.columns = newLinkedList(checkNotNull(columns));
        this.foreignKeys = newLinkedList(checkNotNull(foreignKeys));
    }

    public String getName()
    {
        return name;
    }

    public List<Column> getColumns()
    {
        return unmodifiableList(columns);
    }

    public Collection<ForeignKey> getForeignKeys()
    {
        return unmodifiableCollection(foreignKeys);
    }
}
