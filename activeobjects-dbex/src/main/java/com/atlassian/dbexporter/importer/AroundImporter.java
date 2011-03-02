package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeParser;

public interface AroundImporter
{
    void before(NodeParser node, ImportConfiguration configuration, Context context);

    void after(NodeParser node, ImportConfiguration configuration, Context context);
}
