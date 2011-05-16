package com.atlassian.dbexporter;

import static com.google.common.base.Preconditions.*;

public final class Column
{


    private final String name;
    private final int sqlType;
    private final Boolean primaryKey;
    private final Boolean autoIncrement;
    private final Integer precision;
    private final Integer scale;

    public Column(String name, int sqlType, Boolean pk, Boolean autoIncrement, Integer precision, Integer scale)
    {
        this.name = checkNotNull(name);
        this.sqlType = sqlType;
        this.primaryKey = pk;
        this.autoIncrement = autoIncrement;
        this.precision = precision;
        this.scale = scale;
    }

    public String getName()
    {
        return name;
    }

    public Boolean isPrimaryKey()
    {
        return primaryKey;
    }

    public Boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    public int getSqlType()
    {
        return sqlType;
    }

    public Integer getPrecision()
    {
        return precision;
    }

    public Integer getScale()
    {
        return scale;
    }
}
