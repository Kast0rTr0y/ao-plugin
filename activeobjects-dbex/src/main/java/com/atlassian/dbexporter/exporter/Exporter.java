package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeCreator;

public interface Exporter
{
    void export(NodeCreator node, ExportConfiguration configuration, Context context);
}
