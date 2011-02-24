package com.atlassian.dbexporter.jdbc;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

public final class SqlRuntimeException extends RuntimeException
{
    private final SQLException sqlException;

    public SqlRuntimeException(SQLException sqlException)
    {
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
