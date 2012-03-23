package com.atlassian.dbexporter.node;

import java.io.Closeable;

/** @author Erik van Zijst */
public interface NodeStreamWriter extends Closeable
{

    /**
     * Creates the root node for the graph. This method can be called only
     * once.
     *
     * @throws IllegalStateException when this method is called more than once.
     */
    NodeCreator addRootNode(String name) throws IllegalStateException;

    void flush();

    void close();
}
