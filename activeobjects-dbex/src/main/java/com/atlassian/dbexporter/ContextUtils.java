package com.atlassian.dbexporter;

import com.atlassian.dbexporter.progress.ProgressMonitor;

import static com.google.common.base.Preconditions.*;

public final class ContextUtils
{
    private ContextUtils()
    {
    }

    public static ProgressMonitor getProgressMonitor(Context context)
    {
        return checkNotNull(context).getRequired(ProgressMonitor.class);
    }

    public static BatchMode getBatchMode(Context context)
    {
        final BatchMode batchMode = checkNotNull(context).get(BatchMode.class);
        return batchMode != null ? batchMode : BatchMode.ON;
    }

    public static DatabaseInformation getDatabaseInformation(Context context)
    {
        return checkNotNull(context).get(DatabaseInformation.class);
    }

    public static ConnectionProvider getConnectionProvider(Context context)
    {
        return checkNotNull(context).getRequired(ConnectionProvider.class);
    }
}
