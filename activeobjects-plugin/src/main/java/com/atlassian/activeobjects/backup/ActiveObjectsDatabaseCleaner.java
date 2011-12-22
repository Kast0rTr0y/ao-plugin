package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.CleanupMode;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.importer.DatabaseCleaner;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SQLAction;
import net.java.ao.schema.ddl.SchemaReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.activeobjects.backup.SqlUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsDatabaseCleaner implements DatabaseCleaner
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ImportExportErrorService errorService;
    private final NameConverters converters;
    private final DatabaseProvider provider;
    private final SchemaConfiguration schemaConfiguration;

    public ActiveObjectsDatabaseCleaner(DatabaseProvider provider, NameConverters converters, SchemaConfiguration schemaConfiguration, ImportExportErrorService errorService)
    {
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
        this.converters = checkNotNull(converters);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
    }

    @Override
    public void cleanup(CleanupMode cleanupMode)
    {
        if (cleanupMode.equals(CleanupMode.CLEAN))
        {
            doCleanup();
        }
        else
        {
            logger.debug("Not cleaning up database before import. " +
                    "Any existing entity with the same name of entity being imported will make the import fail.");
        }
    }

    private void doCleanup()
    {
        Connection conn = null;
        Statement stmt = null;
        try
        {
            final DDLTable[] readTables = SchemaReader.readSchema(provider, converters, schemaConfiguration);
            final DDLAction[] actions = SchemaReader.sortTopologically(SchemaReader.diffSchema(provider.getTypeManager(), new DDLTable[]{}, readTables, provider.isCaseSensitive()));

            conn = provider.getConnection();
            stmt = conn.createStatement();

            for (DDLAction a : actions)
            {
                final Iterable<SQLAction> sqlActions = provider.renderAction(converters, a);
                for (SQLAction sql : sqlActions)
                {
                    executeUpdate(errorService, tableName(a), stmt, sql.getStatement());
                }
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

    private String tableName(DDLAction a)
    {
        if (a == null)
        {
            return null;
        }
        if (a.getTable() == null)
        {
            return null;
        }
        return a.getTable().getName();
    }
}
