package com.atlassian.activeobjects.refapp;

import com.atlassian.activeobjects.spi.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;

public class RefappExecutorServiceProvider implements ExecutorServiceProvider
{
    @Override
    public ExecutorService initExecutorService()
    {
        return null;
    }
}
