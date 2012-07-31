package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.ActiveObjectsImportExportException;
import com.atlassian.activeobjects.spi.ImportExportException;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.dbexporter.ImportExportErrorService;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

public final class ImportExportErrorServiceImpl implements ImportExportErrorService
{
    private final PluginInformationFactory pluginInformationFactory;

    public ImportExportErrorServiceImpl(PluginInformationFactory pluginInformationFactory)
    {
        this.pluginInformationFactory = checkNotNull(pluginInformationFactory);
    }

    @Override
    public ImportExportException newImportExportException(String tableName, String message)
    {
        return new ActiveObjectsImportExportException(getPluginInformation(tableName), message);
    }

    @Override
    public ImportExportException newImportExportSqlException(String tableName, String message, SQLException e)
    {
        return new ActiveObjectsImportExportException(getPluginInformation(tableName), message, e);
    }

    @Override
    public ImportExportException newRowImportSqlException(String tableName, long rowNum, SQLException e)
    {
        return new ActiveObjectsImportExportException(
                getPluginInformation(tableName),
                "There has been a SQL exception importing row #"
                        + rowNum + " for table '" + tableName +
                        "' see  the cause of this exception for more detail about it.", e);
    }

    @Override
    public ImportExportException newParseException(Throwable t)
    {
        return new ActiveObjectsImportExportException(getPluginInformation(null), t);
    }

    @Override
    public ImportExportException newParseException(String message)
    {
        return new ActiveObjectsImportExportException(getPluginInformation(null), message);
    }

    @Override
    public ImportExportException newParseException(String message, Throwable t)
    {
        return new ActiveObjectsImportExportException(getPluginInformation(null), message, t);
    }

    private PluginInformation getPluginInformation(String tableName)
    {
        return pluginInformationFactory.getPluginInformation(tableName);
    }
}
