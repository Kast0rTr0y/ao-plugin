package com.atlassian.activeobjects.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

/**
 * A utility to register and get a service associated to a specific AO bundle
 */
public interface ActiveObjectOsgiServiceUtils<S>
{
    <O extends S> ServiceRegistration registerService(Bundle bundle, O obj);

    S getService(Bundle bundle) throws TooManyServicesFoundException, NoServicesFoundException;
}
