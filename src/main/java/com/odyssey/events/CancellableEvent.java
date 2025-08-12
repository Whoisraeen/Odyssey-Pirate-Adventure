package com.odyssey.events;

/**
 * Base class for events that can be cancelled.
 * When an event is cancelled, it should not proceed with its normal execution.
 */
public abstract class CancellableEvent extends Event {
    private boolean cancelled = false;
    
    /**
     * Checks if this event has been cancelled
     * @return true if the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Sets the cancelled state of this event
     * @param cancelled true to cancel the event, false to allow it
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Cancels this event
     */
    public void cancel() {
        setCancelled(true);
    }
    
    @Override
    public String toString() {
        return getEventName() + "{timestamp=" + getTimestamp() + ", cancelled=" + cancelled + "}";
    }
}