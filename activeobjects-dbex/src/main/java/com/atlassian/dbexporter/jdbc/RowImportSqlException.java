package com.atlassian.dbexporter.jdbc;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

public final class RowImportSqlException extends ImportExportSqlException
{
    private final String tableName;
    private final long rowNumber;

    public RowImportSqlException(SQLException sqlException, String tableName, long rowNumber)
    {
        super(sqlException);
        this.tableName = checkNotNull(tableName);
        this.rowNumber = rowNumber;
    }

    @Override
    public String getMessage()
    {
        return "There has been a SQL exception importing row #"
                + rowNumber +
                " for table '" + tableName +
                "' see  the cause of this exception for more detail about it.";
    }

    public String getTableName()
    {
        return tableName;
    }

    public long getRowNumber()
    {
        return rowNumber;
    }
}
