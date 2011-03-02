package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.progress.ProgressMonitor;

public interface ImportConfiguration
{
    ConnectionProvider getConnectionProvider();

    ProgressMonitor getProgressMonitor();

    BatchMode getBatchMode();

    EntityNameProcessor getEntityNameProcessor();
}
