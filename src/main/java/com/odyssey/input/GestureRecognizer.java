package com.odyssey.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Gesture recognition system for touch input and mobile/tablet support.
 * Recognizes common gestures like tap, double-tap, long press, swipe, and pinch.
 */
public class GestureRecognizer {
    private static final Logger logger = LoggerFactory.getLogger(GestureRecognizer.class);
    
    public static class TouchPoint {
        public final int id;
        public final float x, y;
        public final long timestamp;
        public final float pressure;
        
        public TouchPoint(int id, float x, float y, long timestamp, float pressure) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
            this.pressure = pressure;
        }
        
        public float distanceTo(TouchPoint other) {
            float dx = x - other.x;
            float dy = y - other.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }
    
    public static class Gesture {
        public final InputAction action;
        public final float x, y; // Primary position
        public final float deltaX, deltaY; // For swipes
        public final float scale; // For pinch gestures
        public final long duration; // Gesture duration
        public final int touchCount; // Number of fingers
        
        public Gesture(InputAction action, float x, float y, float deltaX, float deltaY, 
                      float scale, long duration, int touchCount) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.scale = scale;
            this.duration = duration;
            this.touchCount = touchCount;
        }
        
        @Override
        public String toString() {
            return String.format("Gesture{action=%s, pos=(%.1f,%.1f), delta=(%.1f,%.1f), scale=%.2f, duration=%dms, touches=%d}", 
                               action, x, y, deltaX, deltaY, scale, duration, touchCount);
        }
    }
    
    public interface GestureListener {
        void onGestureDetected(Gesture gesture);
    }
    
    private final List<GestureListener> listeners = new ArrayList<>();
    private final Map<Integer, Queue<TouchPoint>> touchTracks = new HashMap<>();
    private final Queue<Gesture> recentGestures = new ConcurrentLinkedQueue<>();
    
    // Gesture detection parameters
    private final float tapMaxDistance = 20.0f; // pixels
    private final long tapMaxDuration = 200; // milliseconds
    private final long doubleTapMaxInterval = 300; // milliseconds
    private final long longPressMinDuration = 500; // milliseconds
    private final float swipeMinDistance = 50.0f; // pixels
    private final float swipeMaxDeviation = 30.0f; // pixels
    private final float pinchMinDistance = 20.0f; // pixels
    
    // State tracking
    private long lastTapTime = 0;
    private TouchPoint lastTapPoint = null;
    private final Map<Integer, TouchPoint> activeTouches = new HashMap<>();
    private final Map<Integer, Long> touchStartTimes = new HashMap<>();
    
    public void addGestureListener(GestureListener listener) {
        listeners.add(listener);
    }
    
    public void removeGestureListener(GestureListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Process a touch down event
     */
    public void onTouchDown(int touchId, float x, float y, float pressure) {
        long currentTime = System.currentTimeMillis();
        TouchPoint point = new TouchPoint(touchId, x, y, currentTime, pressure);
        
        activeTouches.put(touchId, point);
        touchStartTimes.put(touchId, currentTime);
        
        // Initialize touch track
        touchTracks.computeIfAbsent(touchId, k -> new ConcurrentLinkedQueue<>()).offer(point);
        
        // Limit track size
        Queue<TouchPoint> track = touchTracks.get(touchId);
        while (track.size() > 100) {
            track.poll();
        }
    }
    
    /**
     * Process a touch move event
     */
    public void onTouchMove(int touchId, float x, float y, float pressure) {
        long currentTime = System.currentTimeMillis();
        TouchPoint point = new TouchPoint(touchId, x, y, currentTime, pressure);
        
        activeTouches.put(touchId, point);
        
        // Add to track
        Queue<TouchPoint> track = touchTracks.get(touchId);
        if (track != null) {
            track.offer(point);
            
            // Limit track size
            while (track.size() > 100) {
                track.poll();
            }
        }
        
        // Check for ongoing gestures
        checkPinchGesture();
    }
    
    /**
     * Process a touch up event
     */
    public void onTouchUp(int touchId, float x, float y) {
        long currentTime = System.currentTimeMillis();
        TouchPoint endPoint = new TouchPoint(touchId, x, y, currentTime, 0.0f);
        
        TouchPoint startPoint = activeTouches.get(touchId);
        Long startTime = touchStartTimes.get(touchId);
        
        if (startPoint != null && startTime != null) {
            long duration = currentTime - startTime;
            float distance = startPoint.distanceTo(endPoint);
            
            // Analyze the gesture
            analyzeGesture(touchId, startPoint, endPoint, duration, distance);
        }
        
        // Clean up
        activeTouches.remove(touchId);
        touchStartTimes.remove(touchId);
        
        // Keep track for a short time for multi-touch analysis
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                touchTracks.remove(touchId);
            }
        }, 1000);
    }
    
    /**
     * Analyze a completed touch gesture
     */
    private void analyzeGesture(int touchId, TouchPoint start, TouchPoint end, long duration, float distance) {
        // Check for tap
        if (distance <= tapMaxDistance && duration <= tapMaxDuration) {
            handleTap(end);
            return;
        }
        
        // Check for long press
        if (distance <= tapMaxDistance && duration >= longPressMinDuration) {
            handleLongPress(end, duration);
            return;
        }
        
        // Check for swipe
        if (distance >= swipeMinDistance) {
            handleSwipe(start, end, duration);
            return;
        }
    }
    
    /**
     * Handle tap gesture
     */
    private void handleTap(TouchPoint point) {
        long currentTime = point.timestamp;
        
        // Check for double tap
        if (lastTapTime > 0 && lastTapPoint != null) {
            long timeSinceLastTap = currentTime - lastTapTime;
            float distanceFromLastTap = point.distanceTo(lastTapPoint);
            
            if (timeSinceLastTap <= doubleTapMaxInterval && distanceFromLastTap <= tapMaxDistance) {
                // Double tap detected
                Gesture gesture = new Gesture(InputAction.DOUBLE_TAP, point.x, point.y, 0, 0, 1.0f, 0, 1);
                fireGestureDetected(gesture);
                
                // Reset to prevent triple tap
                lastTapTime = 0;
                lastTapPoint = null;
                return;
            }
        }
        
        // Single tap
        Gesture gesture = new Gesture(InputAction.TAP, point.x, point.y, 0, 0, 1.0f, 0, 1);
        fireGestureDetected(gesture);
        
        lastTapTime = currentTime;
        lastTapPoint = point;
    }
    
    /**
     * Handle long press gesture
     */
    private void handleLongPress(TouchPoint point, long duration) {
        Gesture gesture = new Gesture(InputAction.LONG_PRESS, point.x, point.y, 0, 0, 1.0f, duration, 1);
        fireGestureDetected(gesture);
    }
    
    /**
     * Handle swipe gesture
     */
    private void handleSwipe(TouchPoint start, TouchPoint end, long duration) {
        float deltaX = end.x - start.x;
        float deltaY = end.y - start.y;
        float distance = start.distanceTo(end);
        
        // Determine swipe direction
        InputAction swipeAction;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        
        if (absDeltaX > absDeltaY) {
            // Horizontal swipe
            if (deltaX > 0) {
                swipeAction = InputAction.SWIPE_RIGHT;
            } else {
                swipeAction = InputAction.SWIPE_LEFT;
            }
        } else {
            // Vertical swipe
            if (deltaY > 0) {
                swipeAction = InputAction.SWIPE_DOWN;
            } else {
                swipeAction = InputAction.SWIPE_UP;
            }
        }
        
        // Check if the swipe is straight enough
        float deviation = Math.min(absDeltaX, absDeltaY);
        if (deviation <= swipeMaxDeviation || deviation / distance <= 0.3f) {
            Gesture gesture = new Gesture(swipeAction, start.x, start.y, deltaX, deltaY, 1.0f, duration, 1);
            fireGestureDetected(gesture);
        }
    }
    
    /**
     * Check for pinch gesture (two-finger zoom)
     */
    private void checkPinchGesture() {
        if (activeTouches.size() != 2) return;
        
        List<TouchPoint> touches = new ArrayList<>(activeTouches.values());
        TouchPoint touch1 = touches.get(0);
        TouchPoint touch2 = touches.get(1);
        
        // Get initial positions
        Queue<TouchPoint> track1 = touchTracks.get(touch1.id);
        Queue<TouchPoint> track2 = touchTracks.get(touch2.id);
        
        if (track1 == null || track2 == null || track1.isEmpty() || track2.isEmpty()) {
            return;
        }
        
        TouchPoint start1 = track1.peek();
        TouchPoint start2 = track2.peek();
        
        if (start1 == null || start2 == null) return;
        
        float initialDistance = start1.distanceTo(start2);
        float currentDistance = touch1.distanceTo(touch2);
        
        if (initialDistance < pinchMinDistance) return;
        
        float scale = currentDistance / initialDistance;
        
        // Only trigger if there's significant scale change
        if (Math.abs(scale - 1.0f) > 0.1f) {
            float centerX = (touch1.x + touch2.x) / 2;
            float centerY = (touch1.y + touch2.y) / 2;
            
            Gesture gesture = new Gesture(InputAction.PINCH, centerX, centerY, 0, 0, scale, 
                                        touch1.timestamp - start1.timestamp, 2);
            fireGestureDetected(gesture);
        }
    }
    
    /**
     * Fire gesture detected event to all listeners
     */
    private void fireGestureDetected(Gesture gesture) {
        recentGestures.offer(gesture);
        
        // Limit recent gestures queue
        while (recentGestures.size() > 50) {
            recentGestures.poll();
        }
        
        logger.debug("Gesture detected: {}", gesture);
        
        for (GestureListener listener : listeners) {
            try {
                listener.onGestureDetected(gesture);
            } catch (Exception e) {
                logger.error("Error in gesture listener", e);
            }
        }
    }
    
    /**
     * Get recent gestures
     */
    public List<Gesture> getRecentGestures() {
        return new ArrayList<>(recentGestures);
    }
    
    /**
     * Clear all gesture state
     */
    public void clear() {
        activeTouches.clear();
        touchStartTimes.clear();
        touchTracks.clear();
        recentGestures.clear();
        lastTapTime = 0;
        lastTapPoint = null;
    }
    
    /**
     * Get current touch count
     */
    public int getActiveTouchCount() {
        return activeTouches.size();
    }
    
    /**
     * Check if a specific gesture was recently detected
     */
    public boolean wasGestureDetected(InputAction gestureAction, long withinMs) {
        long currentTime = System.currentTimeMillis();
        
        return recentGestures.stream()
                           .anyMatch(gesture -> gesture.action == gestureAction && 
                                              currentTime - gesture.duration <= withinMs);
    }
    
    /**
     * Get gesture recognition statistics
     */
    public String getStats() {
        return String.format("GestureRecognizer{activeTouches=%d, recentGestures=%d, tracks=%d}", 
                           activeTouches.size(), recentGestures.size(), touchTracks.size());
    }
}