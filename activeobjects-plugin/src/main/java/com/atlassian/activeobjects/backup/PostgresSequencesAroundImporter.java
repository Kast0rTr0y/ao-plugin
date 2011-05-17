package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.NoOpAroundImporter;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.atlassian.dbexporter.node.NodeParser;
import net.java.ao.DatabaseProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.activeobjects.backup.SequenceUtils.*;
import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

/**
 * Updates the auto-increment sequences so that they start are the correct min value after some data has been 'manually'
 * imported into the database.
 */
public final class PostgresSequencesAroundImporter extends NoOpAroundImporter
{
    private final DatabaseProvider provider;

    public PostgresSequencesAroundImporter(DatabaseProvider provider)
    {
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
        for (SequenceUtils.TableColumnPair tableColumnPair : tableColumnPairs(context.getAll(Table.class)))
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

            final ResultSet res = executeQuery(maxStmt, max(connection, tableName, columnName));

            final int max = getIntFromResultSet(res);
            alterSeqStmt = connection.createStatement();
            executeUpdate(alterSeqStmt, alterSequence(connection, tableName, columnName, max + 1));
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(maxStmt, alterSeqStmt);
            closeQuietly(connection);
        }
    }

    private static String max(Connection connection, String tableName, String columnName)
    {
        return "SELECT MAX(" + quote(connection, columnName) + ") FROM " + quote(connection, tableName);
    }

    private static String alterSequence(Connection connection, String tableName, String columnName, int val)
    {
        return "ALTER SEQUENCE " + quote(connection, sequenceName(tableName, columnName)) + " RESTART WITH " + val;
    }

    private static String sequenceName(String tableName, String columnName)
    {
        return tableName + "_" + columnName + "_" + "seq";
    }
}
