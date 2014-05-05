package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;

public class JiraExecutorServiceProvider implements ExecutorServiceProvider
{
    @Override
    public ExecutorService initExecutorService()
    {
        return null;
    }
}
