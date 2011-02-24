package com.atlassian.dbexporter;

import static com.google.common.base.Preconditions.*;

public final class ForeignKey
{
    private final String name;
    private final String fromTable;
    private final String fromField;
    private final String toTable;
    private final String toField;

    public ForeignKey(String name, String fromTable, String fromField, String toTable, String toField)
    {
        this.name = checkNotNull(name);
        this.fromTable = checkNotNull(fromTable);
        this.fromField = checkNotNull(fromField);
        this.toTable = checkNotNull(toTable);
        this.toField = checkNotNull(toField);
    }

    public String getName()
    {
        return name;
    }

    public String getFromTable()
    {
        return fromTable;
    }

    public String getFromField()
    {
        return fromField;
    }

    public String getToTable()
    {
        return toTable;
    }

    public String getToField()
    {
        return toField;
    }
}
