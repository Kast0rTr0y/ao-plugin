package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.SchemaConfiguration;

public class PrefixedSchemaConfigurationFactory
{
    public SchemaConfiguration getSchemaConfiguration(Prefix prefix)
    {
        return new PrefixedSchemaConfiguration(prefix);
    }
}
