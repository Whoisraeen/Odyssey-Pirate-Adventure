package com.odyssey.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Input buffering system for frame-perfect input handling.
 * Stores input events with timestamps to allow for precise timing and combo detection.
 */
public class InputBuffer {
    private static final Logger logger = LoggerFactory.getLogger(InputBuffer.class);
    
    public static class InputEvent {
        public final InputAction action;
        public final boolean pressed;
        public final long timestamp;
        public final float value; // For analog inputs
        
        public InputEvent(InputAction action, boolean pressed, long timestamp) {
            this(action, pressed, timestamp, pressed ? 1.0f : 0.0f);
        }
        
        public InputEvent(InputAction action, boolean pressed, long timestamp, float value) {
            this.action = action;
            this.pressed = pressed;
            this.timestamp = timestamp;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return String.format("InputEvent{action=%s, pressed=%s, timestamp=%d, value=%.2f}", 
                               action, pressed, timestamp, value);
        }
    }
    
    public static class InputSequence {
        private final List<InputAction> sequence;
        private final long maxTimeBetweenInputs;
        private final String name;
        
        public InputSequence(String name, long maxTimeBetweenInputs, InputAction... actions) {
            this.name = name;
            this.maxTimeBetweenInputs = maxTimeBetweenInputs;
            this.sequence = Arrays.asList(actions);
        }
        
        public List<InputAction> getSequence() { return sequence; }
        public long getMaxTimeBetweenInputs() { return maxTimeBetweenInputs; }
        public String getName() { return name; }
    }
    
    private final Queue<InputEvent> inputBuffer = new ConcurrentLinkedQueue<>();
    private final Map<InputAction, InputEvent> lastInputState = new HashMap<>();
    private final List<InputSequence> registeredSequences = new ArrayList<>();
    private final Map<String, Long> lastSequenceDetection = new HashMap<>();
    
    // Buffer settings
    private final long bufferTimeMs;
    private final int maxBufferSize;
    private long lastCleanupTime = 0;
    private final long cleanupIntervalMs = 1000; // Clean up every second
    
    public InputBuffer(long bufferTimeMs, int maxBufferSize) {
        this.bufferTimeMs = bufferTimeMs;
        this.maxBufferSize = maxBufferSize;
    }
    
    /**
     * Add an input event to the buffer
     */
    public void addInputEvent(InputAction action, boolean pressed, float value) {
        long currentTime = System.currentTimeMillis();
        InputEvent event = new InputEvent(action, pressed, currentTime, value);
        
        inputBuffer.offer(event);
        lastInputState.put(action, event);
        
        // Limit buffer size
        while (inputBuffer.size() > maxBufferSize) {
            inputBuffer.poll();
        }
        
        // Check for input sequences
        checkInputSequences(event);
        
        // Periodic cleanup
        if (currentTime - lastCleanupTime > cleanupIntervalMs) {
            cleanup();
            lastCleanupTime = currentTime;
        }
    }
    
    /**
     * Add a simple press/release event
     */
    public void addInputEvent(InputAction action, boolean pressed) {
        addInputEvent(action, pressed, pressed ? 1.0f : 0.0f);
    }
    
    /**
     * Get the current state of an action
     */
    public boolean isActionActive(InputAction action) {
        InputEvent lastEvent = lastInputState.get(action);
        if (lastEvent == null) return false;
        
        long currentTime = System.currentTimeMillis();
        
        // Check if the event is still within the buffer time
        if (currentTime - lastEvent.timestamp > bufferTimeMs) {
            return false;
        }
        
        return lastEvent.pressed;
    }
    
    /**
     * Get the analog value of an action
     */
    public float getActionValue(InputAction action) {
        InputEvent lastEvent = lastInputState.get(action);
        if (lastEvent == null) return 0.0f;
        
        long currentTime = System.currentTimeMillis();
        
        // Check if the event is still within the buffer time
        if (currentTime - lastEvent.timestamp > bufferTimeMs) {
            return 0.0f;
        }
        
        return lastEvent.value;
    }
    
    /**
     * Check if an action was just pressed (within the buffer window)
     */
    public boolean wasActionJustPressed(InputAction action) {
        InputEvent lastEvent = lastInputState.get(action);
        if (lastEvent == null || !lastEvent.pressed) return false;
        
        long currentTime = System.currentTimeMillis();
        return currentTime - lastEvent.timestamp <= bufferTimeMs;
    }
    
    /**
     * Check if an action was just released (within the buffer window)
     */
    public boolean wasActionJustReleased(InputAction action) {
        // Look for recent release events in the buffer
        long currentTime = System.currentTimeMillis();
        
        return inputBuffer.stream()
                         .filter(event -> event.action == action && !event.pressed)
                         .anyMatch(event -> currentTime - event.timestamp <= bufferTimeMs);
    }
    
