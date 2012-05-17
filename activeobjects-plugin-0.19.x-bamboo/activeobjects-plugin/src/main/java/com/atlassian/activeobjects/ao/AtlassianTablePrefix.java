package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.atlassian.TablePrefix;

import static com.google.common.base.Preconditions.*;

public final class AtlassianTablePrefix implements TablePrefix
{
    private final Prefix prefix;

    public AtlassianTablePrefix(Prefix prefix)
    {
        this.prefix = checkNotNull(prefix);
    }

    @Override
    public String prepend(String s)
    {
        return prefix.prepend(s);
    }
}
