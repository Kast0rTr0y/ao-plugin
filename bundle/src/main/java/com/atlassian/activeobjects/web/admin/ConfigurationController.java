package com.atlassian.activeobjects.web.admin;

import com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class ConfigurationController
{
    private static final String VIEW = "view-config";

    private final ActiveObjectsPluginConfiguration pluginConfig;

    public ConfigurationController(ActiveObjectsPluginConfiguration pluginConfiguration)
    {
        this.pluginConfig = checkNotNull(pluginConfiguration);
    }

    public ModelAndView view(HttpServletRequest request, HttpServletResponse response)
    {
        return new ModelAndView(VIEW, getReferenceData());
    }

    private Map<String, Object> getReferenceData()
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("pluginConfig", pluginConfig);
        return model;
    }
}
