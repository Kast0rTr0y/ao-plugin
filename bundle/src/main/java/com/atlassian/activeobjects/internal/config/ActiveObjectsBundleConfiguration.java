package com.atlassian.activeobjects.internal.config;

import net.java.ao.RawEntity;

import java.util.Set;

/**
 * This represents the configuration of active objects for a given bundle
 */
public interface ActiveObjectsBundleConfiguration
{
    Set<Class<? extends RawEntity<?>>> getEntities();
}
