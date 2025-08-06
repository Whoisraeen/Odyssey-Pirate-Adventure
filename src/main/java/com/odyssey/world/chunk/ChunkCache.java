package com.odyssey.world.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe LRU (Least Recently Used) cache for chunks that automatically
 * manages memory usage by evicting the least recently accessed chunks when
 * the cache reaches its capacity limit.
 * 
 * <p>Key features:
 * <ul>
 *   <li>LRU eviction policy to maintain optimal memory usage</li>
 *   <li>Thread-safe operations with minimal contention</li>
 *   <li>Configurable cache size limits</li>
 *   <li>Automatic chunk serialization on eviction</li>
 *   <li>Cache statistics and monitoring</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ChunkCache {
    private static final Logger logger = LoggerFactory.getLogger(ChunkCache.class);
    
    /** Default maximum number of chunks to keep in memory. */
    public static final int DEFAULT_MAX_CHUNKS = 1024;
    
    /** Default maximum memory usage in bytes (256 MB). */
    public static final long DEFAULT_MAX_MEMORY = 256L * 1024 * 1024;
    
    private final int maxChunks;
    private final long maxMemory;
    private final ChunkSerializer serializer;
    
    // Thread-safe LRU cache implementation
    private final Map<ChunkPosition, Chunk> cache;
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Cache statistics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);
    
    /**
     * Creates a new chunk cache with default settings.
     */
    public ChunkCache() {
        this(DEFAULT_MAX_CHUNKS, DEFAULT_MAX_MEMORY, new ChunkSerializer());
    }
    
    /**
     * Creates a new chunk cache with the specified settings.
     * 
     * @param maxChunks the maximum number of chunks to keep in memory
     * @param maxMemory the maximum memory usage in bytes
     * @param serializer the chunk serializer for disk operations
     */
    public ChunkCache(int maxChunks, long maxMemory, ChunkSerializer serializer) {
        if (maxChunks <= 0) {
            throw new IllegalArgumentException("Max chunks must be positive");
        }
        if (maxMemory <= 0) {
            throw new IllegalArgumentException("Max memory must be positive");
        }
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer cannot be null");
        }
        
        this.maxChunks = maxChunks;
        this.maxMemory = maxMemory;
        this.serializer = serializer;
        
        // Create LRU cache with access-order LinkedHashMap
        this.cache = new LinkedHashMap<ChunkPosition, Chunk>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ChunkPosition, Chunk> eldest) {
                return size() > maxChunks || getCurrentMemoryUsage() > maxMemory;
            }
        };
    }
    
    /**
     * Gets a chunk from the cache.
     * 
     * @param position the chunk position
     * @return the chunk, or null if not in cache
     */
    public Chunk get(ChunkPosition position) {
        if (position == null) {
            return null;
        }
        
        cacheLock.readLock().lock();
        try {
            Chunk chunk = cache.get(position);
            if (chunk != null) {
                hits.incrementAndGet();
                return chunk;
            } else {
                misses.incrementAndGet();
                return null;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Puts a chunk into the cache.
     * 
     * @param position the chunk position
     * @param chunk the chunk to cache
     * @return the previous chunk at this position, or null if none
     */
    public Chunk put(ChunkPosition position, Chunk chunk) {
        if (position == null || chunk == null) {
            throw new IllegalArgumentException("Position and chunk cannot be null");
        }
        
        cacheLock.writeLock().lock();
        try {
            // Check if we need to evict chunks before adding
            while ((cache.size() >= maxChunks || getCurrentMemoryUsage() > maxMemory) && !cache.isEmpty()) {
                evictLeastRecentlyUsed();
            }
            
            Chunk previous = cache.put(position, chunk);
            updateMemoryUsage();
            
            if (previous != null) {
                logger.debug("Replaced chunk at position {}", position);
            } else {
                logger.debug("Cached new chunk at position {}", position);
            }
            
            return previous;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Removes a chunk from the cache.
     * 
     * @param position the chunk position
     * @return the removed chunk, or null if not in cache
     */
    public Chunk remove(ChunkPosition position) {
        if (position == null) {
            return null;
        }
        
        cacheLock.writeLock().lock();
        try {
            Chunk removed = cache.remove(position);
            if (removed != null) {
                updateMemoryUsage();
                logger.debug("Removed chunk at position {} from cache", position);
            }
            return removed;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if the cache contains a chunk at the specified position.
     * 
     * @param position the chunk position
     * @return true if the cache contains the chunk
     */
    public boolean contains(ChunkPosition position) {
        if (position == null) {
            return false;
        }
        
        cacheLock.readLock().lock();
        try {
            return cache.containsKey(position);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the number of chunks currently in the cache.
     * 
     * @return the cache size
     */
    public int size() {
        cacheLock.readLock().lock();
        try {
            return cache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Clears all chunks from the cache, optionally saving them to disk.
     * 
     * @param saveToDisk whether to save chunks to disk before clearing
     */
    public void clear(boolean saveToDisk) {
        cacheLock.writeLock().lock();
        try {
            if (saveToDisk) {
                for (Map.Entry<ChunkPosition, Chunk> entry : cache.entrySet()) {
                    try {
                        serializer.saveChunk(entry.getValue());
                    } catch (Exception e) {
                        logger.error("Failed to save chunk {} during cache clear", entry.getKey(), e);
                    }
                }
            }
            
            cache.clear();
            currentMemoryUsage.set(0);
            logger.info("Cleared chunk cache");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Evicts the least recently used chunk from the cache.
     */
    private void evictLeastRecentlyUsed() {
        if (cache.isEmpty()) {
            return;
        }
        
        // Get the first entry (least recently used)
        Map.Entry<ChunkPosition, Chunk> eldest = cache.entrySet().iterator().next();
        ChunkPosition position = eldest.getKey();
        Chunk chunk = eldest.getValue();
        
        // Save to disk if dirty
        if (chunk.isDirty()) {
            try {
                serializer.saveChunk(chunk);
                chunk.markClean();
            } catch (Exception e) {
                logger.error("Failed to save chunk {} during eviction", position, e);
            }
        }
        
        // Remove from cache
        cache.remove(position);
        evictions.incrementAndGet();
        
        logger.debug("Evicted chunk at position {} from cache", position);
    }
    
    /**
     * Updates the current memory usage calculation.
     */
    private void updateMemoryUsage() {
        long totalMemory = 0;
        for (Chunk chunk : cache.values()) {
            if (chunk instanceof PalettizedChunk) {
                totalMemory += ((PalettizedChunk) chunk).getMemoryUsage();
            } else {
                // Estimate memory usage for regular chunks
                totalMemory += estimateChunkMemory(chunk);
            }
        }
        currentMemoryUsage.set(totalMemory);
    }
    
    /**
     * Estimates the memory usage of a regular chunk.
     * 
     * @param chunk the chunk to estimate
     * @return the estimated memory usage in bytes
     */
    private long estimateChunkMemory(Chunk chunk) {
        // Rough estimate: 16x16x256 blocks * 4 bytes per block reference + overhead
        return (long) Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * Chunk.CHUNK_HEIGHT * 4 + 1024;
    }
    
    /**
     * Gets the current memory usage of the cache.
     * 
     * @return the current memory usage in bytes
     */
    public long getCurrentMemoryUsage() {
        return currentMemoryUsage.get();
    }
    
    /**
     * Gets the maximum memory limit of the cache.
     * 
     * @return the maximum memory in bytes
     */
    public long getMaxMemory() {
        return maxMemory;
    }
    
    /**
     * Gets the maximum number of chunks the cache can hold.
     * 
     * @return the maximum chunk count
     */
    public int getMaxChunks() {
        return maxChunks;
    }
    
    /**
     * Gets the cache hit rate as a percentage.
     * 
     * @return the hit rate (0.0 to 1.0)
     */
    public double getHitRate() {
        long totalRequests = hits.get() + misses.get();
        return totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;
    }
    
    /**
     * Gets the number of cache hits.
     * 
     * @return the hit count
     */
    public long getHits() {
        return hits.get();
    }
    
    /**
     * Gets the number of cache misses.
     * 
     * @return the miss count
     */
    public long getMisses() {
        return misses.get();
    }
    
    /**
     * Gets the number of evictions.
     * 
     * @return the eviction count
     */
    public long getEvictions() {
        return evictions.get();
    }
    
    /**
     * Resets all cache statistics.
     */
    public void resetStatistics() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
    }
    
    /**
     * Gets detailed cache statistics.
     * 
     * @return cache statistics
     */
    public CacheStats getStats() {
        cacheLock.readLock().lock();
        try {
            return new CacheStats(
                cache.size(),
                maxChunks,
                getCurrentMemoryUsage(),
                maxMemory,
                hits.get(),
                misses.get(),
                evictions.get(),
                getHitRate()
            );
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Gets cache statistics as a map for integration with other systems.
     * 
     * @return map containing cache statistics
     */
    public Map<String, Object> getStatistics() {
        CacheStats stats = getStats();
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("currentSize", stats.currentSize);
        result.put("maxSize", stats.maxSize);
        result.put("currentMemory", stats.currentMemory);
        result.put("maxMemory", stats.maxMemory);
        result.put("hits", stats.hits);
        result.put("misses", stats.misses);
        result.put("evictions", stats.evictions);
        result.put("hitRate", stats.hitRate);
        return result;
    }
    
    /**
     * Performs cleanup operations on the cache.
     * Saves all dirty chunks and clears the cache.
     */
    public void cleanup() {
        clear(true); // Save chunks to disk during cleanup
    }
    
    /**
     * Cache statistics data class.
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final long currentMemory;
        public final long maxMemory;
        public final long hits;
        public final long misses;
        public final long evictions;
        public final double hitRate;
        
        CacheStats(int currentSize, int maxSize, long currentMemory, long maxMemory,
                  long hits, long misses, long evictions, double hitRate) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.currentMemory = currentMemory;
            this.maxMemory = maxMemory;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{size=%d/%d, memory=%d/%d MB, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%}",
                currentSize, maxSize,
                currentMemory / (1024 * 1024), maxMemory / (1024 * 1024),
                hits, misses, evictions, hitRate * 100
            );
        }
    }
}