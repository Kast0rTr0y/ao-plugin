package com.atlassian.dbexporter.jdbc;

import com.atlassian.dbexporter.ImportExportException;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

public class ImportExportSqlException extends ImportExportException
{
    private final SQLException sqlException;

    public ImportExportSqlException(SQLException sqlException)
    {
        super(sqlException);
        this.sqlException = checkNotNull(sqlException);
    }

    public ImportExportSqlException(String message, SQLException sqlException)
    {
        super(message, sqlException);
        this.sqlException = checkNotNull(sqlException);
    }

    public String getSQLState()
    {
        return sqlException.getSQLState();
    }

    public int getErrorCode()
    {
        return sqlException.getErrorCode();
    }

    public SQLException getNextException()
    {
        return sqlException.getNextException();
    }

    public SQLException getSqlException()
    {
        return sqlException;
    }
}
