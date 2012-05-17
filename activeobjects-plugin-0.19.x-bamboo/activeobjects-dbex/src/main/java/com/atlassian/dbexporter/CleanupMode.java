package com.atlassian.dbexporter;

/**
 * Determine how the database should be cleaned up before an import.
 */
public enum CleanupMode
{
    NONE, // no clean up necessary
    CLEAN, // clean up the database, drop tables etc. before the import
}
