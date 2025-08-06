package com.odyssey.core.memory;

/**
 * Interface for objects that can be pooled for memory management.
 * Objects implementing this interface can be safely reused from object pools.
 */
public interface PoolableObject {
    
    /**
     * Reset the object to its initial state for reuse.
     * This method should clear all state and prepare the object for reuse.
     */
    void reset();
    
    /**
     * Functional interface for resetting poolable objects
     */
    @FunctionalInterface
    interface Resetter<T> {
        void reset(T object);
    }
}