package com.odyssey.core.memory;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Garbage Collection tuning and monitoring for The Odyssey.
 * Provides intelligent GC management and performance optimization.
 */
public class GCTuner {
    private static final Logger logger = LoggerFactory.getLogger(GCTuner.class);
    
    private final GameConfig config;
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final ScheduledExecutorService scheduler;
    
    // GC Statistics
    private long lastGcCount = 0;
    private long lastGcTime = 0;
    private double averageGcTime = 0.0;
    private long totalGcCount = 0;
    private long totalGcTime = 0;
    
    // Performance thresholds
    private static final double HIGH_MEMORY_THRESHOLD = 0.85; // 85% memory usage
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.95; // 95% memory usage
    private static final long MAX_GC_PAUSE_MS = 50; // Maximum acceptable GC pause
    private static final double MAX_GC_OVERHEAD = 0.05; // 5% of total time spent in GC
    
    // Adaptive tuning
    private boolean adaptiveTuningEnabled = true;
    private long lastTuningTime = 0;
    private static final long TUNING_INTERVAL_MS = 30000; // 30 seconds
    
    public GCTuner(GameConfig config) {
        this.config = config;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GCTuner-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("GC Tuner initialized with {} garbage collectors", gcBeans.size());
        logGCConfiguration();
    }
    
    /**
     * Initialize GC monitoring and tuning
     */
    public void initialize() {
        // Start GC monitoring
        scheduler.scheduleAtFixedRate(this::monitorGC, 1, 1, TimeUnit.SECONDS);
        
        // Start adaptive tuning
        if (adaptiveTuningEnabled) {
            scheduler.scheduleAtFixedRate(this::performAdaptiveTuning, 30, 30, TimeUnit.SECONDS);
        }
        
        // Apply initial optimizations
        applyInitialOptimizations();
        
        logger.info("GC monitoring and tuning started");
    }
    
    /**
     * Log current GC configuration
     */
    private void logGCConfiguration() {
        logger.info("=== Garbage Collection Configuration ===");
        
        // Log JVM arguments related to GC
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (arg.startsWith("-XX:") && (arg.contains("GC") || arg.contains("Heap") || arg.contains("Memory"))) {
                logger.info("JVM GC Arg: {}", arg);
            }
        }
        
