package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;

public class ConfluenceExecutorServiceProvider implements ExecutorServiceProvider
{
    @Override
    public ExecutorService initExecutorService()
    {
        throw new UnsupportedOperationException("@fabs please do the magic in here");
    }
}
