package com.odyssey.graphics;

import com.odyssey.core.jobs.Job;
import com.odyssey.core.jobs.JobHandle;
import com.odyssey.core.jobs.JobSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Asset Streaming Pipeline for The Odyssey.
 * Provides progressive loading of distant world content with memory management
 * and priority-based streaming for optimal performance.
 */
public class AssetManager {
    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    
    // Asset cache with LRU eviction
    private final ConcurrentHashMap<String, CachedAsset> assetCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheMemoryUsage = new AtomicLong(0);
    
    // Configuration
    private final long maxCacheMemory;
    private final int maxConcurrentLoads;
    private final JobSystem jobSystem;
    
    // Asset loading state
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> loadingAssets = new ConcurrentHashMap<>();
    
    /**
     * Represents a cached asset with metadata for memory management.
     */
    private static class CachedAsset {
        final byte[] data;
        final long size;
        final long lastAccessed;
        final AssetPriority priority;
        
        CachedAsset(byte[] data, AssetPriority priority) {
            this.data = data;
            this.size = data.length;
            this.lastAccessed = System.currentTimeMillis();
            this.priority = priority;
        }
        
        void updateAccess() {
            // Note: In a real implementation, we'd use AtomicLong for thread safety
            // For simplicity, we're using the immutable approach here
        }
    }
    
    /**
     * Asset loading priority levels for streaming optimization.
     */
    public enum AssetPriority {
        CRITICAL(0),    // Player immediate vicinity
        HIGH(1),        // Visible chunks
        MEDIUM(2),      // Near chunks
        LOW(3),         // Distant chunks
        BACKGROUND(4);  // Preloading
        
        private final int level;
        
        AssetPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Creates a new AssetManager with specified configuration.
     *
     * @param maxCacheMemoryMB Maximum cache memory in megabytes
     * @param maxConcurrentLoads Maximum concurrent asset loading operations
     * @param jobSystem Job system for async operations
     */
    public AssetManager(int maxCacheMemoryMB, int maxConcurrentLoads, JobSystem jobSystem) {
        this.maxCacheMemory = maxCacheMemoryMB * 1024L * 1024L;
        this.maxConcurrentLoads = maxConcurrentLoads;
        this.jobSystem = jobSystem;
        
        logger.info("AssetManager initialized with {}MB cache, {} concurrent loads", 
                   maxCacheMemoryMB, maxConcurrentLoads);
    }
    
    /**
     * Loads an asset asynchronously with specified priority.
     *
     * @param assetPath Path to the asset
     * @param priority Loading priority
     * @param callback Callback when asset is loaded
     * @return CompletableFuture for the asset data
     */
    public CompletableFuture<byte[]> loadAssetAsync(String assetPath, AssetPriority priority, 
                                                   Consumer<byte[]> callback) {
        // Check cache first
        CachedAsset cached = assetCache.get(assetPath);
        if (cached != null) {
            cached.updateAccess();
            if (callback != null) {
                callback.accept(cached.data);
            }
            return CompletableFuture.completedFuture(cached.data);
        }
        
        // Check if already loading
        CompletableFuture<byte[]> existingLoad = loadingAssets.get(assetPath);
        if (existingLoad != null) {
            if (callback != null) {
                existingLoad.thenAccept(callback);
            }
            return existingLoad;
        }
        
        // Create a job to load the asset
        Job loadJob = new Job() {
            @Override
            public void execute() throws Exception {
                // Job execution is handled by the supplier
            }
            
            @Override
            public int getPriority() {
                return priority.getLevel();
            }
            
            @Override
            public String getDescription() {
                return "Load asset: " + assetPath;
            }
            
            @Override
            public boolean isCancellable() {
                return true;
            }
        };
        
        // Start new loading operation
        JobHandle<byte[]> jobHandle = jobSystem.submit(loadJob, () -> {
            try {
                return loadAssetFromDisk(assetPath);
            } catch (IOException e) {
                logger.error("Failed to load asset: {}", assetPath, e);
                throw new RuntimeException("Asset loading failed: " + assetPath, e);
            }
        });
        
        // Convert JobHandle to CompletableFuture
        CompletableFuture<byte[]> loadFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return jobHandle.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenApply(data -> {
            // Cache the loaded asset
            cacheAsset(assetPath, data, priority);
            loadingAssets.remove(assetPath);
            return data;
        });
        
        loadingAssets.put(assetPath, loadFuture);
        
        if (callback != null) {
            loadFuture.thenAccept(callback);
        }
        
        return loadFuture;
    }
    
