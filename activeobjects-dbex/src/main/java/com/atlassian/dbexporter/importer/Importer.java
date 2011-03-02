package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeParser;

public interface Importer
{
    void importNode(NodeParser node, ImportConfiguration configuration, Context context);

    boolean supports(NodeParser node);
}
