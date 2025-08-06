package com.odyssey.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Generic object pool for memory management in The Odyssey.
 * Provides thread-safe pooling of frequently allocated objects to reduce GC pressure.
 * 
 * @param <T> The type of objects to pool
 */
public class ObjectPool<T> {
    private static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);
    
    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> factory;
    private final PoolableObject.Resetter<T> resetter;
    private final int maxSize;
    private final AtomicInteger currentSize;
    private final AtomicInteger totalCreated;
    private final AtomicInteger totalAcquired;
    private final AtomicInteger totalReleased;
    private final String poolName;
    
    /**
     * Create a new object pool
     * 
     * @param poolName Name for debugging and logging
     * @param factory Factory function to create new objects
     * @param resetter Function to reset objects when returned to pool
     * @param maxSize Maximum number of objects to keep in pool
     */
    public ObjectPool(String poolName, Supplier<T> factory, PoolableObject.Resetter<T> resetter, int maxSize) {
        this.poolName = poolName;
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
        this.pool = new ConcurrentLinkedQueue<>();
        this.currentSize = new AtomicInteger(0);
        this.totalCreated = new AtomicInteger(0);
        this.totalAcquired = new AtomicInteger(0);
        this.totalReleased = new AtomicInteger(0);
        
        logger.debug("Created object pool '{}' with max size: {}", poolName, maxSize);
    }
    
    /**
     * Acquire an object from the pool, creating a new one if necessary
     */
    public T acquire() {
        T object = pool.poll();
        
        if (object == null) {
            // Create new object
            object = factory.get();
            totalCreated.incrementAndGet();
            logger.trace("Created new object for pool '{}'", poolName);
        } else {
            currentSize.decrementAndGet();
            logger.trace("Reused object from pool '{}'", poolName);
        }
        
        totalAcquired.incrementAndGet();
        return object;
    }
    
    /**
     * Release an object back to the pool
     */
    public void release(T object) {
        if (object == null) {
            return;
        }
        
        totalReleased.incrementAndGet();
        
        // Reset the object to clean state
        if (resetter != null) {
            resetter.reset(object);
        }
        
        // Only add back to pool if we haven't exceeded max size
        if (currentSize.get() < maxSize) {
            pool.offer(object);
            currentSize.incrementAndGet();
            logger.trace("Returned object to pool '{}'", poolName);
        } else {
            logger.trace("Pool '{}' at max capacity, discarding object", poolName);
        }
    }
    
    /**
     * Clear all objects from the pool
     */
    public void clear() {
        pool.clear();
        currentSize.set(0);
        logger.debug("Cleared pool '{}'", poolName);
    }
    
    /**
     * Get pool statistics
     */
    public PoolStats getStats() {
        return new PoolStats(
            poolName,
            currentSize.get(),
            maxSize,
            totalCreated.get(),
            totalAcquired.get(),
            totalReleased.get()
        );
    }
    
    /**
     * Pool statistics for monitoring and debugging
     */
    public static class PoolStats {
        public final String name;
        public final int currentSize;
        public final int maxSize;
        public final int totalCreated;
        public final int totalAcquired;
        public final int totalReleased;
        public final double hitRate;
        
        public PoolStats(String name, int currentSize, int maxSize, int totalCreated, int totalAcquired, int totalReleased) {
            this.name = name;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalCreated = totalCreated;
            this.totalAcquired = totalAcquired;
            this.totalReleased = totalReleased;
            this.hitRate = totalAcquired > 0 ? (double) (totalAcquired - totalCreated) / totalAcquired : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("Pool[%s]: size=%d/%d, created=%d, acquired=%d, released=%d, hit_rate=%.2f%%",
                name, currentSize, maxSize, totalCreated, totalAcquired, totalReleased, hitRate * 100);
        }
    }
}