package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.TableCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.SQLAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.activeobjects.backup.SqlUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsTableCreator extends AbstractActiveObjectsTableManager implements TableCreator
{
    private final Logger logger = LoggerFactory.getLogger("net.java.ao.sql");

    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;
    private final NameConverters converters;

    public ActiveObjectsTableCreator(ImportExportErrorService errorService, DatabaseProvider provider, NameConverters converters)
    {
        super(errorService, provider);
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
        this.converters = checkNotNull(converters);
    }

    public void create(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor, ProgressMonitor monitor)
    {
        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = provider.getConnection();
            stmt = conn.createStatement();

            for (Table table : tables)
            {
                monitor.begin(ProgressMonitor.Task.TABLE_CREATION, entityNameProcessor.tableName(table.getName()));
                create(DatabaseInformations.database(databaseInformation), stmt, table, entityNameProcessor);
                monitor.end(ProgressMonitor.Task.TABLE_CREATION, entityNameProcessor.tableName(table.getName()));
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

    private void create(DatabaseInformations.Database db, Statement stmt, Table table, EntityNameProcessor entityNameProcessor)
    {
        final DDLAction a = new DDLAction(DDLActionType.CREATE);
        a.setTable(toDdlTable(exportTypeManager(db), entityNameProcessor, table));
        final Iterable<SQLAction> sqlStatements = provider.renderAction(converters, a);
        for (SQLAction sql : sqlStatements)
        {
            executeUpdate(errorService, table.getName(), stmt, sql.getStatement());
        }
    }

}
