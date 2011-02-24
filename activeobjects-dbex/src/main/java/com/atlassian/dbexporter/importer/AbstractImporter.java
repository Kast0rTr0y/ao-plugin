package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.node.NodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.google.common.base.Preconditions.*;

public abstract class AbstractImporter implements Importer
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<AroundImporter> arounds;

    protected AbstractImporter()
    {
        this(Collections.<AroundImporter>emptyList());
    }

    protected AbstractImporter(List<AroundImporter> arounds)
    {
        this.arounds = checkNotNull(arounds);
    }

    @Override
    public final void importNode(NodeParser node, Context context)
    {
        checkNotNull(node);
        checkArgument(!node.isClosed(), "Node must not be closed to be imported! " + node);
        checkArgument(supports(node), "Importer called on unsupported node! " + node);
        checkNotNull(context);

        logger.debug("Importing node {}", node);

        for (AroundImporter around : arounds)
        {
            around.before(node, context);
        }

        doImportNode(node, context);

        for (ListIterator<AroundImporter> iterator = arounds.listIterator(arounds.size()); iterator.hasPrevious();)
        {
            iterator.previous().after(node, context);
        }
    }

    protected abstract void doImportNode(NodeParser node, Context context);
}
