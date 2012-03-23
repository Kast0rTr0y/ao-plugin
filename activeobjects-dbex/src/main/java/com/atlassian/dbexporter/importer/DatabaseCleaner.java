package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.CleanupMode;

/**
 * Main interface for cleaning up the database before import. Cleaning up meaning running the necessary
 * updates on the DB (drops, updates, etc.) so that the import might be successful.
 */
public interface DatabaseCleaner
{
    void cleanup(CleanupMode cleanupMode);
}
