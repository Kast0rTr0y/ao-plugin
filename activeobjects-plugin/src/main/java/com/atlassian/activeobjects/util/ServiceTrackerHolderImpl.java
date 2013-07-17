package com.atlassian.activeobjects.util;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;


public class ServiceTrackerHolderImpl implements ServiceTrackerHolder, InitializingBean
{
    static final String DELEGATEE_CLASS_NAME = ActiveObjectsConfiguration.class.getName();

    private final BundleContext bundleContext;

    private ServiceTracker serviceTracker;

    public ServiceTrackerHolderImpl(@Nonnull final BundleContext bundleContext)
    {
        this.bundleContext = checkNotNull(bundleContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        serviceTracker = new ServiceTracker(bundleContext, DELEGATEE_CLASS_NAME, null);
    }

    @Override
    public ServiceTracker getServiceTracker()
    {
        return serviceTracker;
    }
}
