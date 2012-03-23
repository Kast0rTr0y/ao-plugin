package com.atlassian.activeobjects.junit;

import org.junit.rules.TemporaryFolder;

public final class ConfigurableTemporaryFolder extends TemporaryFolder
{
    private final boolean deleteOnExit;

    public ConfigurableTemporaryFolder()
    {
        this(true);
    }

    public ConfigurableTemporaryFolder(boolean deleteOnExit)
    {
        this.deleteOnExit = deleteOnExit;
    }

    @Override
    public void delete()
    {
        if (deleteOnExit)
        {
            super.delete();
        }
    }
}
