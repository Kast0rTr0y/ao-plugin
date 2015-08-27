package com.atlassian.activeobjects.spi;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActiveObjectsImportExportException extends ImportExportException {
    private final PluginInformation pluginInformation;
    private final String tableName;

    public ActiveObjectsImportExportException(String tableName, PluginInformation pluginInformation, String message) {
        super(message);
        this.pluginInformation = checkNotNull(pluginInformation);
        this.tableName = tableName;
    }

    public ActiveObjectsImportExportException(String tableName, PluginInformation pluginInformation, Throwable t) {
        super(t);
        this.pluginInformation = checkNotNull(pluginInformation);
        this.tableName = tableName;
    }

    public ActiveObjectsImportExportException(String tableName, PluginInformation pluginInformation, String message, Throwable t) {
        super(message, t);
        this.pluginInformation = checkNotNull(pluginInformation);
        this.tableName = tableName;
    }

    public PluginInformation getPluginInformation() {
        return pluginInformation;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String getMessage() {
        return "There was an error during import/export with " + pluginInformation
                + (tableName != null ? " (table " + tableName + ")" : "")
                + ":" + super.getMessage();
    }
}
