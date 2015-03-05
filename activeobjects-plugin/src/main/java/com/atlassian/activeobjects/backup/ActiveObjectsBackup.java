package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.osgi.ActiveObjectsServiceFactory;
import com.atlassian.activeobjects.spi.ActiveObjectsImportExportException;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.activeobjects.spi.ImportExportException;
import com.atlassian.dbexporter.api.BackupConfiguration;
import com.atlassian.dbexporter.api.ClearConfiguration;
import com.atlassian.dbexporter.api.GenericImportExportException;
import com.atlassian.dbexporter.api.RestoreConfiguration;
import com.atlassian.dbexporter.api.TableImportExportException;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import net.java.ao.sql.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsBackup implements Backup
{
    public static final Prefix PREFIX = new SimplePrefix("AO");

    private static final String NAMESPACE = "http://www.atlassian.com/ao";

    private final Supplier<DatabaseProvider> databaseProviderSupplier;
    private final com.atlassian.dbexporter.api.Backup delegate;
    private final PluginInformationFactory pluginInformationFactory;

    public ActiveObjectsBackup(final DatabaseProviderFactory databaseProviderFactory,
            final TenantAwareDataSourceProvider tenantAwareDataSourceProvider,
            final TenantContext tenantContext,
            PluginInformationFactory pluginInformationFactory,
            com.atlassian.dbexporter.api.Backup delegate)
    {
        this((Supplier<DatabaseProvider>) () -> {
            final Tenant tenant = tenantContext.getCurrentTenant();
            return checkNotNull(databaseProviderFactory).getDatabaseProvider(tenantAwareDataSourceProvider.getDataSource(tenant), tenantAwareDataSourceProvider.getDatabaseType(tenant), tenantAwareDataSourceProvider.getSchema(tenant));
        }, pluginInformationFactory, delegate);
    }

    ActiveObjectsBackup(DatabaseProvider databaseProvider,
            PluginInformationFactory pluginInformationFactory,
            com.atlassian.dbexporter.api.Backup delegate)
    {
        this(Suppliers.ofInstance(checkNotNull(databaseProvider)), pluginInformationFactory, delegate);
    }

    private ActiveObjectsBackup(Supplier<DatabaseProvider> databaseProviderSupplier,
            PluginInformationFactory pluginInformationFactory,
            com.atlassian.dbexporter.api.Backup delegate)
    {
        this.pluginInformationFactory = pluginInformationFactory;
        this.databaseProviderSupplier = checkNotNull(databaseProviderSupplier);
        this.delegate = delegate;
    }

    /**
     * Saves the backup to an output stream.
     *
     * @param stream the stream to write the backup to
     * @param monitor the progress monitor for the current backup
     * @throws ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link ImportExportException}.
     */
    public void save(OutputStream stream, BackupProgressMonitor monitor)
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        try (Connection connection = provider.getConnection())
        {
            delegate.save(connection, stream,
                    new BackupConfiguration.Builder()
                            .monitor(new BackupProgressMonitorAdaptor(monitor))
                            .namespace(NAMESPACE)
                            .tableFilter(tableName -> schemaConfiguration().shouldManageTable(tableName, true))
                            .build());
        }
        catch (GenericImportExportException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e.getCause());
        }
        catch (TableImportExportException e)
        {
            throw new ActiveObjectsImportExportException(e.getTableName(),
                    pluginInformationFactory.getPluginInformation(e.getTableName()), e.getCause());
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e);
        }
    }

    public static SchemaConfiguration schemaConfiguration()
    {
        return new PrefixedSchemaConfiguration(PREFIX);
    }

    /**
     * Restores the backup coming from the given input stream.
     *
     * @param stream the stream of data previously backed up by the plugin.
     * @param monitor the progress monitor for the current restore
     * @throws ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link ImportExportException}.
     */
    public void restore(InputStream stream, RestoreProgressMonitor monitor)
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        try (Connection connection = provider.getConnection())
        {
            delegate.restore(stream, connection,
                    new RestoreConfiguration.Builder()
                            .monitor(new RestoreProgressMonitorAdaptor(monitor))
                            .build());
        }
        catch (GenericImportExportException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e.getCause());
        }
        catch (TableImportExportException e)
        {
            throw new ActiveObjectsImportExportException(e.getTableName(),
                    pluginInformationFactory.getPluginInformation(e.getTableName()), e.getCause());
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e);
        }
    }

    @Override
    public void clear()
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        try (Connection connection = provider.getConnection())
        {
            delegate.clear(connection,
                    new ClearConfiguration.Builder()
                            .tableFilter(tableName -> schemaConfiguration().shouldManageTable(tableName, true))
                            .build());
        }
        catch (GenericImportExportException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e.getCause());
        }
        catch (TableImportExportException e)
        {
            throw new ActiveObjectsImportExportException(e.getTableName(),
                    pluginInformationFactory.getPluginInformation(e.getTableName()), e.getCause());
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsImportExportException(null, null, e);
        }
    }
}
