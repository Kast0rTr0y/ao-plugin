package com.atlassian.activeobjects.osgi;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContextClassLoaderThreadFactory implements ThreadFactory
{
    private final ClassLoader contextClassLoader;

    public ContextClassLoaderThreadFactory(final ClassLoader contextClassLoader)
    {
        this.contextClassLoader = checkNotNull(contextClassLoader);
    }

    @Override
    public Thread newThread(final Runnable r)
    {
        Thread thread = Executors.defaultThreadFactory().newThread(r);

        thread.setContextClassLoader(contextClassLoader);

        return thread;
    }
}
