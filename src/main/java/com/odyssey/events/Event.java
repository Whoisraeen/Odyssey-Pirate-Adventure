package com.odyssey.events;

/**
 * Base class for all events in the game.
 * Events are used to communicate between different systems in a decoupled way.
 */
public abstract class Event {
    private final long timestamp;
    
    public Event() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the timestamp when this event was created
     * @return timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the name of this event type
     * @return simple class name
     */
    public String getEventName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public String toString() {
        return getEventName() + "{timestamp=" + timestamp + "}";
    }
}