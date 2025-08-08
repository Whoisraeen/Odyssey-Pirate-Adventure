package com.odyssey.ui;

/**
 * Interface for handling UI events.
 */
@FunctionalInterface
public interface UIEventListener {
    /**
     * Called when a UI event occurs.
     * @param event the UI event
     */
    void onEvent(UIEvent event);
}