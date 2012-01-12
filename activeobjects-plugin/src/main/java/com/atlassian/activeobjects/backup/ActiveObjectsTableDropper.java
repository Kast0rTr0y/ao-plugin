package com.atlassian.activeobjects.backup;

import static com.atlassian.activeobjects.backup.SqlUtils.executeUpdate;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.closeQuietly;
import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.SQLAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.TableDropper;
import com.google.common.collect.Lists;

final public class ActiveObjectsTableDropper extends AbstractActiveObjectsTableManager implements TableDropper
{
    private final Logger logger = LoggerFactory.getLogger("net.java.ao.sql");

    private final DatabaseProvider provider;
    private final NameConverters converters;
    private final ImportExportErrorService errorService;

    public ActiveObjectsTableDropper(ImportExportErrorService errorService, DatabaseProvider provider, NameConverters converters)
    {
        super(errorService, provider);
        this.provider = checkNotNull(provider);
        this.converters = checkNotNull(converters);
        this.errorService = errorService;
    }

    public void drop(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor)
    {
        // Sort the tables so that foreign keys are dropped first
        List<Table> orderedTables = Lists.newArrayList(tables);
        Collections.sort(orderedTables, new Comparator<Table>()
        {
            @Override
            public int compare(Table table1, Table table2)
            {
                // It seems keys are only represented on the From side,
                // se we don't test getFromTable()
                for (ForeignKey fk : table1.getForeignKeys())
                {
                    if (table2.getName().equals(fk.getToTable()))
                    {
                        // Table1 is dependant on table2
                        return -1;
                    }
                }

                for (ForeignKey fk : table2.getForeignKeys())
                {
                    if (table1.getName().equals(fk.getToTable()))
                    {
                        // Table2 is dependant on table1
                        return 1;
                    }
                }

                return 0;
            }
        });

        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = provider.getConnection();
            stmt = conn.createStatement();

            for (Table table : orderedTables)
            {
                drop(DatabaseInformations.database(databaseInformation), stmt, table, entityNameProcessor);
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private void drop(DatabaseInformations.Database db, Statement stmt, Table table, EntityNameProcessor entityNameProcessor)
    {
        final DDLAction a = new DDLAction(DDLActionType.DROP);
        a.setTable(toDdlTable(exportTypeManager(db), entityNameProcessor, table));
        final Iterable<SQLAction> sqlStatements = provider.renderAction(converters, a);
        for (SQLAction sql : sqlStatements)
        {
            executeUpdate(errorService, table.getName(), stmt, sql.getStatement());
        }
    }
}
