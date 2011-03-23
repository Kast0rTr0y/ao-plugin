package com.atlassian.activeobjects.web.admin;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import net.java.ao.RawEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

/**
 *
 */
public class ConfigurationController
{
    private static final String VIEW = "view-config";

    /**
     * This is the configuration of the plugin itself
     */
    private final ActiveObjectsPluginConfiguration pluginConfig;

    /**
     * This is the list of configuration of the 'mutliple' active objects capacble plugins
     */
    private final Collection<ActiveObjectsConfiguration> configurations;

    private final PluginAccessor pluginAccessor;

    public ConfigurationController(ActiveObjectsPluginConfiguration pluginConfiguration, Collection<ActiveObjectsConfiguration> configurations, PluginAccessor pluginAccessor)
    {
        this.pluginConfig = checkNotNull(pluginConfiguration);
        this.configurations = checkNotNull(configurations);
        this.pluginAccessor = checkNotNull(pluginAccessor);
    }

    public ModelAndView view(HttpServletRequest request, HttpServletResponse response)
    {

        return new ModelAndView(VIEW, getData());
    }

    private Map<String, Object> getData()
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("pluginConfig", pluginConfig);
        model.put("configurations", transform(configurations));
        return model;
    }

    private Collection<PluginConfiguration> transform(Collection<ActiveObjectsConfiguration> configurations)
    {
        return Collections2.transform(configurations, new Function<ActiveObjectsConfiguration, PluginConfiguration>()
        {
            public PluginConfiguration apply(ActiveObjectsConfiguration from)
            {
                return toPluginConfiguration(from);
            }
        });
    }

    private PluginConfiguration toPluginConfiguration(ActiveObjectsConfiguration config)
    {
        final PluginConfiguration c = new PluginConfiguration();
        c.setPlugin(getPlugin(config));
        c.setEntities(getEntities(config));
        return c;
    }

    private Map<String, String> getEntities(final ActiveObjectsConfiguration config)
    {
        final ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        for (Class<? extends RawEntity<?>> entityClass : config.getEntities())
        {
            map.put(entityClass.getName(), config.getTableNameConverter().getName(entityClass));
        }
        return map.build();
    }

    private Plugin getPlugin(ActiveObjectsConfiguration config)
    {
        return pluginAccessor.getPlugin(config.getPluginKey().toString());
    }
}
