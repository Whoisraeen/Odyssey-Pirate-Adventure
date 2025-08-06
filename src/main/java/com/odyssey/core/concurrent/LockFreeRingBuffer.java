package com.odyssey.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A high-performance lock-free ring buffer for single-producer, single-consumer scenarios.
 * This implementation uses memory barriers and atomic operations to ensure thread safety
 * without locks, making it ideal for high-frequency inter-thread communication.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Lock-free implementation using atomic operations</li>
 *   <li>Memory-efficient circular buffer design</li>
 *   <li>Cache-friendly memory layout with padding to avoid false sharing</li>
 *   <li>Wait-free operations for both producer and consumer</li>
 * </ul>
 * 
 * @param <T> the type of elements stored in the buffer
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class LockFreeRingBuffer<T> {
    
    // Cache line padding to prevent false sharing
    private static final int CACHE_LINE_SIZE = 64;
    private static final int PADDING_SIZE = CACHE_LINE_SIZE / Long.BYTES;
    
    // Buffer storage
    private final Object[] buffer;
    private final int capacity;
    private final int mask;
    
    // Producer sequence (write position)
    private volatile long writeSequence = 0;
    @SuppressWarnings("unused")
    private final long[] writeSequencePadding = new long[PADDING_SIZE];
    
    // Consumer sequence (read position)
    private volatile long readSequence = 0;
    @SuppressWarnings("unused")
    private final long[] readSequencePadding = new long[PADDING_SIZE];
    
    // Cached sequences to reduce volatile reads
    private long cachedReadSequence = 0;
    private long cachedWriteSequence = 0;
    
    /**
     * Creates a new lock-free ring buffer with the specified capacity.
     * The capacity must be a power of 2 for optimal performance.
     * 
     * @param capacity the buffer capacity (must be power of 2)
     * @throws IllegalArgumentException if capacity is not a power of 2 or is less than 2
     */
    public LockFreeRingBuffer(int capacity) {
        if (capacity < 2 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity must be a power of 2 and >= 2");
        }
        
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.buffer = new Object[capacity];
    }
    
    /**
     * Attempts to offer an element to the buffer.
     * This method is wait-free and will return immediately.
     * 
     * @param element the element to offer
     * @return true if the element was successfully added, false if buffer is full
     */
    public boolean offer(T element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        
        final long currentWriteSequence = writeSequence;
        final long nextWriteSequence = currentWriteSequence + 1;
        
        // Check if buffer is full by comparing with cached read sequence
        if (nextWriteSequence - cachedReadSequence > capacity) {
            // Update cached read sequence and check again
            cachedReadSequence = readSequence;
            if (nextWriteSequence - cachedReadSequence > capacity) {
                return false; // Buffer is full
            }
        }
        
        // Store element and advance write sequence
        buffer[(int) currentWriteSequence & mask] = element;
        
        // Memory barrier to ensure element is visible before sequence update
        writeSequence = nextWriteSequence;
        
        return true;
    }
    
    /**
     * Attempts to poll an element from the buffer.
     * This method is wait-free and will return immediately.
     * 
     * @return the next element, or null if buffer is empty
     */
    @SuppressWarnings("unchecked")
    public T poll() {
        final long currentReadSequence = readSequence;
        
        // Check if buffer is empty by comparing with cached write sequence
        if (currentReadSequence >= cachedWriteSequence) {
            // Update cached write sequence and check again
            cachedWriteSequence = writeSequence;
            if (currentReadSequence >= cachedWriteSequence) {
                return null; // Buffer is empty
            }
        }
        
        // Read element and advance read sequence
        final T element = (T) buffer[(int) currentReadSequence & mask];
        buffer[(int) currentReadSequence & mask] = null; // Help GC
        
        // Memory barrier to ensure element read before sequence update
        readSequence = currentReadSequence + 1;
        
        return element;
    }
    
    /**
     * Returns the current number of elements in the buffer.
     * This is an approximation due to concurrent access.
     * 
     * @return approximate number of elements
     */
    public int size() {
        return (int) Math.max(0, writeSequence - readSequence);
    }
    
    /**
     * Returns whether the buffer appears empty.
     * This is an approximation due to concurrent access.
     * 
     * @return true if buffer appears empty
     */
    public boolean isEmpty() {
        return readSequence >= writeSequence;
    }
    
    /**
     * Returns whether the buffer appears full.
     * This is an approximation due to concurrent access.
     * 
     * @return true if buffer appears full
     */
    public boolean isFull() {
        return writeSequence - readSequence >= capacity;
    }
    
    /**
     * Returns the buffer capacity.
     * 
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Clears all elements from the buffer.
     * This operation is not thread-safe and should only be called
     * when no other threads are accessing the buffer.
     */
    public void clear() {
        while (poll() != null) {
            // Clear all elements
        }
    }
    
    /**
     * Returns buffer utilization as a percentage.
     * 
     * @return utilization percentage (0.0 to 1.0)
     */
    public double getUtilization() {
        return (double) size() / capacity;
    }
}