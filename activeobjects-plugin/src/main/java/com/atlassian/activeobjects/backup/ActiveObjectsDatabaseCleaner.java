package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.CleanupMode;
import com.atlassian.dbexporter.importer.DatabaseCleaner;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsDatabaseCleaner implements DatabaseCleaner
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DatabaseProvider provider;
    private final SchemaConfiguration schemaConfiguration;

    public ActiveObjectsDatabaseCleaner(DatabaseProvider provider, SchemaConfiguration schemaConfiguration)
    {
        this.provider = checkNotNull(provider);
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
            final DDLTable[] readTables = SchemaReader.readSchema(provider, schemaConfiguration);
            final DDLAction[] actions = SchemaReader.sortTopologically(SchemaReader.diffSchema(new DDLTable[]{}, readTables, provider.isCaseSensetive()));

            conn = provider.getConnection();
            stmt = conn.createStatement();

            for (DDLAction a : actions)
            {
                final String[] sqlStatements = provider.renderAction(a);
                for (String sql : sqlStatements)
                {
                    try
                    {
                        stmt.executeUpdate(sql);
                    }
                    catch (SQLException e)
                    {
                        onSqlException(sql, e);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private void onSqlException(String sql, SQLException e)
    {
        if (sql.startsWith("DROP") && e.getMessage().contains("does not exist"))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignoring exception for SQL <" + sql + ">", e);
            }
            return;
        }
        throw new ImportExportSqlException(e);
    }
}
