package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.node.NodeParser;

import static com.google.common.base.Preconditions.*;

public final class ImporterUtils
{
    private ImporterUtils()
    {
    }

    public static void checkStartNode(NodeParser node, String nodeName)
    {
        checkNode(node, nodeName, !node.isClosed());
    }

    /**
     * Checks that the node is the node with the given name and is not closed.
     *
     * @param node the node to check
     * @param nodeName the name that the node must match.
     * @return {@code true} if and only if the node is of the given {@code nodeName} and {@link NodeParser#isClosed() is not closed}
     * @throws NullPointerException is {@code node} or {@code nodeName} is {@code null}
     */
    public static boolean isNodeNotClosed(NodeParser node, String nodeName)
    {
        checkNotNull(node);
        checkNotNull(nodeName);
        return !node.isClosed() && nodeName.equals(node.getName());
    }

    public static void checkEndNode(NodeParser node, String nodeName)
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
