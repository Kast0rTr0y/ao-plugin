package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.sal.api.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class DataSourceActiveObjectsFactory implements ActiveObjectsFactory
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String pluginKey;
    private final DataSourceProvider dataSourceProvider;

    public DataSourceActiveObjectsFactory(String pluginKey, DataSourceProvider dataSourceProvider)
    {
        this.pluginKey = checkNotNull(pluginKey);
        this.dataSourceProvider = dataSourceProvider;
    }

    public ActiveObjects create()
    {
        try {
            return new DataSourceActiveObjects(dataSourceProvider, pluginKey);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
