package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.SchemaConfiguration;

public interface PrefixedSchemaConfigurationFactory
{
    SchemaConfiguration getSchemaConfiguration(Prefix prefix);
}
