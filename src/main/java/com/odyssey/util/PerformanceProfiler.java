package com.odyssey.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Performance profiling utility for monitoring rendering performance
 * and identifying bottlenecks in the game engine.
 * 
 * This class provides frame timing, GPU memory usage tracking,
 * and detailed performance metrics for optimization purposes.
 */
public class PerformanceProfiler {
    private static final Logger logger = Logger.getLogger(PerformanceProfiler.class.getName());
    
    // Singleton instance
    private static PerformanceProfiler instance;
    
    // Performance tracking
    private final Map<String, ProfileData> profiles = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    
    // Frame timing
    private long frameStartTime;
    private long lastFrameTime;
    private float currentFPS;
    private float averageFPS;
    private int frameCount;
    private long totalFrameTime;
    
    // Memory tracking
    private long lastGCTime;
    private long totalGCTime;
    private int gcCount;
    
    // Performance thresholds
    private float targetFPS = 60.0f;
    private float minAcceptableFPS = 30.0f;
    private boolean profilingEnabled = true;
    
    /**
     * Private constructor for singleton pattern
     */
    private PerformanceProfiler() {
        reset();
    }
    
    /**
     * Get the singleton instance of the performance profiler
     * @return The profiler instance
     */
    public static PerformanceProfiler getInstance() {
        if (instance == null) {
            synchronized (PerformanceProfiler.class) {
                if (instance == null) {
                    instance = new PerformanceProfiler();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start profiling a specific section
     * @param name The name of the section to profile
     */
    public void startSection(String name) {
        startProfile(name);
    }
    
    /**
     * End profiling for a specific section
     * @param name The name of the section to stop profiling
     */
    public void endSection(String name) {
        endProfile(name);
    }
    
    /**
     * Cleanup resources used by the performance profiler
     */
    public void cleanup() {
        reset();
        // Additional cleanup if needed
    }
    
    /**
     * Start profiling a specific operation
     * @param name The name of the operation to profile
     */
    public void startProfile(String name) {
        if (!profilingEnabled) return;
        
        startTimes.put(name, System.nanoTime());
    }
    
    /**
     * End profiling for a specific operation
     * @param name The name of the operation to stop profiling
     */
    public void endProfile(String name) {
        if (!profilingEnabled) return;
        
        Long startTime = startTimes.remove(name);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            updateProfile(name, duration);
        }
    }
    
    /**
     * Start frame timing
     */
    public void startFrame() {
        if (!profilingEnabled) return;
        
        frameStartTime = System.nanoTime();
    }
    
    /**
     * End frame timing and update FPS calculations
     */
    public void endFrame() {
        if (!profilingEnabled) return;
        
        long frameEndTime = System.nanoTime();
        long frameDuration = frameEndTime - frameStartTime;
        
        // Update frame statistics
        frameCount++;
        totalFrameTime += frameDuration;
        lastFrameTime = frameDuration;
        
        // Calculate current FPS
        currentFPS = 1_000_000_000.0f / frameDuration;
        
        // Calculate average FPS
        averageFPS = frameCount * 1_000_000_000.0f / totalFrameTime;
        
        // Update frame profile
        updateProfile("Frame", frameDuration);
        
        // Check for performance issues
        if (currentFPS < minAcceptableFPS) {
            logger.warning(String.format("Low FPS detected: %.2f (target: %.2f)", currentFPS, targetFPS));
        }
    }
    
    /**
     * Update profile data for a specific operation
     * @param name The operation name
     * @param duration The duration in nanoseconds
     */
    private void updateProfile(String name, long duration) {
        profiles.compute(name, (key, data) -> {
            if (data == null) {
                data = new ProfileData();
            }
            data.update(duration);
            return data;
        });
    }
    
    /**
     * Get the current FPS
     * @return Current frames per second
     */
    public float getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * Get the average FPS since profiling started
     * @return Average frames per second
     */
    public float getAverageFPS() {
        return averageFPS;
    }
    
    /**
     * Get the last frame time in milliseconds
     * @return Frame time in milliseconds
     */
    public float getLastFrameTimeMs() {
        return lastFrameTime / 1_000_000.0f;
    }
    
    /**
     * Get the average frame time in milliseconds
     * @return Average frame time in milliseconds
     */
    public float getAverageFrameTimeMs() {
        if (frameCount == 0) return 0.0f;
        return (totalFrameTime / frameCount) / 1_000_000.0f;
    }
    
    /**
     * Get profile data for a specific operation
     * @param name The operation name
     * @return Profile data or null if not found
     */
    public ProfileData getProfile(String name) {
        return profiles.get(name);
    }
    
    /**
     * Get all profile data
     * @return Map of all profile data
     */
    public Map<String, ProfileData> getAllProfiles() {
        return new HashMap<>(profiles);
    }
    
    /**
     * Check if the current performance meets the target FPS
     * @return True if performance is acceptable
     */
    public boolean isPerformanceAcceptable() {
        return currentFPS >= minAcceptableFPS;
    }
    
    /**
     * Get performance recommendation based on current metrics
     * @return Performance recommendation string
     */
    public String getPerformanceRecommendation() {
        if (currentFPS >= targetFPS) {
            return "Performance is excellent. Consider enabling higher quality settings.";
        } else if (currentFPS >= minAcceptableFPS) {
            return "Performance is acceptable. Current settings are appropriate.";
        } else {
            return "Performance is below acceptable levels. Consider lowering quality settings.";
        }
    }
    
    /**
     * Reset all profiling data
     */
    public void reset() {
        profiles.clear();
        startTimes.clear();
        frameCount = 0;
        totalFrameTime = 0;
        currentFPS = 0.0f;
        averageFPS = 0.0f;
        lastFrameTime = 0;
        frameStartTime = 0;
        gcCount = 0;
        totalGCTime = 0;
        lastGCTime = System.currentTimeMillis();
    }
    
    /**
     * Enable or disable profiling
     * @param enabled True to enable profiling
     */
    public void setProfilingEnabled(boolean enabled) {
        this.profilingEnabled = enabled;
        if (!enabled) {
            profiles.clear();
            startTimes.clear();
        }
    }
    
    /**
     * Check if profiling is enabled
     * @return True if profiling is enabled
     */
    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }
    
    /**
     * Set the target FPS for performance monitoring
     * @param targetFPS The target frames per second
     */
    public void setTargetFPS(float targetFPS) {
        this.targetFPS = targetFPS;
    }
    
    /**
     * Set the minimum acceptable FPS
     * @param minFPS The minimum acceptable frames per second
     */
    public void setMinAcceptableFPS(float minFPS) {
        this.minAcceptableFPS = minFPS;
    }
    
    /**
     * Get memory usage information
     * @return Memory usage string
     */
    public String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return String.format("Memory: %d MB used / %d MB total / %d MB max", 
                           usedMemory / (1024 * 1024),
                           totalMemory / (1024 * 1024),
                           maxMemory / (1024 * 1024));
    }
    
    /**
     * Generate a comprehensive performance report
     * @return Performance report string
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Report ===\n");
        report.append(String.format("Current FPS: %.2f\n", currentFPS));
        report.append(String.format("Average FPS: %.2f\n", averageFPS));
        report.append(String.format("Frame Time: %.2f ms (avg: %.2f ms)\n", 
                                  getLastFrameTimeMs(), getAverageFrameTimeMs()));
        report.append(String.format("Frame Count: %d\n", frameCount));
        report.append(getMemoryUsage()).append("\n");
        report.append("Recommendation: ").append(getPerformanceRecommendation()).append("\n");
        
        report.append("\n=== Operation Profiles ===\n");
        for (Map.Entry<String, ProfileData> entry : profiles.entrySet()) {
            ProfileData data = entry.getValue();
            report.append(String.format("%s: %.2f ms avg (min: %.2f, max: %.2f, calls: %d)\n",
                                      entry.getKey(),
                                      data.getAverageMs(),
                                      data.getMinMs(),
                                      data.getMaxMs(),
                                      data.getCallCount()));
        }
        
        return report.toString();
    }
    
    /**
     * Profile data container class
     */
    public static class ProfileData {
        private long totalTime;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = Long.MIN_VALUE;
        private int callCount;
        
        /**
         * Update profile data with a new timing
         * @param duration Duration in nanoseconds
         */
        public void update(long duration) {
            totalTime += duration;
            callCount++;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
        }
        
        /**
         * Get average time in milliseconds
         * @return Average time in milliseconds
         */
        public float getAverageMs() {
            if (callCount == 0) return 0.0f;
            return (totalTime / callCount) / 1_000_000.0f;
        }
        
        /**
         * Get minimum time in milliseconds
         * @return Minimum time in milliseconds
         */
        public float getMinMs() {
            if (callCount == 0) return 0.0f;
            return minTime / 1_000_000.0f;
        }
        
        /**
         * Get maximum time in milliseconds
         * @return Maximum time in milliseconds
         */
        public float getMaxMs() {
            if (callCount == 0) return 0.0f;
            return maxTime / 1_000_000.0f;
        }
        
        /**
         * Get total call count
         * @return Number of calls
         */
        public int getCallCount() {
            return callCount;
        }
        
        /**
         * Get total time in nanoseconds
         * @return Total time in nanoseconds
         */
        public long getTotalTimeNs() {
            return totalTime;
        }
    }
}