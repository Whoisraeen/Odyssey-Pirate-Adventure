package com.odyssey.events;

/**
 * Interface for events that can be cancelled.
 */
public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}