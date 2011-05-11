package com.atlassian.dbexporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

public final class Column
{
    private static final int MAX_MYSQL_SCALE = 30;

    private static final Logger logger = LoggerFactory.getLogger(Column.class);

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
        this.scale = checkScale(scale, precision);
    }

    private static Integer checkScale(Integer scale, Integer precision)
    {
        if (scale == null)
        {
            return null;
        }

        if (precision != null && scale > precision)
        {
            logger.warn("Scale is greater than precision (" + scale + " > " + precision + "), which is not allowed in most databases, setting scale with same value as precision");
            scale = precision;
        }

        if (scale > MAX_MYSQL_SCALE)
        {
            logger.warn("Scale is set to a value greater than 30 (" + scale + "), which is not compatible with MySQL 5, setting actual value to 30.");
            return MAX_MYSQL_SCALE;
        }

        return scale;
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
