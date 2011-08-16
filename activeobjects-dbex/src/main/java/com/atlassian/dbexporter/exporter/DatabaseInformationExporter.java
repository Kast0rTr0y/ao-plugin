package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.atlassian.dbexporter.progress.ProgressMonitor.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.*;

public final class DatabaseInformationExporter implements Exporter
{
    private final List<DatabaseInformationReader> informationReaders;

    public DatabaseInformationExporter()
    {
        this(new DatabaseInformationReader()
        {
            public Map<String, String> get()
            {
                return emptyMap();
            }
        });
    }

    public DatabaseInformationExporter(DatabaseInformationReader... informationReaders)
    {
        this.informationReaders = newArrayList(checkNotNull(informationReaders));
    }

    @Override
    public void export(NodeCreator node, ExportConfiguration configuration, Context context)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.DATABASE_INFORMATION);
        final Map<String, String> properties = allProperties();
        if (!properties.isEmpty())
        {
            export(node, properties);
        }
        context.put(new DatabaseInformation(properties));
        monitor.end(Task.DATABASE_INFORMATION);
    }

    private Map<String, String> allProperties()
    {
        final Map<String, String> props = Maps.newTreeMap();
        for (DatabaseInformationReader informationReader : informationReaders)
        {
            props.putAll(informationReader.get());
        }
        return props;
    }

    private void export(NodeCreator node, Map<String, String> properties)
    {
        node.addNode(DatabaseInformationNode.NAME);
        for (Map.Entry<String, String> property : properties.entrySet())
        {
            DatabaseInformationNode.addMeta(node, property.getKey(), property.getValue());
        }
        node.closeEntity();
    }
}
