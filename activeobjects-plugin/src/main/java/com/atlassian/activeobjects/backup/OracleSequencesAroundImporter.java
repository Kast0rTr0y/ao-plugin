package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformations;
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
import java.util.Collection;

import static com.atlassian.activeobjects.backup.SequenceUtils.*;
import static com.atlassian.dbexporter.DatabaseInformations.database;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.checkNotNull;

public final class OracleSequencesAroundImporter extends NoOpAroundImporter
{
    private final DatabaseProvider provider;

    public OracleSequencesAroundImporter(DatabaseProvider provider)
    {
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
                executeUpdate(connection, "ALTER TABLE " + quote(connection, table.getName()) + " DISABLE ALL TRIGGERS");
            }
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
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
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private void dropSequence(Connection connection, TableColumnPair tcp)
    {
        executeUpdate(connection, "DROP SEQUENCE " + sequenceName(tcp));
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
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private void createSequence(Connection connection, TableColumnPair tcp)
    {
        Statement maxStmt = null;
        try
        {
            maxStmt = connection.createStatement();
            final ResultSet res = executeQuery(maxStmt, "SELECT MAX(" + quote(connection, tcp.column.getName()) + ")" +
                    " FROM " + quote(connection, tcp.table.getName()));
            final int max = getIntFromResultSet(res);
            executeUpdate(connection, "CREATE SEQUENCE " + sequenceName(tcp)
                    + " INCREMENT BY 1 START WITH " + (max + 1) + " NOMAXVALUE MINVALUE " + (max + 1));
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
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
                executeUpdate(connection, "ALTER TABLE " + quote(connection, table.getName()) + " ENABLE ALL TRIGGERS");
            }
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
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
