package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import org.osgi.framework.Bundle;

import javax.annotation.Nonnull;

/**
 * Attempts to generate a "default" {@link ActiveObjectsConfiguration}
 */
public interface AOConfigurationGenerator
{
    /**
     * Attempt to generate a configuration for entities in the java package passed.
     *
     * Return null if there are no entities in that package.
     */
    ActiveObjectsConfiguration generateScannedConfiguration(@Nonnull Bundle bundle, @Nonnull String entityPackage);
}
