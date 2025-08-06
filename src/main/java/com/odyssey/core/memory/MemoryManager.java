package com.odyssey.core.memory;

import com.odyssey.core.GameConfig;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central memory management system for The Odyssey.
 * Manages object pools, monitors memory usage, and provides memory optimization.
 */
public class MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);
    
    private final GameConfig config;
    private final ConcurrentHashMap<String, ObjectPool<?>> pools;
    private final ScheduledExecutorService monitoringService;
    private final GCTuner gcTuner;
    
    // Common object pools
    private final ObjectPool<Vector3f> vector3fPool;
    private final ObjectPool<Vector3i> vector3iPool;
    private final ObjectPool<float[]> floatArrayPool;
    private final ObjectPool<int[]> intArrayPool;
    private final ObjectPool<ChunkData> chunkDataPool;
    private final ObjectPool<MeshData> meshDataPool;
    
    // Memory monitoring
    private volatile long lastGcTime = 0;
    private volatile long totalAllocatedMemory = 0;
    private volatile double memoryPressure = 0.0;
    
    public MemoryManager(GameConfig config) {
        this.config = config;
        this.pools = new ConcurrentHashMap<>();
        this.monitoringService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MemoryManager-Monitor");
            t.setDaemon(true);
            return t;
        });
        this.gcTuner = new GCTuner(config);
        
        // Initialize common pools
        vector3fPool = createPool("Vector3f", Vector3f::new, Vector3f::zero, 1000);
        vector3iPool = createPool("Vector3i", Vector3i::new, Vector3i::zero, 1000);
        floatArrayPool = createPool("FloatArray", () -> new float[1024], arr -> {}, 100);
        intArrayPool = createPool("IntArray", () -> new int[1024], arr -> {}, 100);
        chunkDataPool = createPool("ChunkData", ChunkData::new, ChunkData::reset, 50);
        meshDataPool = createPool("MeshData", MeshData::new, MeshData::reset, 200);
        
        logger.info("Memory manager initialized with {} pools", pools.size());
    }
    
    /**
     * Initialize memory monitoring
     */
    public void initialize() {
        // Initialize GC tuner
        gcTuner.initialize();
        
        // Start memory monitoring
        monitoringService.scheduleAtFixedRate(this::monitorMemory, 1, 1, TimeUnit.SECONDS);
        
        // Schedule periodic pool cleanup
        monitoringService.scheduleAtFixedRate(this::cleanupPools, 30, 30, TimeUnit.SECONDS);
        
        logger.info("Memory monitoring started");
    }
    
    /**
     * Create and register a new object pool
     */
    public <T> ObjectPool<T> createPool(String name, java.util.function.Supplier<T> factory, 
                                       PoolableObject.Resetter<T> resetter, int maxSize) {
        ObjectPool<T> pool = new ObjectPool<>(name, factory, resetter, maxSize);
        pools.put(name, pool);
        return pool;
    }
    
    /**
     * Get an existing pool by name
     */
    @SuppressWarnings("unchecked")
    public <T> ObjectPool<T> getPool(String name) {
        return (ObjectPool<T>) pools.get(name);
    }
    
    // Convenience methods for common objects
    public Vector3f acquireVector3f() {
        return vector3fPool.acquire();
    }
    
    public void releaseVector3f(Vector3f vector) {
        vector3fPool.release(vector);
    }
    
    public Vector3i acquireVector3i() {
        return vector3iPool.acquire();
    }
    
    public void releaseVector3i(Vector3i vector) {
        vector3iPool.release(vector);
    }
    
    public float[] acquireFloatArray() {
        return floatArrayPool.acquire();
    }
    
    public void releaseFloatArray(float[] array) {
        floatArrayPool.release(array);
    }
    
    public int[] acquireIntArray() {
        return intArrayPool.acquire();
    }
    
    public void releaseIntArray(int[] array) {
        intArrayPool.release(array);
    }
    
    public ChunkData acquireChunkData() {
        return chunkDataPool.acquire();
    }
    
    public void releaseChunkData(ChunkData chunkData) {
        chunkDataPool.release(chunkData);
    }
    
    public MeshData acquireMeshData() {
        return meshDataPool.acquire();
    }
    
    public void releaseMeshData(MeshData meshData) {
        meshDataPool.release(meshData);
    }
    
    /**
     * Monitor memory usage and pressure
     */
    private void monitorMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memoryPressure = (double) usedMemory / maxMemory;
        
        // Log memory stats if debug mode is enabled
        if (config.isDebugMode()) {
            logger.debug("Memory: used={}MB, total={}MB, max={}MB, pressure={:.1f}%",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                memoryPressure * 100);
        }
        
        // Trigger cleanup if memory pressure is high
        if (memoryPressure > 0.8) {
            logger.warn("High memory pressure detected: {:.1f}%, triggering cleanup", memoryPressure * 100);
            triggerMemoryCleanup();
        }
    }
    
    /**
     * Clean up pools periodically
     */
    private void cleanupPools() {
        if (config.isDebugMode()) {
            logger.debug("Performing periodic pool cleanup");
            
            for (ObjectPool<?> pool : pools.values()) {
                ObjectPool.PoolStats stats = pool.getStats();
                logger.debug(stats.toString());
            }
        }
        
        // Force GC if memory pressure is moderate
        if (memoryPressure > 0.6) {
            System.gc();
            lastGcTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Trigger aggressive memory cleanup
     */
    private void triggerMemoryCleanup() {
        // Clear pools that are not frequently used
        for (ObjectPool<?> pool : pools.values()) {
            ObjectPool.PoolStats stats = pool.getStats();
            if (stats.hitRate < 0.5) { // Less than 50% hit rate
                pool.clear();
            }
        }
        
        // Force garbage collection
        System.gc();
        lastGcTime = System.currentTimeMillis();
        
        logger.info("Memory cleanup completed");
    }
    
    /**
     * Get current memory pressure (0.0 to 1.0)
     */
    public double getMemoryPressure() {
        return memoryPressure;
    }
    
    /**
     * Get GC tuner for advanced GC monitoring
     */
    public GCTuner getGCTuner() {
        return gcTuner;
    }
    
    /**
     * Get memory statistics
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        return new MemoryStats(
            runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            memoryPressure,
            lastGcTime,
            pools.size()
        );
    }
    
    /**
     * Cleanup all resources
     */
    public void cleanup() {
        logger.info("Cleaning up memory manager");
        
        monitoringService.shutdown();
        try {
            if (!monitoringService.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringService.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitoringService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Cleanup GC tuner
        if (gcTuner != null) {
            gcTuner.cleanup();
        }
        
        // Clear all pools
        for (ObjectPool<?> pool : pools.values()) {
            pool.clear();
        }
        pools.clear();
        
        logger.info("Memory manager cleanup complete");
    }
    
    /**
     * Memory statistics for monitoring
     */
    public static class MemoryStats {
        public final long maxMemory;
        public final long totalMemory;
        public final long freeMemory;
        public final long usedMemory;
        public final double memoryPressure;
        public final long lastGcTime;
        public final int poolCount;
        
        public MemoryStats(long maxMemory, long totalMemory, long freeMemory, 
                          double memoryPressure, long lastGcTime, int poolCount) {
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = totalMemory - freeMemory;
            this.memoryPressure = memoryPressure;
            this.lastGcTime = lastGcTime;
            this.poolCount = poolCount;
        }
        
        @Override
        public String toString() {
            return String.format("Memory: used=%dMB, total=%dMB, max=%dMB, pressure=%.1f%%, pools=%d",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                memoryPressure * 100,
                poolCount);
        }
    }
    
    /**
     * Poolable chunk data structure
     */
    public static class ChunkData implements PoolableObject {
        public int[] blocks;
        public byte[] lightLevels;
        public boolean isDirty;
        public int chunkX, chunkZ;
        
        public ChunkData() {
            this.blocks = new int[32 * 256 * 32]; // 32x256x32 chunk
            this.lightLevels = new byte[32 * 256 * 32];
            reset();
        }
        
        @Override
        public void reset() {
            if (blocks != null) {
                java.util.Arrays.fill(blocks, 0);
            }
            if (lightLevels != null) {
                java.util.Arrays.fill(lightLevels, (byte) 0);
            }
            isDirty = false;
            chunkX = 0;
            chunkZ = 0;
        }
    }
    
    /**
     * Poolable mesh data structure
     */
    public static class MeshData implements PoolableObject {
        public float[] vertices;
        public int[] indices;
        public int vertexCount;
        public int indexCount;
        
        public MeshData() {
            this.vertices = new float[8192]; // Initial capacity
            this.indices = new int[4096];
            reset();
        }
        
        @Override
        public void reset() {
            vertexCount = 0;
            indexCount = 0;
            // Don't clear arrays, just reset counters for performance
        }
    }
}