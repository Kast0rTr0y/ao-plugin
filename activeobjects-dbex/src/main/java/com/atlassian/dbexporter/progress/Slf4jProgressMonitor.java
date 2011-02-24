package com.atlassian.dbexporter.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

public final class Slf4jProgressMonitor implements ProgressMonitor
{
    private final Logger logger;

    public Slf4jProgressMonitor()
    {
        this(LoggerFactory.getLogger(ProgressMonitor.class));
    }

    public Slf4jProgressMonitor(Logger logger)
    {
        this.logger = checkNotNull(logger);
    }

    public void update(Message message)
    {
        if (message instanceof Warning)
        {
            logger.warn("{}", message);
        }
        else
        {
            logger.info("{}", message);
        }
    }
}
