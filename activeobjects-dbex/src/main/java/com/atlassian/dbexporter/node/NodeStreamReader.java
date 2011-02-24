package com.atlassian.dbexporter.node;

import java.io.Closeable;

/**
 * Provides streaming read access to a node graph.
 *
 * @author Erik van Zijst
 */
public interface NodeStreamReader extends Closeable
{

    /**
     * Returns the root node of the object tree. The method can only be invoked
     * once. Subsequent calls will raise an {@link IllegalStateException}.
     *
     * @return the root node of the graph.
     * @throws IllegalStateException when the root node has already been
     * returned.
     * @throws ParseException when the document could not be parsed.
     */
    NodeParser getRootNode() throws ParseException, IllegalStateException;

    /**
     * Closes all resources of the underlying document.
     *
     * @throws ParseException
     */
    void close() throws ParseException;
}
