package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformations;
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
import java.util.Collection;

import static com.atlassian.activeobjects.backup.SequenceUtils.*;
import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

public final class OracleSequencesAroundImporter extends NoOpAroundImporter
{
    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;

    public OracleSequencesAroundImporter(ImportExportErrorService errorService, DatabaseProvider provider)
    {
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
    }

    @Override
    public void before(NodeParser node, ImportConfiguration configuration, Context context)
    {
        if (isOracle(configuration))
        {
            doBefore(context);
        }
    }

    @Override
    public void after(NodeParser node, ImportConfiguration configuration, Context context)
    {
        if (isOracle(configuration))
        {
            doAfter(context);
        }
    }

    private boolean isOracle(ImportConfiguration configuration)
    {
        return DatabaseInformations.Database.Type.ORACLE.equals(database(configuration.getDatabaseInformation()).getType());
    }

    private void doBefore(Context context)
    {
        final Collection<Table> tables = context.getAll(Table.class);
        disableAllTriggers(tables);
        dropAllSequences(tables);
    }

    private void doAfter(Context context)
    {
        final Collection<Table> tables = context.getAll(Table.class);
        createAllSequences(tables);
        enableAllTriggers(tables);
    }

    private void disableAllTriggers(Collection<Table> tables)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            for (Table table : tables)
            {
                executeUpdate(errorService, table.getName(), connection, "ALTER TABLE " + quote(errorService, table.getName(), connection, table.getName()) + " DISABLE ALL TRIGGERS");
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private void dropAllSequences(Collection<Table> tables)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            for (TableColumnPair tcp : tableColumnPairs(tables))
            {
                dropSequence(connection, tcp);
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private void dropSequence(Connection connection, TableColumnPair tcp)
    {
        executeUpdate(errorService, tcp.table.getName(), connection, "DROP SEQUENCE " + sequenceName(tcp));
    }

    private void createAllSequences(Collection<Table> tables)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            for (TableColumnPair tcp : tableColumnPairs(tables))
            {
                createSequence(connection, tcp);
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private void createSequence(Connection connection, TableColumnPair tcp)
    {
        Statement maxStmt = null;
        final String tableName = tcp.table.getName();
        try
        {
            maxStmt = connection.createStatement();
            final ResultSet res = executeQuery(errorService, tableName, maxStmt, "SELECT MAX(" + quote(errorService, tableName, connection, tcp.column.getName()) + ")" +
                    " FROM " + quote(errorService, tableName, connection, tableName));
            final int max = getIntFromResultSet(errorService, tableName, res);
            executeUpdate(errorService, tableName, connection, "CREATE SEQUENCE " + sequenceName(tcp)
                    + " INCREMENT BY 1 START WITH " + (max + 1) + " NOMAXVALUE MINVALUE " + (max + 1));
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "", e);
        }
        finally
        {
            closeQuietly(maxStmt);
        }
    }

    private void enableAllTriggers(Collection<Table> tables)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            for (Table table : tables)
            {
                executeUpdate(errorService, table.getName(), connection, "ALTER TABLE " + quote(errorService, table.getName(), connection, table.getName()) + " ENABLE ALL TRIGGERS");
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private static String sequenceName(TableColumnPair tcp)
    {
        return tcp.table.getName() + "_" + tcp.column.getName() + "_SEQ";
    }
}
