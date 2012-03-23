package com.atlassian.activeobjects.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

/**
 * A utility to register and get a service associated to a specific AO bundle
 */
public interface OsgiServiceUtils
{
    <S, O extends S> ServiceRegistration registerService(Bundle bundle, Class<S> ifce, O obj);

    <S> S getService(Bundle bundle, Class<S> ifce) throws TooManyServicesFoundException, NoServicesFoundException;
}