        // Log available garbage collectors
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            logger.info("GC: {} - Memory Pools: {}", gcBean.getName(), gcBean.getMemoryPoolNames());
        }
        
        // Log memory configuration
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        logger.info("Heap Memory: init={}MB, used={}MB, committed={}MB, max={}MB",
            heapUsage.getInit() / (1024 * 1024),
            heapUsage.getUsed() / (1024 * 1024),
            heapUsage.getCommitted() / (1024 * 1024),
            heapUsage.getMax() / (1024 * 1024));
            
        logger.info("Non-Heap Memory: init={}MB, used={}MB, committed={}MB, max={}MB",
            nonHeapUsage.getInit() / (1024 * 1024),
            nonHeapUsage.getUsed() / (1024 * 1024),
            nonHeapUsage.getCommitted() / (1024 * 1024),
            nonHeapUsage.getMax() / (1024 * 1024));
    }
    
    /**
     * Apply initial GC optimizations
     */
    private void applyInitialOptimizations() {
        // Force an initial GC to establish baseline
        System.gc();
        
        // Log recommendations for JVM tuning
        logGCRecommendations();
    }
    
    /**
     * Log GC tuning recommendations
     */
    private void logGCRecommendations() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long maxHeap = heapUsage.getMax();
        
        logger.info("=== GC Tuning Recommendations ===");
        
        if (maxHeap < 2L * 1024 * 1024 * 1024) { // Less than 2GB
            logger.info("Consider increasing heap size: -Xmx4g or higher");
        }
        
        // Check if G1GC is being used (recommended for games)
        boolean usingG1 = gcBeans.stream().anyMatch(gc -> gc.getName().contains("G1"));
        if (!usingG1) {
            logger.info("Consider using G1GC for better latency: -XX:+UseG1GC");
            logger.info("G1GC tuning options:");
            logger.info("  -XX:MaxGCPauseMillis=50 (target 50ms pause times)");
            logger.info("  -XX:G1HeapRegionSize=16m (adjust based on heap size)");
            logger.info("  -XX:+G1UseAdaptiveIHOP (adaptive heap occupancy)");
        }
        
        logger.info("General recommendations:");
        logger.info("  -XX:+UnlockExperimentalVMOptions");
        logger.info("  -XX:+UseStringDeduplication (reduce string memory usage)");
        logger.info("  -XX:+OptimizeStringConcat (optimize string operations)");
    }
    
    /**
     * Monitor GC performance
     */
    private void monitorGC() {
        long currentGcCount = 0;
        long currentGcTime = 0;
        
        // Collect GC statistics
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            currentGcCount += gcBean.getCollectionCount();
            currentGcTime += gcBean.getCollectionTime();
        }
        
        // Calculate deltas
        long gcCountDelta = currentGcCount - lastGcCount;
        long gcTimeDelta = currentGcTime - lastGcTime;
        
        if (gcCountDelta > 0) {
            double avgPauseTime = (double) gcTimeDelta / gcCountDelta;
            averageGcTime = (averageGcTime * 0.9) + (avgPauseTime * 0.1); // Exponential moving average
            
            totalGcCount += gcCountDelta;
            totalGcTime += gcTimeDelta;
            
            if (config.isDebugMode()) {
                logger.debug("GC Activity: {} collections, {}ms total, {:.1f}ms avg pause",
                    gcCountDelta, gcTimeDelta, avgPauseTime);
            }
            
            // Check for performance issues
            if (avgPauseTime > MAX_GC_PAUSE_MS) {
                logger.warn("High GC pause time detected: {:.1f}ms (threshold: {}ms)", 
                    avgPauseTime, MAX_GC_PAUSE_MS);
            }
        }
        
        // Check memory pressure
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryPressure = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        if (memoryPressure > CRITICAL_MEMORY_THRESHOLD) {
            logger.error("Critical memory pressure: {:.1f}% heap usage", memoryPressure * 100);
            triggerEmergencyGC();
        } else if (memoryPressure > HIGH_MEMORY_THRESHOLD) {
            logger.warn("High memory pressure: {:.1f}% heap usage", memoryPressure * 100);
        }
        
        lastGcCount = currentGcCount;
        lastGcTime = currentGcTime;
    }
    
    /**
     * Perform adaptive GC tuning
     */
    private void performAdaptiveTuning() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTuningTime < TUNING_INTERVAL_MS) {
            return;
        }
        
        // Calculate GC overhead
        double gcOverhead = totalGcTime > 0 ? (double) totalGcTime / (currentTime - lastTuningTime) : 0.0;
        
        if (gcOverhead > MAX_GC_OVERHEAD) {
            logger.warn("High GC overhead detected: {:.2f}% (threshold: {:.2f}%)", 
                gcOverhead * 100, MAX_GC_OVERHEAD * 100);
            
            // Suggest tuning adjustments
            suggestTuningAdjustments();
        }
        
        if (config.isDebugMode()) {
            logger.debug("GC Performance: overhead={:.2f}%, avg_pause={:.1f}ms, total_collections={}",
                gcOverhead * 100, averageGcTime, totalGcCount);
        }
        
        lastTuningTime = currentTime;
    }
    
    /**
     * Suggest GC tuning adjustments
     */
    private void suggestTuningAdjustments() {
        logger.info("=== GC Performance Tuning Suggestions ===");
        
        if (averageGcTime > MAX_GC_PAUSE_MS) {
            logger.info("High pause times detected. Consider:");
            logger.info("  - Reducing -XX:MaxGCPauseMillis target");
            logger.info("  - Increasing heap size to reduce GC frequency");
            logger.info("  - Using concurrent collectors (G1GC, ZGC, Shenandoah)");
        }
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryPressure = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        if (memoryPressure > HIGH_MEMORY_THRESHOLD) {
            logger.info("High memory pressure detected. Consider:");
            logger.info("  - Increasing heap size (-Xmx)");
            logger.info("  - Optimizing object allocation patterns");
            logger.info("  - Using object pools for frequently allocated objects");
        }
        
        if (totalGcCount > 1000) { // High GC frequency
            logger.info("High GC frequency detected. Consider:");
            logger.info("  - Increasing young generation size (-XX:NewRatio)");
            logger.info("  - Tuning allocation rate and object lifecycle");
        }
    }
    
    /**
     * Trigger emergency garbage collection
     */
    private void triggerEmergencyGC() {
        logger.warn("Triggering emergency garbage collection due to critical memory pressure");
        
        // Force full GC
        System.gc();
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Log results
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double newMemoryPressure = (double) heapUsage.getUsed() / heapUsage.getMax();
        logger.info("Emergency GC completed. Memory pressure: {:.1f}%", newMemoryPressure * 100);
    }
    
    /**
     * Get current GC statistics
     */
    public GCStats getGCStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryPressure = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        long currentGcCount = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long currentGcTime = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        
        return new GCStats(
            currentGcCount,
            currentGcTime,
            averageGcTime,
            memoryPressure,
            heapUsage.getUsed(),
            heapUsage.getMax()
        );
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        logger.info("Cleaning up GC tuner");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Log final GC statistics
        GCStats finalStats = getGCStats();
        logger.info("Final GC Statistics: {}", finalStats);
    }
    
    /**
     * GC statistics for monitoring
     */
    public static class GCStats {
        public final long totalCollections;
        public final long totalCollectionTime;
        public final double averagePauseTime;
        public final double memoryPressure;
        public final long usedMemory;
        public final long maxMemory;
        
        public GCStats(long totalCollections, long totalCollectionTime, double averagePauseTime,
                      double memoryPressure, long usedMemory, long maxMemory) {
            this.totalCollections = totalCollections;
            this.totalCollectionTime = totalCollectionTime;
            this.averagePauseTime = averagePauseTime;
            this.memoryPressure = memoryPressure;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
        }
        
        @Override
        public String toString() {
            return String.format("GC Stats: collections=%d, total_time=%dms, avg_pause=%.1fms, " +
                               "memory_pressure=%.1f%%, used=%dMB, max=%dMB",
                totalCollections, totalCollectionTime, averagePauseTime,
                memoryPressure * 100, usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
        }
    }
}