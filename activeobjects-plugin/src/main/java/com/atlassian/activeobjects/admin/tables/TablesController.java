package com.atlassian.activeobjects.admin.tables;

import com.atlassian.activeobjects.backup.ActiveObjectsBackup;
import com.atlassian.activeobjects.backup.ActiveObjectsHashesReader;
import com.atlassian.activeobjects.backup.ActiveObjectsTableReader;
import com.atlassian.activeobjects.backup.ImportExportErrorServiceImpl;
import com.atlassian.activeobjects.backup.PluginInformationFactory;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.Table;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import net.java.ao.DatabaseProvider;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newArrayList;

public final class TablesController
{
    private final DatabaseProviderFactory databaseProviderFactory;
    private final DataSourceProvider dataSourceProvider;
    private final ImportExportErrorServiceImpl errorService;
    private final ActiveObjectsHashesReader hashesReader;
    private final PluginInformationFactory pluginInformationFactory;
    private final I18nResolver i18nResolver;

    public TablesController(DatabaseProviderFactory databaseProviderFactory, DataSourceProvider dataSourceProvider, ImportExportErrorServiceImpl errorService, I18nResolver i18nResolver, ActiveObjectsHashesReader hashesReader, PluginInformationFactory pluginInformationFactory)
    {
        this.hashesReader = hashesReader;
        this.pluginInformationFactory = pluginInformationFactory;
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.errorService = checkNotNull(errorService);
        this.i18nResolver = checkNotNull(i18nResolver);
    }

    public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        final DatabaseProvider databaseProvider = databaseProviderFactory.getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType(), dataSourceProvider.getSchema());
        final Iterable<Table> tables = new ActiveObjectsTableReader(errorService, databaseProvider, ActiveObjectsBackup.schemaConfiguration()).read(new DatabaseInformation(Maps.<String, String>newHashMap()), new ActiveObjectsBackup.UpperCaseEntityNameProcessor());
        final RowCounter from = RowCounter.from(databaseProvider);
        return new ModelAndView("list-tables", "tables", newArrayList(Iterables.transform(tables, new Function<Table, TableInformation>()
        {
            @Override
            public TableInformation apply(Table t)
            {
                final PluginInformation pluginInformation = pluginInformationFactory.getPluginInformation(hashesReader.getHash(t.getName()));
                final int rowCount = from.count(t.getName());
                return new TableInformation(
                        pluginInformation.isAvailable() ? pluginInformation.getPluginName() : i18nResolver.getText("ao.admin.plugin.unknown"),
                        t.getName(),
                        rowCount != -1 ? String.valueOf(rowCount) : i18nResolver.getText("ao.admin.rows.unknown"));
            }
        })));
    }

    public static final class TableInformation
    {
        private final String plugin;
        private final String table;
        private final String rows;

        public TableInformation(String plugin, String table, String rows)
        {
            this.plugin = Preconditions.checkNotNull(plugin);
            this.table = Preconditions.checkNotNull(table);
            this.rows = Preconditions.checkNotNull(rows);
        }

        public String getPlugin()
        {
            return plugin;
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