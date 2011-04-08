package com.atlassian.dbexporter;

import com.atlassian.dbexporter.exporter.ExportConfiguration;
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
    private final List<Exporter> exporters;

    public DbExporter(final Exporter... exporters)
    {
        this(newArrayList(checkNotNull(exporters)));
    }

    public DbExporter(final List<Exporter> exporters)
    {
        checkArgument(!checkNotNull(exporters).isEmpty(), "DbExporter must be created with at least one Exporter!");
        this.exporters = exporters;
    }

    public void exportData(NodeStreamWriter streamWriter, ExportConfiguration configuration)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin();

        final NodeCreator node = RootNode.add(streamWriter);
        final Context context = new Context();
        for (Exporter exporter : exporters)
        {
            exporter.export(node, configuration, context);
        }

        monitor.end();
    }
}
