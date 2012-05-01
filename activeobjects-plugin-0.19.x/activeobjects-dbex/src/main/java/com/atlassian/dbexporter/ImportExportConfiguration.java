package com.atlassian.dbexporter;

import com.atlassian.dbexporter.progress.ProgressMonitor;

/**
 * Gives access to the essential configuration elements used during import/export
 *
 * @author Samuel Le Berrigaud
 */
public interface ImportExportConfiguration
{
    ConnectionProvider getConnectionProvider();

    ProgressMonitor getProgressMonitor();

    EntityNameProcessor getEntityNameProcessor();
}
