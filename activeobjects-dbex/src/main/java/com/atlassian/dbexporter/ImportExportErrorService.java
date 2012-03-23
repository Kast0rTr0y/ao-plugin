package com.atlassian.dbexporter;

import com.atlassian.activeobjects.spi.ImportExportException;

import java.sql.SQLException;

public interface ImportExportErrorService
{
    ImportExportException newImportExportException(String tableName, String message);

    ImportExportException newImportExportSqlException(String tableName, String message, SQLException e);

    ImportExportException newRowImportSqlException(String tableName, long rowNum, SQLException e);

    ImportExportException newParseException(Throwable t);

    ImportExportException newParseException(String message);

    ImportExportException newParseException(String message, Throwable t);
}
