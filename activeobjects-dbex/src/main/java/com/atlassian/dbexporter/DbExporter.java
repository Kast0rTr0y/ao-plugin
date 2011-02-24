package com.atlassian.dbexporter;

import com.atlassian.dbexporter.exporter.Exporter;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.node.NodeStreamWriter;

import java.util.List;

import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

/**
 * <p>Creates an export of a database. What exactly the export 'looks' like depends heavily on the exporters passed-in.</p>
 * <p>A {@link ProgressMonitor} can be supplied to be notified of the progress as the export is being made.</p>
 *
 * @author Erik van Zijst
 * @author Samuel Le Berrigaud
 */
public final class DbExporter
{
    private final ProgressMonitor monitor;
    private final List<Exporter> exporters;

    public DbExporter(final ProgressMonitor monitor, final Exporter... exporters)
    {
        this(monitor, newArrayList(checkNotNull(exporters)));
    }

    public DbExporter(final ProgressMonitor monitor, final List<Exporter> exporters)
    {
        checkArgument(!checkNotNull(exporters).isEmpty(), "DbExporter must be created with at least one Exporter!");
        this.monitor = checkNotNull(monitor);
        this.exporters = exporters;
    }

    public void exportData(NodeStreamWriter streamWriter, ConnectionProvider connectionProvider, Object... configuration)
    {
        final Context context = new Context(monitor, connectionProvider).putAll(newArrayList(configuration));

        final NodeCreator node = RootNode.add(streamWriter);
        for (Exporter exporter : exporters)
        {
            exporter.export(node, context);
        }
    }
}
