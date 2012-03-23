package com.atlassian.activeobjects.spi;

public interface PluginInformation
{
    /**
     * If the plugin information is available then the following methods will return non-{@code null} values.
     * <ul>
     * <li>{@link #getPluginName()}</li>
     * <li>{@link #getPluginKey()}</li>
     * <li>{@link #getPluginVersion()}</li>
     * </ul>
     *
     * @return whether or not this is plugin information is actually available.
     */
    boolean isAvailable();

    String getPluginName();

    String getPluginKey();

    String getPluginVersion();

    String getVendorName();

    String getVendorUrl();
}