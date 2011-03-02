package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DbImportException;

public final class SchemaVersionDatabaseInformationChecker implements DatabaseInformationChecker
{
    private static final String SCHEMA_VERSION = "schema-version";

    private final int schemaVersion;

    public SchemaVersionDatabaseInformationChecker(int schemaVersion)
    {
        this.schemaVersion = schemaVersion;
    }

    public void check(DatabaseInformation information)
    {
        final int readSchemaVersion = information.getInt(SCHEMA_VERSION, -1);
        if (readSchemaVersion > schemaVersion)
        {
            throw new DbImportException(String.format("ERROR: This backup archive is from a " +
                    "newer version cannot be restored in this version (schema %s > %s)...", readSchemaVersion, schemaVersion));
        }
    }
}
