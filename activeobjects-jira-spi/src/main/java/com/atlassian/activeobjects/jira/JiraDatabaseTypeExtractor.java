package com.atlassian.activeobjects.jira;

import com.atlassian.jira.configurator.config.DatabaseType;

/**
 * Extracts the type of database JIRA is currently configured to use.
 */
public interface JiraDatabaseTypeExtractor
{
    /**
     * Gets the current database type.
     * @return the type of the database currently in use, {@code null} if it could not be resolved.
     */
    DatabaseType getDatabaseType();
}
