package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeCreator;

import java.util.Map;

import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static java.util.Collections.*;

public final class DatabaseInformationExporter implements Exporter
{
    private final DatabaseInformationReader databaseInformationReader;

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

    public DatabaseInformationExporter(DatabaseInformationReader databaseInformationReader)
    {
        this.databaseInformationReader = checkNotNull(databaseInformationReader);
    }

    @Override
    public void export(NodeCreator node, ExportConfiguration configuration, Context context)
    {
        final Map<String, String> properties = databaseInformationReader.get();
        if (!properties.isEmpty())
        {
            export(node, properties);
        }
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
