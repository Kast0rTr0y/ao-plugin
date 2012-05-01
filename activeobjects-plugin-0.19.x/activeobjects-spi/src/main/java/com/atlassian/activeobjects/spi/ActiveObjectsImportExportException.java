package com.atlassian.activeobjects.spi;


import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsImportExportException extends ImportExportException
{
    private final PluginInformation pluginInformation;

    public ActiveObjectsImportExportException(PluginInformation pluginInformation, String message)
    {
        super(message);
        this.pluginInformation = checkNotNull(pluginInformation);
    }

    public ActiveObjectsImportExportException(PluginInformation pluginInformation, Throwable t)
    {
        super(t);
        this.pluginInformation = checkNotNull(pluginInformation);
    }

    public ActiveObjectsImportExportException(PluginInformation pluginInformation, String message, Throwable t)
    {
        super(message, t);
        this.pluginInformation = checkNotNull(pluginInformation);
    }

    public PluginInformation getPluginInformation()
    {
        return pluginInformation;
    }

    @Override
    public String getMessage()
    {
        return "There was an error during import/export with " + pluginInformation + ":" + super.getMessage();
    }
}
