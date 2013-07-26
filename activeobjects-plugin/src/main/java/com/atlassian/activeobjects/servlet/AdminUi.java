package com.atlassian.activeobjects.servlet;

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;

public final class AdminUi
{
    private static final Logger log = LoggerFactory.getLogger(AdminUi.class);

    private final Map<String, Object> essentials;

    public AdminUi(Map<String, Object> essentials)
    {
        this.essentials = checkNotNull(essentials);
    }

    boolean isEnabled()
    {
        final Map<String, Object> unavailable = filterEntries(essentials, new UnavailableServicePredicate());

        if (!unavailable.isEmpty())
        {
            log.debug("The admin UI is disabled because of the following services not being available:\n{}", unavailable.keySet());
        }
        return unavailable.isEmpty();
    }

    static Boolean isDevModeEnabled()
    {
        return Boolean.valueOf(System.getProperty("atlassian.dev.mode", Boolean.FALSE.toString()));
    }

    private static class UnavailableServicePredicate implements Predicate<Map.Entry<String, Object>>
    {
        @Override
        public boolean apply(Map.Entry<String, Object> entry)
        {
            try
            {
                entry.getValue().toString(); // try toString as it shouldn't have side effects
                return false;
            }
            catch (RuntimeException e)
            {
                if (e.getClass().getSimpleName().equals("ServiceUnavailableException"))
                {
                    if (isDevModeEnabled())
                    {
                        log.warn("Service is unavailable, admin UI will be disabled.", e);
                    }
                    return true;
                }
                throw e;
            }
        }
    }
}
