package com.atlassian.activeobjects.admin.tables;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.atlassian.activeobjects.backup.ActiveObjectsBackup;
import com.atlassian.activeobjects.backup.ActiveObjectsTableDropper;
import com.atlassian.activeobjects.backup.ActiveObjectsTableReader;
import com.atlassian.activeobjects.backup.ImportExportErrorServiceImpl;
import com.atlassian.activeobjects.backup.PluginInformationFactory;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.ImportExportException;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.exporter.ConnectionProviderInformationReader;
import com.atlassian.dbexporter.exporter.DatabaseInformationReader;
import com.atlassian.dbexporter.exporter.TableReader;
import com.atlassian.plugin.web.springmvc.xsrf.XsrfTokenGenerator;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@Controller
public final class TablesController
{
    private static final String AO_ADMIN_DELETE_TABLE_MESSAGE = "aoAdminDeleteTableMessage";

    private static final String AO_ADMIN_DELETE_TABLE_LIST = "aoAdminDeleteTableList";

    private static final String AO_ADMIN_DELETE_TABLE_STATUS = "aoAdminDeleteTableStatus";

    private final Logger logger = LoggerFactory.getLogger(TablesController.class);

    private final DatabaseProviderFactory databaseProviderFactory;
    private final NameConverters nameConverters;
    private final DataSourceProvider dataSourceProvider;
    private final ImportExportErrorServiceImpl errorService;
    private final PluginInformationFactory pluginInformationFactory;
    private final I18nResolver i18n;
    private final XsrfTokenGenerator xsrfTokenGenerator;

    public TablesController(DatabaseProviderFactory databaseProviderFactory, NameConverters nameConverters,
            DataSourceProvider dataSourceProvider,
            ImportExportErrorServiceImpl errorService, PluginInformationFactory pluginInformationFactory,
            I18nResolver i18n, XsrfTokenGenerator xsrfTokenGenerator)
    {
        this.pluginInformationFactory = checkNotNull(pluginInformationFactory);
        this.nameConverters = checkNotNull(nameConverters);
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.errorService = checkNotNull(errorService);
        this.i18n = i18n;
        this.xsrfTokenGenerator = xsrfTokenGenerator;
    }

    @RequestMapping(value = "/tables/list")
    public ModelAndView list(HttpServletRequest request, HttpSession session) throws Exception
    {
        final DatabaseProvider databaseProvider = getDatabaseProvider();
        final Iterable<Table> tables = readTables(newTableReader(databaseProvider));
        final RowCounter rowCounter = RowCounter.from(databaseProvider);

        ModelAndView list = new ModelAndView("list-tables", ImmutableMap.of(
                "tables", tablesPerPlugin(tables, rowCounter),
                "atl_token", xsrfTokenGenerator.generateToken(request)));

        // If present, display the success/error message
        if (session.getAttribute(AO_ADMIN_DELETE_TABLE_STATUS) != null)
        {
            Boolean success = (Boolean) session.getAttribute(AO_ADMIN_DELETE_TABLE_STATUS);
            List<?> tableNames = (List<?>) session.getAttribute(AO_ADMIN_DELETE_TABLE_LIST);
            String errorMessage = (String) session.getAttribute(AO_ADMIN_DELETE_TABLE_MESSAGE);

            session.removeAttribute(AO_ADMIN_DELETE_TABLE_STATUS);
            session.removeAttribute(AO_ADMIN_DELETE_TABLE_LIST);
            session.removeAttribute(AO_ADMIN_DELETE_TABLE_MESSAGE);

            if (success)
            {
                list.addObject("message", ImmutableMap.of(
                        "type", "success",
                        "title", i18n.getText("ao.admin.delete.messages.success", tableNames.size(), StringUtils.join(tableNames, ", "))));
            }
            else
            {
                list.addObject("message", ImmutableMap.of(
                        "type", "error",
                        "title", i18n.getText("ao.admin.delete.messages.error", StringUtils.join(tableNames, ", "), errorMessage)));
            }
        }

        return list;
    }

