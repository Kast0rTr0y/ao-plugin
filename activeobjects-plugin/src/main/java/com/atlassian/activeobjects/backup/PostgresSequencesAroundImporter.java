package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.NoOpAroundImporter;
import com.atlassian.dbexporter.node.NodeParser;
import net.java.ao.DatabaseProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.activeobjects.backup.SqlUtils.*;
import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

/**
 * Updates the auto-increment sequences so that they start are the correct min value after some data has been 'manually'
 * imported into the database.
 */
public final class PostgresSequencesAroundImporter extends NoOpAroundImporter
{
    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;

    public PostgresSequencesAroundImporter(ImportExportErrorService errorService, DatabaseProvider provider)
    {
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
    }

    @Override
    public void after(NodeParser node, ImportConfiguration configuration, Context context)
    {
        if (isPostgres(configuration))
        {
            updateSequences(configuration, context);
        }
    }

    private boolean isPostgres(ImportConfiguration configuration)
    {
        return Database.Type.POSTGRES.equals(database(configuration.getDatabaseInformation()).getType());
    }

    private void updateSequences(ImportConfiguration configuration, Context context)
    {
        final EntityNameProcessor entityNameProcessor = configuration.getEntityNameProcessor();
        for (SqlUtils.TableColumnPair tableColumnPair : tableColumnPairs(context.getAll(Table.class)))
        {
            final String tableName = entityNameProcessor.tableName(tableColumnPair.table.getName());
            final String columnName = entityNameProcessor.columnName(tableColumnPair.column.getName());
            updateSequence(tableName, columnName);
        }
    }

    private void updateSequence(String tableName, String columnName)
    {
        Connection connection = null;
        Statement maxStmt = null;
        Statement alterSeqStmt = null;
        try
        {
            connection = provider.getConnection();
            maxStmt = connection.createStatement();

            final ResultSet res = executeQuery(errorService, tableName, maxStmt, max(connection, tableName, columnName));

            final int max = getIntFromResultSet(errorService, tableName, res);
            alterSeqStmt = connection.createStatement();
            executeUpdate(errorService, tableName, alterSeqStmt, alterSequence(connection, tableName, columnName, max + 1));
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "", e);
        }
        finally
        {
            closeQuietly(maxStmt, alterSeqStmt);
            closeQuietly(connection);
        }
    }

    private String max(Connection connection, String tableName, String columnName)
    {
        return "SELECT MAX(" + quote(errorService, tableName, connection, columnName) + ") FROM " + tableName(connection, tableName);
    }

    private String tableName(Connection connection, String tableName)
    {
        final String schema = isBlank(provider.getSchema()) ? null : provider.getSchema();
        final String quoted = quote(errorService, tableName, connection, tableName);
        return schema != null ? schema + "." + quoted : quoted;
    }

    private String alterSequence(Connection connection, String tableName, String columnName, int val)
    {
        return "ALTER SEQUENCE " + sequenceName(connection, tableName, columnName) + " RESTART WITH " + val;
    }

    private String sequenceName(Connection connection, String tableName, String columnName)
    {
        final String schema = isBlank(provider.getSchema()) ? null : provider.getSchema();
        final String quoted = quote(errorService, tableName, connection, tableName + "_" + columnName + "_" + "seq");
        return schema != null ? schema + "." + quoted : quoted;
    }

    private static boolean isBlank(String str)
    {
        int strLen;
        if (str == null || (strLen = str.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(str.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }
}
