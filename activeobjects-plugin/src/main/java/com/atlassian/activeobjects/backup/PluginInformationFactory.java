package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public final class PluginInformationFactory
{
    private final PluginAccessor pluginAccessor;

    public PluginInformationFactory(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = checkNotNull(pluginAccessor);
    }

    /**
     * Gets plugin information from the Active Objects table hash
     *
     * @param hash the table hash for this plugin
     * @return some plugin information
     */
    PluginInformation getPluginInformation(final String hash)
    {
        if (hash == null)
        {
            return new NotAvailablePluginInformation(hash);
        }

        final ActiveObjectModuleDescriptor aomd = getModuleDescriptor(hash);
        return aomd != null ? new AvailablePluginInformation(hash, aomd.getPlugin()) : new NotAvailablePluginInformation(hash);
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

    private static class NotAvailablePluginInformation implements PluginInformation
    {
        private final String hash;

        public NotAvailablePluginInformation(String hash)
        {
            this.hash = checkNotNull(hash);
        }

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
        public String getHash()
        {
            return hash;
        }

        @Override
        public String toString()
        {
            return "<unknown plugin>";
        }
    }

    private class AvailablePluginInformation implements PluginInformation
    {
        private final String hash;
        private final Plugin plugin;

        public AvailablePluginInformation(String hash, Plugin plugin)
        {
            this.hash = checkNotNull(hash);
            this.plugin = checkNotNull(plugin);
        }

        @Override
        public boolean isAvailable()
        {
            return true;
        }

        @Override
        public String getPluginName()
        {
            return plugin.getName();
        }

        @Override
        public String getPluginKey()
        {
            return plugin.getKey();
        }

        @Override
        public String getPluginVersion()
        {
            return plugin.getPluginInformation().getVersion();
        }

        @Override
        public String getHash()
        {
            return hash;
        }

        @Override
        public String toString()
        {
            return "plugin " + getPluginName() + "(" + getPluginKey() + ") #" + getPluginVersion();
        }
    }
}