package com.atlassian.labs.activeobjects.external;

/**
 * Configuration settings for the ActiveObjects plugin.  Meant to be provided by the embedding product.
 */
public interface ActiveObjectsConfiguration {

    /**
     * @return the base directory for plugin databases, resolved from the product's home directory
     */
    String getDatabaseBaseDirectory();
}
