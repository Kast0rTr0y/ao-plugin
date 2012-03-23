package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.SchemaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SchemaConfiguration schema configuration} that will allow table starting with a given {@link com.atlassian.activeobjects.internal.Prefix prefix}
 * to be managed.
 */
public final class PrefixedSchemaConfiguration implements SchemaConfiguration
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Prefix prefix;

    public PrefixedSchemaConfiguration(Prefix prefix)
    {
        this.prefix = checkNotNull(prefix);
    }

    public final boolean shouldManageTable(String tableName, boolean caseSensitive)
    {
        final boolean should = prefix.isStarting(tableName, caseSensitive);
        logger.debug("Active objects will {} manage table {}", should ? "" : "NOT", tableName);
        return should;
    }
}
