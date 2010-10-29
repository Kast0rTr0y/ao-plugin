package com.atlassian.activeobjects.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class IntegrationTestHelper
{
    private static final String PLUGIN_JAR_PATH_PROPERTY_NAME = "plugin.jar";

    private IntegrationTestHelper()
    {
    }

    public static File getTmpDir(final String prefix)
    {
        return mkdirs(deleteFile(createTmpFile(prefix)));
    }

    public static File getDir(File parentDir, String name)
    {
        return mkdirs(new File(parentDir, name));
    }

    /**
     * The build directory, we can add stuff to this one.
     *
     * @return the build dir, assuming maven and as such './target'
     */
    private static File getBuildDir()
    {
        return new File("target");
    }

    private static File createTmpFile(String prefix)
    {
        try
        {
            return File.createTempFile(prefix, "", getBuildDir());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static File mkdirs(final File dir)
    {
        if (!dir.mkdirs())
        {
            throw new IntegrationTestException("Could not create directory <" + dir.getAbsolutePath() + "> for tests");
        }
        return dir;
    }

    public static void deleteDirectory(File dir)
    {
        try
        {
            FileUtils.deleteDirectory(dir);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static File deleteFile(File file)
    {
        if (!file.delete())
        {
            throw new RuntimeException("Could not delete file <" + file.getAbsolutePath() + ">");
        }
        return file;
    }


    /**
     * Gets the path to the plugin Jar, this is one we just built before running the integration tests
     *
     * @return the path to the plugin jar
     * @throws IntegrationTestException if it could not be found.
     */
    public static String getPluginJarPath()
    {
        final String path = System.getProperty(PLUGIN_JAR_PATH_PROPERTY_NAME);

        final StringBuilder errorMsg = new StringBuilder(180)
                .append("\n")
                .append("Could not find plugin jar path. System property '")
                .append(PLUGIN_JAR_PATH_PROPERTY_NAME).append("' is not set.\n")
                .append("If you're NOT running your tests using maven, make sure to set the system property in your (IDE) test configuration.");

        return checkNotNull(path, errorMsg);
    }

    /**
     * Gets the the file pointing at the plugin jar
     *
     * @return the File designating the plugin jar
     * @see #getPluginJarPath()
     */
    public static File getPluginJar()
    {
        return new File(getPluginJarPath());
    }

    private static <T> T checkNotNull(T t, CharSequence message)
    {
        if (t == null)
        {
            throw new IntegrationTestException(message.toString());
        }
        return t;
    }
}
