package com.atlassian.activeobjects.spi;

/**
 * Represents the type of database.
 */
public enum DatabaseType {
    H2,
    HSQL,
    MYSQL,
    POSTGRESQL,
    ORACLE,
    MS_SQL,
    DB2,
    DERBY_EMBEDDED,
    DERBY_NETWORK,
    NUODB,
    UNKNOWN
}