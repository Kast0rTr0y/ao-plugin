package com.atlassian.activeobjects.internal.config;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.schema.NameConverters;

public interface NameConvertersFactory
{
    NameConverters getNameConverters(Prefix prefix);
}
