package com.atlassian.activeobjects.internal;

import java.sql.SQLException;

public final class ActiveObjectsSqlException extends RuntimeException
{
    public ActiveObjectsSqlException(SQLException e)
    {
        super(e);
    }
}
