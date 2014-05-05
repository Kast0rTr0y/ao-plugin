package com.atlassian.activeobjects.bamboo;

import com.atlassian.activeobjects.spi.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;

public class BambooExecutorServiceProvider implements ExecutorServiceProvider
{
    @Override
    public ExecutorService initExecutorService()
    {
        return null;
    }
}
