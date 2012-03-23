package com.atlassian.activeobjects.admin.tables;

import net.java.ao.DatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;
import static net.java.ao.sql.SqlUtils.*;

public final class RowCounter
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DatabaseProvider provider;

    private RowCounter(DatabaseProvider provider)
    {
        this.provider = checkNotNull(provider);
    }

    int count(String tableName)
    {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = provider.getConnection();
            stmt = provider.preparedStatement(connection, "SELECT COUNT(*) FROM " + provider.withSchema(tableName));
            res = stmt.executeQuery();

            checkState(res.next());
            return res.getInt(1);
        }
        catch (SQLException e)
        {
            logger.warn("Could not count number of rows for table '{}'", tableName);
            logger.warn("Here is the exception:", e);
            return -1;
        }
        finally
        {
            closeQuietly(res);
            closeQuietly(stmt);
            closeQuietly(connection);
        }
    }

    static RowCounter from(DatabaseProvider provider)
    {
        return new RowCounter(provider);
    }
}
