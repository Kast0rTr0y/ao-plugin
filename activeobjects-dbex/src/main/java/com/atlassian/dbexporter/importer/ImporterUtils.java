package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.node.NodeParser;

import static com.google.common.base.Preconditions.*;

final class ImporterUtils
{
    private ImporterUtils()
    {
    }

    static void checkStartNode(NodeParser node, String nodeName)
    {
        checkNode(node, nodeName, !node.isClosed());
    }

    static void checkEndNode(NodeParser node, String nodeName)
    {
        checkNode(node, nodeName, node.isClosed());
    }

    private static void checkNode(NodeParser node, String nodeName, boolean closed)
    {
        checkNotNull(node);
        checkState(node.getName().equals(nodeName), "%s is not named '%s' as expected", node, nodeName);
        checkState(closed, "%s is not closed (%s) as expected", node, closed);
    }
}
