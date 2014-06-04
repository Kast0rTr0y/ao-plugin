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

import static com.google.common.base.Preconditions.*;

/**
 * Default implementation of {@link OsgiServiceUtils}
 */
public class OsgiServiceUtilsImpl implements OsgiServiceUtils
{
    private static final String PROPERTY_KEY = "com.atlassian.plugin.key";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public <S, O extends S> ServiceRegistration registerService(Bundle bundle, Class<S> ifce, O obj)
    {
        checkNotNull(bundle);
        checkNotNull(obj);
        final Map<String, String> properties = getProperties(bundle);

        logger.debug("Registering service {} with interface {} and properties {}", new Object[]{obj, ifce.getName(), properties});

        return getContext(bundle).registerService(ifce.getName(), obj, new Hashtable<String, String>(properties));
    }

    Map<String, String> getProperties(Bundle bundle)
    {
        final Map<String, String> props = new HashMap<String, String>();
        props.put(PROPERTY_KEY, bundle.getSymbolicName());
        return props;
    }

    private BundleContext getContext(Bundle bundle)
    {
        return bundle.getBundleContext();
    }

}