    /**
     * Register an input sequence for combo detection
     */
    public void registerSequence(InputSequence sequence) {
        registeredSequences.add(sequence);
        logger.info("Registered input sequence: {}", sequence.getName());
    }
    
    /**
     * Check if any registered sequences were triggered
     */
    private void checkInputSequences(InputEvent newEvent) {
        if (!newEvent.pressed) return; // Only check on press events
        
        long currentTime = newEvent.timestamp;
        
        for (InputSequence sequence : registeredSequences) {
            if (isSequenceTriggered(sequence, currentTime)) {
                onSequenceDetected(sequence, currentTime);
            }
        }
    }
    
    /**
     * Check if a specific sequence was triggered
     */
    private boolean isSequenceTriggered(InputSequence sequence, long currentTime) {
        List<InputAction> requiredActions = sequence.getSequence();
        if (requiredActions.isEmpty()) return false;
        
        // Find the most recent events for each action in the sequence
        List<InputEvent> sequenceEvents = new ArrayList<>();
        
        for (InputAction action : requiredActions) {
            InputEvent mostRecent = null;
            
            // Find the most recent press event for this action
            for (InputEvent event : inputBuffer) {
                if (event.action == action && event.pressed) {
                    if (mostRecent == null || event.timestamp > mostRecent.timestamp) {
                        mostRecent = event;
                    }
                }
            }
            
            if (mostRecent == null) return false; // Action not found
            sequenceEvents.add(mostRecent);
        }
        
        // Sort events by timestamp
        sequenceEvents.sort(Comparator.comparingLong(e -> e.timestamp));
        
        // Check if events are in the correct order and within time limits
        for (int i = 0; i < sequenceEvents.size(); i++) {
            InputEvent event = sequenceEvents.get(i);
            InputAction expectedAction = requiredActions.get(i);
            
            if (event.action != expectedAction) return false;
            
            // Check timing between consecutive events
            if (i > 0) {
                long timeDiff = event.timestamp - sequenceEvents.get(i - 1).timestamp;
                if (timeDiff > sequence.getMaxTimeBetweenInputs()) {
                    return false;
                }
            }
            
            // Check if the event is recent enough
            if (currentTime - event.timestamp > sequence.getMaxTimeBetweenInputs()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Called when a sequence is detected
     */
    private void onSequenceDetected(InputSequence sequence, long timestamp) {
        String sequenceName = sequence.getName();
        Long lastDetection = lastSequenceDetection.get(sequenceName);
        
        // Prevent rapid re-triggering of the same sequence
        if (lastDetection != null && timestamp - lastDetection < 500) {
            return;
        }
        
        lastSequenceDetection.put(sequenceName, timestamp);
        logger.info("Input sequence detected: {}", sequenceName);
        
        // Here you could trigger specific game events based on the sequence
        // For example, special moves, combos, etc.
    }
    
    /**
     * Get recent input events within the buffer time
     */
    public List<InputEvent> getRecentEvents() {
        long currentTime = System.currentTimeMillis();
        List<InputEvent> recentEvents = new ArrayList<>();
        
        for (InputEvent event : inputBuffer) {
            if (currentTime - event.timestamp <= bufferTimeMs) {
                recentEvents.add(event);
            }
        }
        
        return recentEvents;
    }
    
    /**
     * Get input timing for an action (useful for rhythm games or precise timing)
     */
    public long getTimeSinceLastInput(InputAction action) {
        InputEvent lastEvent = lastInputState.get(action);
        if (lastEvent == null) return Long.MAX_VALUE;
        
        return System.currentTimeMillis() - lastEvent.timestamp;
    }
    
    /**
     * Check if multiple actions are pressed simultaneously
     */
    public boolean areActionsPressed(InputAction... actions) {
        for (InputAction action : actions) {
            if (!isActionActive(action)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Clean up old events from the buffer
     */
    private void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Remove old events from buffer
        inputBuffer.removeIf(event -> currentTime - event.timestamp > bufferTimeMs);
        
        // Clean up old sequence detections
        lastSequenceDetection.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > bufferTimeMs * 2);
    }
    
    /**
     * Get the number of buffered events
     */
    public int getEventCount() {
        return inputBuffer.size();
    }
    
    /**
     * Clear all buffered input
     */
    public void clear() {
        inputBuffer.clear();
        lastInputState.clear();
        lastSequenceDetection.clear();
    }
    
    /**
     * Get buffer statistics
     */
    public String getBufferStats() {
        return String.format("InputBuffer{size=%d, maxSize=%d, bufferTime=%dms, sequences=%d}", 
                           inputBuffer.size(), maxBufferSize, bufferTimeMs, registeredSequences.size());
    }
}