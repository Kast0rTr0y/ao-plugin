package com.atlassian.activeobjects.util;

import org.osgi.util.tracker.ServiceTracker;

public interface ServiceTrackerHolder
{
    ServiceTracker getServiceTracker();
}
