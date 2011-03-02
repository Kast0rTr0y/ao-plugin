package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.ImportExportConfiguration;

public interface ImportConfiguration extends ImportExportConfiguration
{
    /**
     * This is information of the targeted database.
     *
     * @return the "complete" database information of database data is being imported into
     * @see DatabaseInformation
     */
    DatabaseInformation getDatabaseInformation();

    BatchMode getBatchMode();
}