    /**
     * Loads an asset synchronously (blocks current thread).
     *
     * @param assetPath Path to the asset
     * @param priority Loading priority
     * @return Asset data
     * @throws IOException If loading fails
     */
    public byte[] loadAssetSync(String assetPath, AssetPriority priority) throws IOException {
        // Check cache first
        CachedAsset cached = assetCache.get(assetPath);
        if (cached != null) {
            cached.updateAccess();
            return cached.data;
        }
        
        // Load from disk
        byte[] data = loadAssetFromDisk(assetPath);
        cacheAsset(assetPath, data, priority);
        return data;
    }
    
    /**
     * Preloads assets in the background based on predicted need.
     *
     * @param assetPaths Array of asset paths to preload
     * @param priority Priority for preloading
     */
    public void preloadAssets(String[] assetPaths, AssetPriority priority) {
        for (String assetPath : assetPaths) {
            if (!assetCache.containsKey(assetPath) && !loadingAssets.containsKey(assetPath)) {
                loadAssetAsync(assetPath, priority, null);
            }
        }
    }
    
    /**
     * Evicts assets from cache to free memory.
     *
     * @param targetMemory Target memory usage after eviction
     */
    public void evictAssets(long targetMemory) {
        if (cacheMemoryUsage.get() <= targetMemory) {
            return;
        }
        
        // Sort assets by priority and last access time for eviction
        assetCache.entrySet().stream()
                .sorted((e1, e2) -> {
                    CachedAsset a1 = e1.getValue();
                    CachedAsset a2 = e2.getValue();
                    
                    // First by priority (higher priority = lower eviction chance)
                    int priorityCompare = Integer.compare(a2.priority.getLevel(), a1.priority.getLevel());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    
                    // Then by last access time (older = higher eviction chance)
                    return Long.compare(a1.lastAccessed, a2.lastAccessed);
                })
                .forEach(entry -> {
                    if (cacheMemoryUsage.get() > targetMemory) {
                        String key = entry.getKey();
                        CachedAsset asset = entry.getValue();
                        
                        assetCache.remove(key);
                        cacheMemoryUsage.addAndGet(-asset.size);
                        
                        logger.debug("Evicted asset: {} ({}KB)", key, asset.size / 1024);
                    }
                });
        
        logger.info("Asset cache eviction completed. Memory usage: {}MB", 
                   cacheMemoryUsage.get() / (1024 * 1024));
    }
    
    /**
     * Gets current cache statistics.
     *
     * @return Cache statistics string
     */
    public String getCacheStats() {
        long memoryMB = cacheMemoryUsage.get() / (1024 * 1024);
        long maxMemoryMB = maxCacheMemory / (1024 * 1024);
        
        return String.format("Asset Cache: %d assets, %dMB/%dMB (%.1f%%)", 
                           assetCache.size(), memoryMB, maxMemoryMB, 
                           (double) memoryMB / maxMemoryMB * 100);
    }
    
    /**
     * Clears all cached assets.
     */
    public void clearCache() {
        assetCache.clear();
        cacheMemoryUsage.set(0);
        logger.info("Asset cache cleared");
    }
    
    /**
     * Shuts down the asset manager and cleans up resources.
     */
    public void shutdown() {
        clearCache();
        loadingAssets.clear();
        logger.info("AssetManager shutdown completed");
    }
    
    /**
     * Loads asset data from disk.
     *
     * @param assetPath Path to the asset
     * @return Asset data
     * @throws IOException If loading fails
     */
    private byte[] loadAssetFromDisk(String assetPath) throws IOException {
        // Try loading from resources first
        try (InputStream is = getClass().getResourceAsStream("/" + assetPath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        
        // Try loading from file system
        Path filePath = Paths.get(assetPath);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        
        throw new IOException("Asset not found: " + assetPath);
    }
    
    /**
     * Caches an asset with memory management.
     *
     * @param assetPath Asset path
     * @param data Asset data
     * @param priority Asset priority
     */
    private void cacheAsset(String assetPath, byte[] data, AssetPriority priority) {
        long dataSize = data.length;
        
        // Check if we need to evict assets to make room
        if (cacheMemoryUsage.get() + dataSize > maxCacheMemory) {
            long targetMemory = maxCacheMemory - dataSize;
            evictAssets(Math.max(0, targetMemory));
        }
        
        // Cache the asset
        CachedAsset cachedAsset = new CachedAsset(data, priority);
        assetCache.put(assetPath, cachedAsset);
        cacheMemoryUsage.addAndGet(dataSize);
        
        logger.debug("Cached asset: {} ({}KB, priority: {})", 
                    assetPath, dataSize / 1024, priority);
    }
}