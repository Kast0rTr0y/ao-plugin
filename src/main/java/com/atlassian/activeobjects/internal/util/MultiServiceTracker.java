package com.atlassian.activeobjects.internal.util;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 *
 */
public class MultiServiceTracker
{
    private final MultiServiceTrackerCustomizer customizer;

    public static interface MultiServiceTrackerCustomizer
    {
        void refresh(Map<Class<?>, Object> dependencies);
        void unregister();
    }
    private final Logger log = LoggerFactory.getLogger(MultiServiceTracker.class);
    private final List<ServiceTracker> depTrackers;
    private final Map<Class<?>, Object> deps = new LinkedHashMap<Class<?>, Object>();

    private final Collection<Class<?>> requiredDependencies;

    public MultiServiceTracker(final MultiServiceTrackerCustomizer customizer, final BundleContext ctx,
                               final Collection<Class<?>> requiredDependencies, final Collection<Class<?>> optionalDependencies)
    {
        this.customizer = customizer;
        this.requiredDependencies = requiredDependencies;
        depTrackers = new ArrayList<ServiceTracker>();
        Set<Class<?>> allDeps = new HashSet<Class<?>>(requiredDependencies);
        allDeps.addAll(optionalDependencies);
        for (final Class cls : allDeps)
        {
            ServiceTracker st = new ServiceTracker(ctx, cls.getName(), new ServiceTrackerCustomizer()
            {
                public Object addingService(ServiceReference serviceReference) {
                    Object svc = ctx.getService(serviceReference);
                    updateDependency(cls, svc);
                    return svc;
                }

                public void modifiedService(ServiceReference serviceReference, Object o) {
                    updateDependency(cls, o);
                }

                public void removedService(ServiceReference serviceReference, Object o) {
                    removeDependency(cls);
                }
            });
            st.open();
            depTrackers.add(st);
        }
    }

    private void removeDependency(Class cls) {
        deps.remove(cls);
        customizer.unregister();
    }

    private void updateDependency(Class cls, Object svc) {
        deps.put(cls, svc);
        if (deps.keySet().containsAll(requiredDependencies))
        {
            customizer.refresh(deps);
        }
    }

    public void close()
    {
        for (ServiceTracker l : depTrackers)
        {
            l.close();
        }
        depTrackers.clear();
        customizer.unregister();
    }
}
