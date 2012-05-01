package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;

import java.util.Collection;

import static com.google.common.base.Preconditions.*;

public final class PluginInformationFactory
{
    private final PluginToTablesMapping pluginToTablesMapping;
    private final ActiveObjectsHashesReader hashesReader;
    private final PluginAccessor pluginAccessor;

    public PluginInformationFactory(PluginToTablesMapping pluginToTablesMapping, ActiveObjectsHashesReader hashesReader, PluginAccessor pluginAccessor)
    {
        this.hashesReader = checkNotNull(hashesReader);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.pluginAccessor = checkNotNull(pluginAccessor);
    }

    /**
     * Gets plugin information from the Active Objects table hash
     *
     * @param tableName the table name
     * @return some plugin information
     */
    public PluginInformation getPluginInformation(final String tableName)
    {
        if (tableName == null)
        {
            return new NotAvailablePluginInformation();
        }

        final PluginToTablesMapping.PluginInfo pluginInfo = pluginToTablesMapping.get(tableName);
        if (pluginInfo != null)
        {
            return new AvailablePluginInformation(pluginInfo);
        }

        final ActiveObjectModuleDescriptor aomd = getModuleDescriptor(hashesReader.getHash(tableName));
        if (aomd != null)
        {
            return new AvailablePluginInformation(aomd.getPlugin());
        }

        return new NotAvailablePluginInformation();
    }

    private ActiveObjectModuleDescriptor getModuleDescriptor(String hash)
    {
        final Collection<ModuleDescriptor<Object>> moduleDescriptors = findModuleDescriptors(hash);
        return moduleDescriptors.isEmpty() ? null : (ActiveObjectModuleDescriptor) moduleDescriptors.iterator().next();
    }

    private Collection<ModuleDescriptor<Object>> findModuleDescriptors(final String hash)
    {
        return pluginAccessor.getModuleDescriptors(new ModuleDescriptorPredicate<Object>()
        {
            @Override
            public boolean matches(ModuleDescriptor<? extends Object> moduleDescriptor)
            {
                return moduleDescriptor instanceof ActiveObjectModuleDescriptor
                        && ((ActiveObjectModuleDescriptor) moduleDescriptor).getHash().equalsIgnoreCase(hash);
            }
        });
    }

    private static final class NotAvailablePluginInformation implements PluginInformation
    {
        @Override
        public boolean isAvailable()
        {
            return false;
        }

        @Override
        public String getPluginName()
        {
            return null;
        }

        @Override
        public String getPluginKey()
        {
            return null;
        }

        @Override
        public String getPluginVersion()
        {
            return null;
        }

        @Override
        public String getVendorName()
        {
            return null;
        }

        @Override
        public String getVendorUrl()
        {
            return null;
        }

        @Override
        public boolean equals(Object o)
        {
            return o != null && o instanceof NotAvailablePluginInformation;
        }

        @Override
        public int hashCode()
        {
            return 0;
        }

        @Override
        public String toString()
        {
            return "<unknown plugin>";
        }
    }

    private static final class AvailablePluginInformation implements PluginInformation
    {
        private final String name;
        private final String key;
        private final String version;
        private final String vendorName;
        private final String vendorUrl;

        public AvailablePluginInformation(Plugin plugin)
        {
            this(
                    checkNotNull(plugin).getName(),
                    plugin.getKey(),
                    plugin.getPluginInformation().getVersion(),
                    plugin.getPluginInformation().getVendorName(),
                    plugin.getPluginInformation().getVendorUrl()
            );
        }

        public AvailablePluginInformation(PluginToTablesMapping.PluginInfo pluginInfo)
        {
            this(checkNotNull(pluginInfo).name, pluginInfo.key, pluginInfo.version, pluginInfo.vendorName, pluginInfo.vendorUrl);
        }

        private AvailablePluginInformation(String name, String key, String version, String vendorName, String vendorUrl)
        {
            this.name = name;
            this.key = key;
            this.version = version;
            this.vendorName = vendorName;
            this.vendorUrl = vendorUrl;
        }

        @Override
        public boolean isAvailable()
        {
            return true;
        }

        @Override
        public String getPluginName()
        {
            return name;
        }

        @Override
        public String getPluginKey()
        {
            return key;
        }

        @Override
        public String getPluginVersion()
        {
            return version;
        }

        @Override
        public String getVendorName()
        {
            return vendorName;
        }

        @Override
        public String getVendorUrl()
        {
            return vendorUrl;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final AvailablePluginInformation that = (AvailablePluginInformation) o;

            if (key != null ? !key.equals(that.key) : that.key != null)
            {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null)
            {
                return false;
            }
            if (vendorName != null ? !vendorName.equals(that.vendorName) : that.vendorName != null)
            {
                return false;
            }
            if (vendorUrl != null ? !vendorUrl.equals(that.vendorUrl) : that.vendorUrl != null)
            {
                return false;
            }
            if (version != null ? !version.equals(that.version) : that.version != null)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = name != null ? name.hashCode() : 1;
            result = 31 * result + (key != null ? key.hashCode() : 1);
            result = 31 * result + (version != null ? version.hashCode() : 1);
            result = 31 * result + (vendorName != null ? vendorName.hashCode() : 1);
            result = 31 * result + (vendorUrl != null ? vendorUrl.hashCode() : 1);
            return result;
        }

        @Override
        public String toString()
        {
            return "plugin " + getPluginName() + "(" + getPluginKey() + ") #" + getPluginVersion();
        }
    }
}