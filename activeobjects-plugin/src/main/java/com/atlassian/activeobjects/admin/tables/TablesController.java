package com.atlassian.activeobjects.admin.tables;

import com.atlassian.activeobjects.backup.ActiveObjectsBackup;
import com.atlassian.activeobjects.backup.ActiveObjectsTableReader;
import com.atlassian.activeobjects.backup.ImportExportErrorServiceImpl;
import com.atlassian.activeobjects.backup.PluginInformationFactory;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.exporter.TableReader;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TablesController {
    private final DatabaseProviderFactory databaseProviderFactory;
    private final NameConverters nameConverters;
    private final TenantAwareDataSourceProvider tenantAwareDataSourceProvider;
    private final ImportExportErrorServiceImpl errorService;
    private final PluginInformationFactory pluginInformationFactory;
    private final TenantContext tenantContext;

    public TablesController(DatabaseProviderFactory databaseProviderFactory, NameConverters nameConverters, TenantAwareDataSourceProvider tenantAwareDataSourceProvider, ImportExportErrorServiceImpl errorService, PluginInformationFactory pluginInformationFactory, TenantContext tenantContext) {
        this.pluginInformationFactory = checkNotNull(pluginInformationFactory);
        this.nameConverters = checkNotNull(nameConverters);
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.tenantAwareDataSourceProvider = checkNotNull(tenantAwareDataSourceProvider);
        this.errorService = checkNotNull(errorService);
        this.tenantContext = checkNotNull(tenantContext);
    }

    public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Tenant tenant = tenantContext.getCurrentTenant();
        final DatabaseProvider databaseProvider = getDatabaseProvider(tenant);
        final Iterable<Table> tables = readTables(newTableReader(databaseProvider));
        final RowCounter rowCounter = RowCounter.from(databaseProvider);

        return new ModelAndView("list-tables", "tables", tablesPerPlugin(tables, rowCounter));
    }

    private Iterable<Table> readTables(TableReader tableReader) {
        return tableReader.read(emptyDatabaseInformation(), newEntityNameProcessor());
    }

    private ActiveObjectsBackup.UpperCaseEntityNameProcessor newEntityNameProcessor() {
        return new ActiveObjectsBackup.UpperCaseEntityNameProcessor();
    }

    private DatabaseInformation emptyDatabaseInformation() {
        return new DatabaseInformation(Maps.<String, String>newHashMap());
    }

    private ActiveObjectsTableReader newTableReader(DatabaseProvider databaseProvider) {
        return new ActiveObjectsTableReader(errorService, nameConverters, databaseProvider, ActiveObjectsBackup.schemaConfiguration());
    }

    private DatabaseProvider getDatabaseProvider(Tenant tenant) {
        return databaseProviderFactory.getDatabaseProvider(tenantAwareDataSourceProvider.getDataSource(tenant), tenantAwareDataSourceProvider.getDatabaseType(tenant), tenantAwareDataSourceProvider.getSchema(tenant));
    }

    private Multimap<PluginInformation, TableInformation> tablesPerPlugin(Iterable<Table> tables, final RowCounter rowCounter) {
        final Multimap<PluginInformation, TableInformation> tablesPerPlugin = HashMultimap.create();
        for (Table table : tables) {
            final String tableName = table.getName();
            tablesPerPlugin.put(newPluginInformation(tableName), newTableInformation(tableName, rowCounter));
        }
        return tablesPerPlugin;
    }

    private PluginInformation newPluginInformation(String tableName) {
        return pluginInformationFactory.getPluginInformation(tableName);
    }

    private TableInformation newTableInformation(String tableName, RowCounter rowCounter) {
        return new TableInformation(tableName, rowCounter.count(tableName));
    }

    public static final class TableInformation {
        private final String table;
        private final String rows;

        public TableInformation(String table, int rows) {
            this.table = checkNotNull(table);
            this.rows = String.valueOf(rows);
        }

        public String getTable() {
            return table;
        }

        public String getRows() {
            return rows;
        }
    }
}