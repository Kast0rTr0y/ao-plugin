package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DbImportException;
import com.atlassian.dbexporter.progress.Update;

import static com.atlassian.dbexporter.ContextUtils.getProgressMonitor;

public final class SchemaVersionDatabaseInformationChecker implements DatabaseInformationChecker
{
    private static final String SCHEMA_VERSION = "schema-version";

    private final int schemaVersion;

    public SchemaVersionDatabaseInformationChecker(int schemaVersion)
    {
        this.schemaVersion = schemaVersion;
    }

    public void check(DatabaseInformation information, Context context)
    {
        getProgressMonitor(context).update(new Update("Checking schema version"));

        final int readSchemaVersion = information.getInt(SCHEMA_VERSION, -1);
        if (readSchemaVersion > schemaVersion)
        {
            throw new DbImportException(String.format("ERROR: This backup archive is from a " +
                    "newer version cannot be restored in this version (schema %s > %s)...", readSchemaVersion, schemaVersion));
        }
    }
}
