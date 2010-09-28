package com.atlassian.activeobjects.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public final class TransactionalAnnotationProcessor implements BeanPostProcessor
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        logger.info("Post processing bean {} named {} before init", o, s);
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        logger.info("Post processing bean {} named {} after init", o, s);
        return o;
    }
}
