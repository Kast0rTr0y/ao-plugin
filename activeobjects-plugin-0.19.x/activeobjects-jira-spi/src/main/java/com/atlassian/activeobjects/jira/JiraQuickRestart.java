package com.atlassian.activeobjects.jira;

import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.ClearCacheEvent;

import static com.google.common.base.Preconditions.*;

/**
 * This is a listener for the {@link ClearCacheEvent} fired by JIRA, on this event the AO plugin should hot restart, i.e.
 * re-start without re-starting the whole plugin.
 */
public final class JiraQuickRestart
{
    private final EventPublisher eventPublisher;

    public JiraQuickRestart(EventPublisher eventPublisher)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.eventPublisher.register(this);
    }

    @EventListener
    public void onClearCacheEvent(ClearCacheEvent cce)
    {
        eventPublisher.publish(HotRestartEvent.INSTANCE);
    }
}
