package com.odyssey.util;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced logging utility for The Odyssey.
 * 
 * This class provides a wrapper around SLF4J with additional functionality
 * for performance tracking, categorized logging, and game-specific features.
 */
public class Logger {
    
    // SLF4J logger instance
    private final org.slf4j.Logger slf4jLogger;
    
    // Performance tracking
    private final ConcurrentHashMap<String, AtomicLong> performanceCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> performanceTimers = new ConcurrentHashMap<>();
    
    // Markers for different log categories
    public static final Marker PERFORMANCE = MarkerFactory.getMarker("PERFORMANCE");
    public static final Marker RENDERING = MarkerFactory.getMarker("RENDERING");
    public static final Marker PHYSICS = MarkerFactory.getMarker("PHYSICS");
    public static final Marker NETWORKING = MarkerFactory.getMarker("NETWORKING");
    public static final Marker AUDIO = MarkerFactory.getMarker("AUDIO");
    public static final Marker INPUT = MarkerFactory.getMarker("INPUT");
    public static final Marker WORLD = MarkerFactory.getMarker("WORLD");
    public static final Marker SHIP = MarkerFactory.getMarker("SHIP");
    public static final Marker OCEAN = MarkerFactory.getMarker("OCEAN");
    public static final Marker AI = MarkerFactory.getMarker("AI");
    public static final Marker MEMORY = MarkerFactory.getMarker("MEMORY");
    public static final Marker CONFIG = MarkerFactory.getMarker("CONFIG");
    
    // Static logger cache
    private static final ConcurrentHashMap<String, Logger> loggerCache = new ConcurrentHashMap<>();
    
    /**
     * Private constructor - use getLogger() to create instances.
     */
    private Logger(String name) {
        this.slf4jLogger = LoggerFactory.getLogger(name);
    }
    
