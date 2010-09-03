package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.SchemaConfiguration;

public final class PrefixedSchemaConfigurationFactoryImpl implements PrefixedSchemaConfigurationFactory
{
    public SchemaConfiguration getSchemaConfiguration(Prefix prefix)
    {
        return new PrefixedSchemaConfiguration(prefix);
    }
}
