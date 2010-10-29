package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * A {@link TableNameConverter table name converter} that will prepend the given {@link com.atlassian.activeobjects.internal.Prefix} to table names.
 * It uses a {@link TableNameConverter delegate table name converter} for the <em>general</em> conversion strategy.
 */
public final class PrefixedTableNameConverter implements TableNameConverter
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Prefix prefix;

    /**
     * The table name converter we delegate the real conversion to
     */
    private final TableNameConverter delegate;

    public PrefixedTableNameConverter(Prefix prefix, TableNameConverter delegate)
    {
        this.prefix = checkNotNull(prefix);
        this.delegate = checkNotNull(delegate);
    }

    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        final String tableName = prefix.prepend(delegate.getName(clazz));
        logger.debug("Table name for class <{}> is <{}>", clazz.getName(), tableName);
        return tableName;
    }
}
