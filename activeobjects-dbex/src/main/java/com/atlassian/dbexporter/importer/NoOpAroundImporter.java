package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeParser;

/**
 * <p>A no-op around importer, convenient when only overriding one method.</p>
 * <p>The isn't much reason using it for anything else...</p>
 */
public class NoOpAroundImporter implements AroundImporter
{
    @Override
    public void before(NodeParser node, ImportConfiguration configuration, Context context)
    {
        // do nothing
    }

    @Override
    public void after(NodeParser node, ImportConfiguration configuration, Context context)
    {
        // do nothing
    }
}