    @RequestMapping(value = "/tables/drop", method = RequestMethod.POST)
    public ModelAndView drop(final @RequestParam List<String> tableNames, HttpSession session) throws Exception
    {
        ActiveObjectsTableDropper dropper = new ActiveObjectsTableDropper(errorService, getDatabaseProvider(), nameConverters);
        Iterable<Table> tables = readTables(newTableReader(getDatabaseProvider()));
        Iterable<Table> tablesToDelete = Iterables.filter(tables, new Predicate<Table>()
        {
            @Override
            public boolean apply(Table candidate)
            {
                return tableNames.contains(candidate.getName());
            }
        });

        boolean success = true;
        String errorMessage = "";
        if (Iterables.size(tablesToDelete) == Iterables.size(tableNames))
        {

            DatabaseInformation databaseInformation = new DatabaseInformation(getDatabaseInformationReader().get());
            try
            {
                dropper.drop(databaseInformation, tablesToDelete, newEntityNameProcessor());
            }
            catch (ImportExportException exception)
            {
                logger.error("An error was encountered while trying to drop tables (" + StringUtils.join(tablesToDelete.iterator(), ", ") + ")", exception);
                success = false;
                errorMessage = exception.getCause() != null ? exception.getCause().getMessage() : "";
            }
        }
        else
        {
            List<String> inexistentTables = Lists.newArrayList(tableNames);
            for (Table table : tablesToDelete)
            {
                inexistentTables.remove(table.getName());
            }
            success = false;
            errorMessage = i18n.getText("ao.admin.delete.messages.error.inexistent", inexistentTables.size(), StringUtils.join(inexistentTables, ", "));
        }

        session.setAttribute(AO_ADMIN_DELETE_TABLE_STATUS, success);
        session.setAttribute(AO_ADMIN_DELETE_TABLE_LIST, tableNames);
        session.setAttribute(AO_ADMIN_DELETE_TABLE_MESSAGE, errorMessage);
        return new ModelAndView("redirect:list");
    }

    private Iterable<Table> readTables(TableReader tableReader)
    {
        return tableReader.read(emptyDatabaseInformation(), newEntityNameProcessor());
    }

    private ActiveObjectsBackup.UpperCaseEntityNameProcessor newEntityNameProcessor()
    {
        return new ActiveObjectsBackup.UpperCaseEntityNameProcessor();
    }

    private DatabaseInformation emptyDatabaseInformation()
    {
        return new DatabaseInformation(Maps.<String, String>newHashMap());
    }

    private ActiveObjectsTableReader newTableReader(DatabaseProvider databaseProvider)
    {
        return new ActiveObjectsTableReader(errorService, nameConverters, databaseProvider, ActiveObjectsBackup.schemaConfiguration());
    }

    private DatabaseProvider getDatabaseProvider()
    {
        return databaseProviderFactory.getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType(), dataSourceProvider.getSchema());
    }

    private DatabaseInformationReader getDatabaseInformationReader()
    {
        return new ConnectionProviderInformationReader(errorService, new ConnectionProvider()
        {
            @Override
            public Connection getConnection() throws SQLException
            {
                return getDatabaseProvider().getConnection();
            }
        });
    }

    private Multimap<PluginInformation, TableInformation> tablesPerPlugin(Iterable<Table> tables, final RowCounter rowCounter)
    {
        final Multimap<PluginInformation, TableInformation> tablesPerPlugin = HashMultimap.create();
        for (Table table : tables)
        {
            final String tableName = table.getName();
            tablesPerPlugin.put(newPluginInformation(tableName), newTableInformation(tableName, rowCounter));
        }
        return tablesPerPlugin;
    }

    private PluginInformation newPluginInformation(String tableName)
    {
        return pluginInformationFactory.getPluginInformation(tableName);
    }

    private TableInformation newTableInformation(String tableName, RowCounter rowCounter)
    {
        return new TableInformation(tableName, rowCounter.count(tableName));
    }

    public static final class TableInformation
    {
        private final String table;
        private final String rows;

        public TableInformation(String table, int rows)
        {
            this.table = checkNotNull(table);
            this.rows = String.valueOf(rows);
        }

        public String getTable()
        {
            return table;
        }

        public String getRows()
        {
            return rows;
        }
    }
}