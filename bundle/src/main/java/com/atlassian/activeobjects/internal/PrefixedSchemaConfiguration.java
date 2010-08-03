package com.atlassian.activeobjects.internal;

import net.java.ao.SchemaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * A {@link SchemaConfiguration schema configuration} that will allow table starting with a given {@link Prefix prefix}
 * to be managed.
 */
public class PrefixedSchemaConfiguration implements SchemaConfiguration
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Prefix prefix;

    public PrefixedSchemaConfiguration(Prefix prefix)
    {
        this.prefix = checkNotNull(prefix);
    }

    public final boolean shouldManageTable(String tableName)
    {
        final boolean should = prefix.isStarting(tableName);
        logger.debug("Active objects will {} manage table {}", should ? "" : "NOT", tableName);
        return should;
    }
}
