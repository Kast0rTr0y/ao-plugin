package com.atlassian.activeobjects.jira;

import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;

import java.sql.Connection;

public final class OfBizDatabaseTypeExtractor implements JiraDatabaseTypeExtractor
{
    public DatabaseType getDatabaseType(Connection connection)
    {
        return DatabaseTypeFactory.getTypeForConnection(connection);
    }
}
