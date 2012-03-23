package com.atlassian.dbexporter.exporter;

import java.util.Map;

public interface DatabaseInformationReader
{
    /**
     * Gets the database information as a Map of properties
     *
     * @return a map of properties
     */
    Map<String, String> get();
}
