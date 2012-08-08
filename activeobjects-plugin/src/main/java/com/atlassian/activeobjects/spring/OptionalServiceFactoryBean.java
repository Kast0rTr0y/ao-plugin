package com.atlassian.activeobjects.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.osgi.service.ServiceUnavailableException;

import static com.google.common.base.Preconditions.*;

public final class OptionalServiceFactoryBean<T> implements FactoryBean
{
    private final T service;
    private final T defaultValue;
    private final Class<T> type;

    public OptionalServiceFactoryBean(Class<T> type, T service, T defaultValue)
    {
        this.type = checkNotNull(type);
        this.service = checkNotNull(service);
        this.defaultValue = checkNotNull(defaultValue);
    }

    @Override
    public Object getObject() throws Exception
    {
        try
        {
            service.toString();
            return service;
        }
        catch (ServiceUnavailableException e)
        {
            return defaultValue;
        }
    }

    @Override
    public Class getObjectType()
    {
        return type;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
