package com.atlassian.activeobjects.spring;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.osgi.context.BundleContextAware;

import static com.google.common.base.Preconditions.*;

/**
 * A factory bean to expose the current OSGi bundle in a spring context
 */
public final class BundleFactoryBean extends AbstractFactoryBean implements BundleContextAware
{
    private BundleContext bundleContext;

    @Override
    public Class getObjectType()
    {
        return Bundle.class;
    }

    @Override
    protected Object createInstance() throws Exception
    {
        return bundleContext.getBundle();
    }

    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = checkNotNull(bundleContext);
    }
}
