package com.atlassian.activeobjects.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * Default implementation of {@link ActiveObjectOsgiServiceUtils}
 */
public class ActiveObjectOsgiServiceUtilsImpl<S> implements ActiveObjectOsgiServiceUtils<S>
{
    private static final String BUNDLE_SYMBOLIC_NAME = "com.atlassian.activeobjects.bundleSymbolicName";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Class<S> serviceInterface;

    public ActiveObjectOsgiServiceUtilsImpl(Class<S> serviceInterface)
    {
        this.serviceInterface = checkNotNull(serviceInterface);
    }

    public <O extends S> ServiceRegistration registerService(Bundle bundle, O obj)
    {
        checkNotNull(bundle);
        checkNotNull(obj);
        final Map<String, String> properties = getProperties(bundle);

        logger.debug("Registering service {} with interface {} and properties {}", new Object[]{obj, serviceInterface.getName(), properties});

        return getContext(bundle).registerService(serviceInterface.getName(), obj, new Hashtable<String, String>(properties));
    }

    public S getService(Bundle bundle) throws TooManyServicesFoundException, NoServicesFoundException
    {
        checkNotNull(bundle);

        return serviceInterface.cast(getContext(bundle).getService(getServiceReference(bundle)));
    }

    private ServiceReference getServiceReference(Bundle bundle) throws NoServicesFoundException, TooManyServicesFoundException
    {
        final String filter = getFilter(getProperties(bundle));
        final ServiceReference[] serviceReferences = getServiceReferences(bundle, filter);
        if (serviceReferences == null || serviceReferences.length == 0)
        {
            throw new NoServicesFoundException("Was expecting at least one service reference for interface <"
                    + serviceInterface.getName() + "> and filter <" + filter + ">. Got "
                    + (serviceReferences == null ? null : serviceReferences.length) + " !");
        }
        else if (serviceReferences.length > 1)
        {
            throw new TooManyServicesFoundException("Was expecting at mone one service reference for interface <"
                    + serviceInterface.getName() + "> and filter <" + filter + ">. Got " + serviceReferences.length + " !");
        }
        else
        {
            return serviceReferences[0];
        }
    }

    ServiceReference[] getServiceReferences(Bundle bundle, String filter)
    {
        try
        {
            return getContext(bundle).getServiceReferences(serviceInterface.getName(), filter);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalStateException("There was a syntax issue getting service reference for interface <"
                    + serviceInterface.getName() + "> and filter <" + filter + ">.\nHow is that possible ?!", e);
        }
    }

    Map<String, String> getProperties(Bundle bundle)
    {
        final Map<String, String> props = new HashMap<String, String>();
        props.put(BUNDLE_SYMBOLIC_NAME, bundle.getSymbolicName());
        return props;
    }

    String getFilter(Map<String, String> properties)
    {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet())
        {
            sb.append("(").append(entry.getKey()).append("=").append(entry.getValue()).append(")");
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1); // remove the last '&'
        return sb.toString();
    }

    private BundleContext getContext(Bundle bundle)
    {
        return bundle.getBundleContext();
    }
}
