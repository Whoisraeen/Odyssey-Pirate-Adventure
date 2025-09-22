package com.odyssey.util;

/**
 * High-precision timer utility for The Odyssey.
 * 
 * This class provides accurate timing functionality for frame rate calculation,
 * delta time computation, and performance profiling. It uses System.nanoTime()
 * for high precision timing.
 */
public class Timer {
    
    // Time values in nanoseconds
    private long lastTime;
    private long currentTime;
    private long deltaTime;
    
    // Frame rate tracking
    private int frameCount;
    private long fpsTimer;
    private int fps;
    private int targetFPS;
    
    // Performance tracking
    private long frameStartTime;
    private long frameEndTime;
    private long frameTime;
    private long maxFrameTime;
    private long minFrameTime;
    private long totalFrameTime;
    
    // Constants
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
    
    /**
     * Create a new timer with the specified target FPS.
     * 
     * @param targetFPS The target frames per second (0 for unlimited)
     */
    public Timer(int targetFPS) {
        this.targetFPS = targetFPS;
        this.lastTime = System.nanoTime();
        this.currentTime = lastTime;
        this.fpsTimer = 0;
        this.frameCount = 0;
        this.fps = 0;
        this.minFrameTime = Long.MAX_VALUE;
        this.maxFrameTime = 0;
        this.totalFrameTime = 0;
    }
    
    /**
     * Create a new timer with unlimited FPS.
     */
    public Timer() {
        this(0);
    }
    
    /**
     * Update the timer. Should be called once per frame.
     */
    public void update() {
        currentTime = System.nanoTime();
        deltaTime = currentTime - lastTime;
        lastTime = currentTime;
        
        // Update FPS calculation
        updateFPS();
        
        // Update performance tracking
        updatePerformanceTracking();
    }
    
    /**
     * Mark the start of a frame for performance tracking.
     */
    public void startFrame() {
        frameStartTime = System.nanoTime();
    }
    
    /**
     * Mark the end of a frame for performance tracking.
     */
    public void endFrame() {
        frameEndTime = System.nanoTime();
        frameTime = frameEndTime - frameStartTime;
        
        // Update min/max frame times
        if (frameTime < minFrameTime) {
            minFrameTime = frameTime;
        }
        if (frameTime > maxFrameTime) {
            maxFrameTime = frameTime;
        }
        
        totalFrameTime += frameTime;
    }
    
    /**
     * Update FPS calculation.
     */
    private void updateFPS() {
        frameCount++;
        fpsTimer += deltaTime;
        
        // Update FPS every second
        if (fpsTimer >= NANOSECONDS_PER_SECOND) {
            fps = frameCount;
            frameCount = 0;
            fpsTimer = 0;
        }
    }
    
    /**
     * Update performance tracking.
     */
    private void updatePerformanceTracking() {
        // Reset performance stats every second
        if (fpsTimer == 0 && frameCount == 0) {
            minFrameTime = Long.MAX_VALUE;
            maxFrameTime = 0;
            totalFrameTime = 0;
        }
    }
    
    /**
     * Sleep to maintain target FPS if set.
     */
    public void sync() {
        if (targetFPS <= 0) {
            return; // No FPS limit
        }
        
        long targetFrameTime = NANOSECONDS_PER_SECOND / targetFPS;
        long sleepTime = targetFrameTime - deltaTime;
        
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime / NANOSECONDS_PER_MILLISECOND, 
                           (int) (sleepTime % NANOSECONDS_PER_MILLISECOND));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Get the delta time in seconds.
     * 
     * @return Delta time in seconds
     */
    public float getDeltaTime() {
        return (float) deltaTime / NANOSECONDS_PER_SECOND;
    }
    