    /**
     * Get a logger instance for the specified class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    /**
     * Get a logger instance for the specified name.
     */
    public static Logger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, Logger::new);
    }
    
    // Standard logging methods
    
    public void trace(String message) {
        slf4jLogger.trace(message);
    }
    
    public void trace(String format, Object... args) {
        slf4jLogger.trace(format, args);
    }
    
    public void trace(Marker marker, String message) {
        slf4jLogger.trace(marker, message);
    }
    
    public void trace(Marker marker, String format, Object... args) {
        slf4jLogger.trace(marker, format, args);
    }
    
    public void debug(String message) {
        slf4jLogger.debug(message);
    }
    
    public void debug(String format, Object... args) {
        slf4jLogger.debug(format, args);
    }
    
    public void debug(Marker marker, String message) {
        slf4jLogger.debug(marker, message);
    }
    
    public void debug(Marker marker, String format, Object... args) {
        slf4jLogger.debug(marker, format, args);
    }
    
    public void info(String message) {
        slf4jLogger.info(message);
    }
    
    public void info(String format, Object... args) {
        slf4jLogger.info(format, args);
    }
    
    public void info(Marker marker, String message) {
        slf4jLogger.info(marker, message);
    }
    
    public void info(Marker marker, String format, Object... args) {
        slf4jLogger.info(marker, format, args);
    }
    
    public void warn(String message) {
        slf4jLogger.warn(message);
    }
    
    public void warn(String format, Object... args) {
        slf4jLogger.warn(format, args);
    }
    
    public void warn(Marker marker, String message) {
        slf4jLogger.warn(marker, message);
    }
    
    public void warn(Marker marker, String format, Object... args) {
        slf4jLogger.warn(marker, format, args);
    }
    
    public void error(String message) {
        slf4jLogger.error(message);
    }
    
    public void error(String format, Object... args) {
        slf4jLogger.error(format, args);
    }
    
    public void error(String message, Throwable throwable) {
        slf4jLogger.error(message, throwable);
    }
    
    public void error(Marker marker, String message) {
        slf4jLogger.error(marker, message);
    }
    
    public void error(Marker marker, String format, Object... args) {
        slf4jLogger.error(marker, format, args);
    }
    
    public void error(Marker marker, String message, Throwable throwable) {
        slf4jLogger.error(marker, message, throwable);
    }
    
    // Level checking methods
    
    public boolean isTraceEnabled() {
        return slf4jLogger.isTraceEnabled();
    }
    
    public boolean isDebugEnabled() {
        return slf4jLogger.isDebugEnabled();
    }
    
    public boolean isInfoEnabled() {
        return slf4jLogger.isInfoEnabled();
    }
    
    public boolean isWarnEnabled() {
        return slf4jLogger.isWarnEnabled();
    }
    
    public boolean isErrorEnabled() {
        return slf4jLogger.isErrorEnabled();
    }
    
    // Performance tracking methods
    
    /**
     * Start a performance timer with the given name.
     */
    public void startTimer(String timerName) {
        performanceTimers.put(timerName, System.nanoTime());
    }
    
    /**
     * End a performance timer and log the elapsed time.
     */
    public long endTimer(String timerName) {
        Long startTime = performanceTimers.remove(timerName);
        if (startTime == null) {
            warn(PERFORMANCE, "Timer '{}' was not started", timerName);
            return 0;
        }
        
        long elapsedNanos = System.nanoTime() - startTime;
        float elapsedMillis = elapsedNanos / 1_000_000.0f;
        
        debug(PERFORMANCE, "Timer '{}': {:.3f}ms", timerName, elapsedMillis);
        return elapsedNanos;
    }
    
    /**
     * End a performance timer and log if it exceeds the threshold.
     */
    public long endTimer(String timerName, float thresholdMillis) {
        Long startTime = performanceTimers.remove(timerName);
        if (startTime == null) {
            warn(PERFORMANCE, "Timer '{}' was not started", timerName);
            return 0;
        }
        
        long elapsedNanos = System.nanoTime() - startTime;
        float elapsedMillis = elapsedNanos / 1_000_000.0f;
        
        if (elapsedMillis > thresholdMillis) {
            warn(PERFORMANCE, "Timer '{}': {:.3f}ms (exceeded threshold of {:.3f}ms)", 
                 timerName, elapsedMillis, thresholdMillis);
        } else {
            debug(PERFORMANCE, "Timer '{}': {:.3f}ms", timerName, elapsedMillis);
        }
        
        return elapsedNanos;
    }
    
    /**
     * Increment a performance counter.
     */
    public void incrementCounter(String counterName) {
        performanceCounters.computeIfAbsent(counterName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Add to a performance counter.
     */
    public void addToCounter(String counterName, long value) {
        performanceCounters.computeIfAbsent(counterName, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * Get the current value of a performance counter.
     */
    public long getCounterValue(String counterName) {
        AtomicLong counter = performanceCounters.get(counterName);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Reset a performance counter.
     */
    public void resetCounter(String counterName) {
        AtomicLong counter = performanceCounters.get(counterName);
        if (counter != null) {
            counter.set(0);
        }
    }
    
    /**
     * Log all performance counters.
     */
    public void logPerformanceCounters() {
        if (performanceCounters.isEmpty()) {
            debug(PERFORMANCE, "No performance counters to report");
            return;
        }
        
        info(PERFORMANCE, "Performance Counters:");
        performanceCounters.forEach((name, counter) -> {
            info(PERFORMANCE, "  {}: {}", name, counter.get());
        });
    }
    
    /**
     * Clear all performance counters.
     */
    public void clearPerformanceCounters() {
        performanceCounters.clear();
    }
    
    // Game-specific logging methods
    
    /**
     * Log rendering performance information.
     */
    public void logRenderingStats(int fps, float frameTime, int drawCalls, int vertices) {
        debug(RENDERING, "FPS: {}, Frame Time: {:.2f}ms, Draw Calls: {}, Vertices: {}", 
              fps, frameTime, drawCalls, vertices);
    }
    
    /**
     * Log memory usage information.
     */
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        info(MEMORY, "Memory Usage - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
             usedMemory / (1024 * 1024), freeMemory / (1024 * 1024),
             totalMemory / (1024 * 1024), maxMemory / (1024 * 1024));
    }
    
    /**
     * Log world generation progress.
     */
    public void logWorldGeneration(String stage, int progress, int total) {
        info(WORLD, "World Generation - {}: {}/{} ({:.1f}%)", 
             stage, progress, total, (progress * 100.0f) / total);
    }
    
    /**
     * Log ship system status.
     */
    public void logShipStatus(String shipName, String status, Object... details) {
        info(SHIP, "Ship '{}' - {}", shipName, String.format(status, details));
    }
    
    /**
     * Log ocean simulation performance.
     */
    public void logOceanSimulation(float waveHeight, float windSpeed, int activeChunks) {
        debug(OCEAN, "Ocean Simulation - Wave Height: {:.2f}m, Wind Speed: {:.2f}m/s, Active Chunks: {}", 
              waveHeight, windSpeed, activeChunks);
    }
    
    /**
     * Log configuration changes.
     */
    public void logConfigChange(String key, Object oldValue, Object newValue) {
        info(CONFIG, "Configuration changed - {}: {} -> {}", key, oldValue, newValue);
    }
    
    /**
     * Execute a block of code with performance timing.
     */
    public <T> T withTiming(String timerName, java.util.function.Supplier<T> supplier) {
        startTimer(timerName);
        try {
            return supplier.get();
        } finally {
            endTimer(timerName);
        }
    }
    
    /**
     * Execute a block of code with performance timing and threshold checking.
     */
    public <T> T withTiming(String timerName, float thresholdMillis, java.util.function.Supplier<T> supplier) {
        startTimer(timerName);
        try {
            return supplier.get();
        } finally {
            endTimer(timerName, thresholdMillis);
        }
    }
    
    /**
     * Execute a runnable with performance timing.
     */
    public void withTiming(String timerName, Runnable runnable) {
        startTimer(timerName);
        try {
            runnable.run();
        } finally {
            endTimer(timerName);
        }
    }
    
    /**
     * Execute a runnable with performance timing and threshold checking.
     */
    public void withTiming(String timerName, float thresholdMillis, Runnable runnable) {
        startTimer(timerName);
        try {
            runnable.run();
        } finally {
            endTimer(timerName, thresholdMillis);
        }
    }
    
    /**
     * Get the underlying SLF4J logger (for advanced usage).
     */
    public org.slf4j.Logger getSlf4jLogger() {
        return slf4jLogger;
    }
    
    // Static convenience methods for common logging patterns
    
    private static final Logger DEFAULT_LOGGER = getLogger("com.odyssey");
    
    /**
     * Static convenience method for world-related logging.
     */
    public static void world(String message) {
        DEFAULT_LOGGER.info(WORLD, message);
    }
    
    /**
     * Static convenience method for world-related logging with formatting.
     */
    public static void world(String format, Object... args) {
        DEFAULT_LOGGER.info(WORLD, format, args);
    }
    
    /**
     * Static convenience method for error logging.
     */
    public static void logError(String message) {
        DEFAULT_LOGGER.error(message);
    }
    
    /**
     * Static convenience method for error logging with formatting.
     */
    public static void logError(String format, Object... args) {
        DEFAULT_LOGGER.error(format, args);
    }
    
    /**
     * Static convenience method for info logging.
     */
    public static void logInfo(String message) {
        DEFAULT_LOGGER.info(message);
    }
    
    /**
     * Static convenience method for info logging with formatting.
     */
    public static void logInfo(String format, Object... args) {
        DEFAULT_LOGGER.info(format, args);
    }
}