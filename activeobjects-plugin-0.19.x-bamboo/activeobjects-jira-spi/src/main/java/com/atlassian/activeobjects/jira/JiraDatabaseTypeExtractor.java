package com.atlassian.activeobjects.jira;

import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;

import java.sql.Connection;

public interface JiraDatabaseTypeExtractor
{
    public DatabaseType getDatabaseType(Connection connection);
}
