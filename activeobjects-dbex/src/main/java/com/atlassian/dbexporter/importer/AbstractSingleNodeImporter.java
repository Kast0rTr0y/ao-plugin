package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.node.NodeParser;

import java.util.List;

import static com.google.common.base.Preconditions.*;

public abstract class AbstractSingleNodeImporter extends AbstractImporter
{
    protected AbstractSingleNodeImporter(ImportExportErrorService errorService)
    {
        super(errorService);
    }

    protected AbstractSingleNodeImporter(ImportExportErrorService errorService, List<AroundImporter> arounds)
    {
        super(errorService, arounds);
    }

    public final boolean supports(NodeParser node)
    {
        return checkNotNull(node).getName().equals(getNodeName());
    }

    protected abstract String getNodeName();
}
