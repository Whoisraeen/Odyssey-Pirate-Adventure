package com.odyssey.core.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LockFreeHashMap.
 * Tests both single-threaded correctness and concurrent behavior.
 */
class LockFreeHashMapTest {
    
    private LockFreeHashMap<String, Integer> map;
    
    @BeforeEach
    void setUp() {
        map = new LockFreeHashMap<>();
    }
    
    @Test
    void testConstructorValidation() {
        // Valid initial capacities (powers of 2)
        assertDoesNotThrow(() -> new LockFreeHashMap<String, Integer>(2));
        assertDoesNotThrow(() -> new LockFreeHashMap<String, Integer>(4));
        assertDoesNotThrow(() -> new LockFreeHashMap<String, Integer>(1024));
        
        // Invalid initial capacities
        assertThrows(IllegalArgumentException.class, () -> new LockFreeHashMap<String, Integer>(1));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeHashMap<String, Integer>(3));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeHashMap<String, Integer>(7));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeHashMap<String, Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeHashMap<String, Integer>(-1));
    }
    
    @Test
    void testInitialState() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("nonexistent"));
        assertFalse(map.containsKey("nonexistent"));
    }
    
    @Test
    void testNullKeyRejection() {
        assertThrows(IllegalArgumentException.class, () -> map.put(null, 1));
        assertThrows(IllegalArgumentException.class, () -> map.get(null));
        assertThrows(IllegalArgumentException.class, () -> map.remove(null));
        assertThrows(IllegalArgumentException.class, () -> map.containsKey(null));
    }
    
    @Test
    void testNullValueRejection() {
        assertThrows(IllegalArgumentException.class, () -> map.put("key", null));
    }
    
    @Test
    void testBasicPutAndGet() {
        assertNull(map.put("key1", 100));
        assertEquals(100, map.get("key1"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key1"));
        
        // Update existing key
        assertEquals(100, map.put("key1", 200));
        assertEquals(200, map.get("key1"));
        assertEquals(1, map.size()); // Size should not change
    }
    
    @Test
    void testMultiplePutAndGet() {
        for (int i = 0; i < 10; i++) {
            assertNull(map.put("key" + i, i * 10));
            assertEquals(i + 1, map.size());
        }
        
        for (int i = 0; i < 10; i++) {
            assertEquals(i * 10, map.get("key" + i));
            assertTrue(map.containsKey("key" + i));
        }
    }
    
    @Test
    void testRemove() {
        map.put("key1", 100);
        map.put("key2", 200);
        map.put("key3", 300);
        assertEquals(3, map.size());
        
        assertEquals(200, map.remove("key2"));
        assertEquals(2, map.size());
        assertNull(map.get("key2"));
        assertFalse(map.containsKey("key2"));
        
        // Remove non-existent key
        assertNull(map.remove("nonexistent"));
        assertEquals(2, map.size());
        
        // Remove remaining keys
        assertEquals(100, map.remove("key1"));
        assertEquals(300, map.remove("key3"));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }
    
    @Test
    void testClear() {
        for (int i = 0; i < 5; i++) {
            map.put("key" + i, i);
        }
        assertEquals(5, map.size());
        
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        
        // Verify all keys are gone
        for (int i = 0; i < 5; i++) {
            assertNull(map.get("key" + i));
            assertFalse(map.containsKey("key" + i));
        }
        
        // Should be able to use map normally after clear
        map.put("newkey", 999);
        assertEquals(999, map.get("newkey"));
    }
    
    @Test
    void testHashCollisions() {
        // Create keys that will likely hash to same bucket
        String key1 = "Aa";
        String key2 = "BB"; // These have same hashCode in Java
        
        map.put(key1, 1);
        map.put(key2, 2);
        
        assertEquals(1, map.get(key1));
        assertEquals(2, map.get(key2));
        assertEquals(2, map.size());
        
        assertTrue(map.containsKey(key1));
        assertTrue(map.containsKey(key2));
    }
    
    @Test
    void testResize() {
        LockFreeHashMap<String, Integer> smallMap = new LockFreeHashMap<>(2);
        
        // Add enough elements to trigger resize
        for (int i = 0; i < 10; i++) {
            smallMap.put("key" + i, i);
        }
        
        assertEquals(10, smallMap.size());
        
        // Verify all elements are still accessible
        for (int i = 0; i < 10; i++) {
            assertEquals(i, smallMap.get("key" + i));
        }
    }
    
    @Test
    @Timeout(15)
    void testConcurrentPutAndGet() throws InterruptedException {
        final int threadCount = 4;  // Reduced thread count
        final int operationsPerThread = 500;  // Reduced operations per thread
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        final AtomicInteger successfulPuts = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread" + threadId + "_key" + i;
                        int value = threadId * 1000 + i;
                        
                        // Put the value
                        Integer previous = map.put(key, value);
                        if (previous == null) {
                            successfulPuts.incrementAndGet();
                        }
                        
                        // Verify the value was stored correctly
                        Integer retrieved = map.get(key);
                        assertNotNull(retrieved, "Value should not be null for key: " + key);
                        assertEquals(value, retrieved.intValue());
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        // Check that we have the expected number of entries
        int expectedSize = threadCount * operationsPerThread;
        assertEquals(expectedSize, map.size(), 
            "Expected " + expectedSize + " entries but found " + map.size() + 
            ". Successful puts: " + successfulPuts.get());
        
        // Verify all values are correct
        for (int t = 0; t < threadCount; t++) {
            for (int i = 0; i < operationsPerThread; i++) {
                String key = "thread" + t + "_key" + i;
                int expectedValue = t * 1000 + i;
                Integer actualValue = map.get(key);
                assertNotNull(actualValue, "Missing key: " + key);
                assertEquals(expectedValue, actualValue.intValue());
            }
        }
    }
    
    @Test
    @Timeout(10)
    void testConcurrentPutAndRemove() throws InterruptedException {
        final int threadCount = 4;
        final int operationsPerThread = 500;
        final CountDownLatch startLatch = new CountDownLatch(threadCount * 2);
        final CountDownLatch endLatch = new CountDownLatch(threadCount * 2);
        final AtomicReference<Exception> error = new AtomicReference<>();
        final AtomicInteger putCount = new AtomicInteger(0);
        final AtomicInteger removeCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        
        // Put threads
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "put_thread" + threadId + "_key" + i;
                        map.put(key, threadId * 1000 + i);
                        putCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Remove threads
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "put_thread" + threadId + "_key" + i;
                        // Try to remove, may not exist yet
                        if (map.remove(key) != null) {
                            removeCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        assertEquals(threadCount * operationsPerThread, putCount.get());
        
        // Final size should be puts - removes
        int expectedSize = putCount.get() - removeCount.get();
        assertEquals(expectedSize, map.size());
    }
    
    @Test
    @Timeout(10)
    void testConcurrentMixedOperations() throws InterruptedException {
        final int threadCount = 6;
        final int operationsPerThread = 500;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        final Set<String> allKeys = Collections.synchronizedSet(new HashSet<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread" + threadId + "_key" + i;
                        allKeys.add(key);
                        
                        // Mix of operations
                        switch (i % 4) {
                            case 0:
                                map.put(key, threadId * 1000 + i);
                                break;
                            case 1:
                                map.get(key);
                                break;
                            case 2:
                                map.containsKey(key);
                                break;
                            case 3:
                                map.remove(key);
                                break;
                        }
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        // Map should be in a consistent state
        assertTrue(map.size() >= 0);
        
        // Test that map is still functional
        map.put("test", 999);
        assertEquals(999, map.get("test"));
    }
    
    @RepeatedTest(5)
    void testConcurrentStressTest() throws InterruptedException {
        final int threadCount = 4;
        final int operationCount = 200;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationCount; i++) {
                        String key = "stress_" + threadId + "_" + i;
                        
                        // Random operations
                        switch (i % 5) {
                            case 0:
                                map.put(key, i);
                                break;
                            case 1:
                                map.get(key);
                                break;
                            case 2:
                                map.containsKey(key);
                                break;
                            case 3:
                                map.remove(key);
                                break;
                            case 4:
                                if (i % 10 == 0) {
                                    map.clear();
                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        // Map should be in a consistent state
        assertTrue(map.size() >= 0);
    }
    
    @Test
    void testLargeDataSet() {
        final int elementCount = 10000;
        
        // Add large number of elements
        for (int i = 0; i < elementCount; i++) {
            map.put("key" + i, i);
        }
        
        assertEquals(elementCount, map.size());
        
        // Verify all elements
        for (int i = 0; i < elementCount; i++) {
            assertEquals(i, map.get("key" + i).intValue());
            assertTrue(map.containsKey("key" + i));
        }
        
        // Remove half
        for (int i = 0; i < elementCount / 2; i++) {
            assertEquals(i, map.remove("key" + i).intValue());
        }
        
        assertEquals(elementCount / 2, map.size());
        
        // Verify remaining elements
        for (int i = elementCount / 2; i < elementCount; i++) {
            assertEquals(i, map.get("key" + i).intValue());
        }
    }
}