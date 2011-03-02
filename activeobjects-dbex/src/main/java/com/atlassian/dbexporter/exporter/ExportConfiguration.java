package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.progress.ProgressMonitor;

public interface ExportConfiguration
{
    ConnectionProvider getConnectionProvider();

    ProgressMonitor getProgressMonitor();

    EntityNameProcessor getEntityNameProcessor();
}
