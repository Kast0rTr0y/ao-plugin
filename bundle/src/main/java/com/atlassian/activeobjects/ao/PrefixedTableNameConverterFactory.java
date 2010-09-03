package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.schema.TableNameConverter;

public interface PrefixedTableNameConverterFactory
{
    TableNameConverter getTableNameConverter(Prefix prefix);
}
