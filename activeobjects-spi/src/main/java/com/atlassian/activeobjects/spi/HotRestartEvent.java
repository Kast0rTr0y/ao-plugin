package com.atlassian.activeobjects.spi;

/**
 * <p>This is the event that products can publish via their own EventPublisher to tell AO
 * to hot restart itself.</p>
 * <p>The effect of this will mainly be that AO will reconnect to the database and run the (idempotent) upgrade tasks
 * again.</p>
 */
public final class HotRestartEvent {
    public static final HotRestartEvent INSTANCE = new HotRestartEvent();

    private HotRestartEvent() {
    }
}
