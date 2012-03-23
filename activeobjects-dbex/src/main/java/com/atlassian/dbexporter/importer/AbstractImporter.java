package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.node.NodeParser;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.google.common.base.Preconditions.*;

public abstract class AbstractImporter implements Importer
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ImportExportErrorService errorService;
    private List<AroundImporter> arounds;

    protected AbstractImporter(ImportExportErrorService errorService)
    {
        this(errorService, Collections.<AroundImporter>emptyList());
    }

    protected AbstractImporter(ImportExportErrorService errorService, List<AroundImporter> arounds)
    {
        this.errorService = checkNotNull(errorService);
        this.arounds = checkNotNull(arounds);
    }

    @Override
    public final void importNode(NodeParser node, ImportConfiguration configuration, Context context)
    {
        checkNotNull(node);
        checkArgument(!node.isClosed(), "Node must not be closed to be imported! " + node);
        checkArgument(supports(node), "Importer called on unsupported node! " + node);
        checkNotNull(context);

        logger.debug("Importing node {}", node);

        for (AroundImporter around : arounds)
        {
            around.before(node, configuration, context);
        }

        doImportNode(node, configuration, context);

        for (ListIterator<AroundImporter> iterator = arounds.listIterator(arounds.size()); iterator.hasPrevious(); )
        {
            iterator.previous().after(node, configuration, context);
        }
    }

    protected abstract void doImportNode(NodeParser node, ImportConfiguration configuration, Context context);
}