    /**
     * Get the delta time in milliseconds.
     * 
     * @return Delta time in milliseconds
     */
    public float getDeltaTimeMillis() {
        return (float) deltaTime / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the delta time in nanoseconds.
     * 
     * @return Delta time in nanoseconds
     */
    public long getDeltaTimeNanos() {
        return deltaTime;
    }
    
    /**
     * Get the current frames per second.
     * 
     * @return Current FPS
     */
    public int getFPS() {
        return fps;
    }
    
    /**
     * Get the target frames per second.
     * 
     * @return Target FPS (0 for unlimited)
     */
    public int getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * Set the target frames per second.
     * 
     * @param targetFPS Target FPS (0 for unlimited)
     */
    public void setTargetFPS(int targetFPS) {
        this.targetFPS = targetFPS;
    }
    
    /**
     * Get the current frame time in milliseconds.
     * 
     * @return Frame time in milliseconds
     */
    public float getFrameTimeMillis() {
        return (float) frameTime / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the minimum frame time in milliseconds.
     * 
     * @return Minimum frame time in milliseconds
     */
    public float getMinFrameTimeMillis() {
        if (minFrameTime == Long.MAX_VALUE) {
            return 0.0f;
        }
        return (float) minFrameTime / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the maximum frame time in milliseconds.
     * 
     * @return Maximum frame time in milliseconds
     */
    public float getMaxFrameTimeMillis() {
        return (float) maxFrameTime / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the average frame time in milliseconds.
     * 
     * @return Average frame time in milliseconds
     */
    public float getAverageFrameTimeMillis() {
        if (frameCount == 0) {
            return 0.0f;
        }
        return (float) (totalFrameTime / frameCount) / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the current time in nanoseconds.
     * 
     * @return Current time in nanoseconds
     */
    public long getCurrentTimeNanos() {
        return currentTime;
    }
    
    /**
     * Get the current time in milliseconds.
     * 
     * @return Current time in milliseconds
     */
    public long getCurrentTimeMillis() {
        return currentTime / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * Get the current time in seconds.
     * 
     * @return Current time in seconds
     */
    public double getCurrentTimeSeconds() {
        return (double) currentTime / NANOSECONDS_PER_SECOND;
    }
    
    /**
     * Get the current time in seconds as a float.
     * This method is commonly used in physics calculations.
     * 
     * @return Current time in seconds as float
     */
    public float getTime() {
        return (float) currentTime / NANOSECONDS_PER_SECOND;
    }
    
    /**
     * Check if the timer is running at the target FPS.
     * 
     * @param tolerance Tolerance in FPS (e.g., 5 for ±5 FPS)
     * @return true if running within tolerance of target FPS
     */
    public boolean isRunningAtTargetFPS(int tolerance) {
        if (targetFPS <= 0) {
            return true; // No target, always "on target"
        }
        
        return Math.abs(fps - targetFPS) <= tolerance;
    }
    
    /**
     * Reset the timer.
     */
    public void reset() {
        lastTime = System.nanoTime();
        currentTime = lastTime;
        deltaTime = 0;
        frameCount = 0;
        fpsTimer = 0;
        fps = 0;
        frameTime = 0;
        minFrameTime = Long.MAX_VALUE;
        maxFrameTime = 0;
        totalFrameTime = 0;
    }
    
    /**
     * Get a formatted string with timing information.
     * 
     * @return Formatted timing information
     */
    public String getTimingInfo() {
        return String.format("FPS: %d/%d, Frame: %.2fms (min: %.2fms, max: %.2fms, avg: %.2fms)", 
                           fps, targetFPS, getFrameTimeMillis(), getMinFrameTimeMillis(), 
                           getMaxFrameTimeMillis(), getAverageFrameTimeMillis());
    }
    
    /**
     * Get a simple FPS string.
     * 
     * @return Simple FPS string
     */
    public String getFPSString() {
        if (targetFPS > 0) {
            return String.format("FPS: %d/%d", fps, targetFPS);
        } else {
            return String.format("FPS: %d", fps);
        }
    }
}