package com.atlassian.activeobjects.jira;

import com.atlassian.jira.configurator.config.DatabaseType;
import com.atlassian.jira.configurator.config.SettingsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@com.atlassian.activeobjects.jira.JiraDatabaseTypeExtractor} that loads the settings
 * using the {@link com.atlassian.jira.configurator.config.SettingsLoader}
 */
public class JiraDatabaseTypeExtractorImpl implements JiraDatabaseTypeExtractor
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DatabaseType getDatabaseType()
    {
        try
        {
            return SettingsLoader.loadCurrentSettings().getDatabaseType();
        }
        catch (Exception e)
        {
            logger.warn("Could not load JIRA settings, can't determine database type at this time.");
            logger.debug("This is the exception we got:", e);
            return null;
        }
    }
}
