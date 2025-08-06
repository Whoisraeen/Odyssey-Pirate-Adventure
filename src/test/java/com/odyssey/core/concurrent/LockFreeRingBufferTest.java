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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LockFreeRingBuffer.
 * Tests both single-threaded correctness and concurrent behavior.
 */
class LockFreeRingBufferTest {
    
    private LockFreeRingBuffer<Integer> buffer;
    
    @BeforeEach
    void setUp() {
        buffer = new LockFreeRingBuffer<>(8); // Power of 2 capacity
    }
    
    @Test
    void testConstructorValidation() {
        // Valid capacities (powers of 2)
        assertDoesNotThrow(() -> new LockFreeRingBuffer<Integer>(2));
        assertDoesNotThrow(() -> new LockFreeRingBuffer<Integer>(4));
        assertDoesNotThrow(() -> new LockFreeRingBuffer<Integer>(1024));
        
        // Invalid capacities
        assertThrows(IllegalArgumentException.class, () -> new LockFreeRingBuffer<Integer>(1));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeRingBuffer<Integer>(3));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeRingBuffer<Integer>(7));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeRingBuffer<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new LockFreeRingBuffer<Integer>(-1));
    }
    
    @Test
    void testInitialState() {
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(0, buffer.size());
        assertEquals(8, buffer.getCapacity());
        assertEquals(0.0, buffer.getUtilization(), 0.001);
        assertNull(buffer.poll());
    }
    
    @Test
    void testOfferAndPoll() {
        // Test basic offer/poll
        assertTrue(buffer.offer(1));
        assertFalse(buffer.isEmpty());
        assertEquals(1, buffer.size());
        
        Integer value = buffer.poll();
        assertEquals(1, value);
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
    }
    
    @Test
    void testNullOfferRejection() {
        assertThrows(IllegalArgumentException.class, () -> buffer.offer(null));
    }
    
    @Test
    void testFillToCapacity() {
        // Fill buffer to capacity
        for (int i = 0; i < 8; i++) {
            assertTrue(buffer.offer(i));
            assertEquals(i + 1, buffer.size());
        }
        
        assertTrue(buffer.isFull());
        assertEquals(1.0, buffer.getUtilization(), 0.001);
        
        // Should reject additional offers
        assertFalse(buffer.offer(8));
        assertEquals(8, buffer.size());
    }
    
    @Test
    void testDrainBuffer() {
        // Fill buffer
        for (int i = 0; i < 8; i++) {
            buffer.offer(i);
        }
        
        // Drain buffer
        for (int i = 0; i < 8; i++) {
            Integer value = buffer.poll();
            assertEquals(i, value);
            assertEquals(7 - i, buffer.size());
        }
        
        assertTrue(buffer.isEmpty());
        assertNull(buffer.poll());
    }
    
    @Test
    void testFIFOOrdering() {
        // Test that elements come out in FIFO order
        for (int i = 0; i < 5; i++) {
            buffer.offer(i * 10);
        }
        
        for (int i = 0; i < 5; i++) {
            assertEquals(i * 10, buffer.poll());
        }
    }
    
    @Test
    void testWrapAround() {
        // Test buffer wrap-around behavior
        for (int cycle = 0; cycle < 3; cycle++) {
            // Fill buffer
            for (int i = 0; i < 8; i++) {
                assertTrue(buffer.offer(cycle * 100 + i));
            }
            
            // Drain buffer
            for (int i = 0; i < 8; i++) {
                assertEquals(cycle * 100 + i, buffer.poll());
            }
        }
    }
    
    @Test
    void testPartialFillAndDrain() {
        // Test partial operations
        for (int i = 0; i < 3; i++) {
            buffer.offer(i);
        }
        assertEquals(3, buffer.size());
        
        assertEquals(0, buffer.poll());
        assertEquals(1, buffer.poll());
        assertEquals(1, buffer.size());
        
        buffer.offer(10);
        buffer.offer(11);
        assertEquals(3, buffer.size());
        
        assertEquals(2, buffer.poll());
        assertEquals(10, buffer.poll());
        assertEquals(11, buffer.poll());
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    void testClear() {
        // Fill buffer partially
        for (int i = 0; i < 5; i++) {
            buffer.offer(i);
        }
        
        buffer.clear();
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertNull(buffer.poll());
        
        // Should be able to use buffer normally after clear
        assertTrue(buffer.offer(100));
        assertEquals(100, buffer.poll());
    }
    
    @Test
    @Timeout(10)
    void testSingleProducerSingleConsumer() throws InterruptedException {
        final int messageCount = 10000;
        final CountDownLatch startLatch = new CountDownLatch(2);
        final CountDownLatch endLatch = new CountDownLatch(2);
        final AtomicInteger producedCount = new AtomicInteger(0);
        final AtomicInteger consumedCount = new AtomicInteger(0);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                startLatch.countDown();
                startLatch.await();
                
                for (int i = 0; i < messageCount; i++) {
                    while (!buffer.offer(i)) {
                        Thread.yield(); // Wait for space
                    }
                    producedCount.incrementAndGet();
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                endLatch.countDown();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                startLatch.countDown();
                startLatch.await();
                
                int expected = 0;
                while (expected < messageCount) {
                    Integer value = buffer.poll();
                    if (value != null) {
                        assertEquals(expected, value.intValue());
                        expected++;
                        consumedCount.incrementAndGet();
                    } else {
                        Thread.yield(); // Wait for data
                    }
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                endLatch.countDown();
            }
        });
        
        producer.start();
        consumer.start();
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        assertEquals(messageCount, producedCount.get());
        assertEquals(messageCount, consumedCount.get());
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    @Timeout(10)
    void testMultipleProducersMultipleConsumers() throws InterruptedException {
        final int producerCount = 2;
        final int consumerCount = 2;
        final int messagesPerProducer = 10;
        final int totalMessages = producerCount * messagesPerProducer;
        
        final CountDownLatch startLatch = new CountDownLatch(producerCount + consumerCount);
        final CountDownLatch endLatch = new CountDownLatch(producerCount + consumerCount);
        final AtomicInteger producedCount = new AtomicInteger(0);
        final AtomicInteger consumedCount = new AtomicInteger(0);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);
        
        // Start producers
        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < messagesPerProducer; i++) {
                        int value = producerId * messagesPerProducer + i;
                        while (!buffer.offer(value)) {
                            Thread.yield();
                        }
                        producedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Start consumers
        for (int c = 0; c < consumerCount; c++) {
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    while (true) {
                        Integer value = buffer.poll();
                        if (value != null) {
                            if (consumedCount.incrementAndGet() >= totalMessages) {
                                break;
                            }
                        } else {
                            // Check if all messages have been consumed
                            if (consumedCount.get() >= totalMessages) {
                                break;
                            }
                            Thread.yield();
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
        
        assertEquals(totalMessages, producedCount.get());
        assertEquals(totalMessages, consumedCount.get());
    }
    
    @RepeatedTest(10)
    void testConcurrentStressTest() throws InterruptedException {
        final int operationCount = 1000;
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        Thread thread1 = new Thread(() -> {
            try {
                for (int i = 0; i < operationCount; i++) {
                    if (i % 2 == 0) {
                        buffer.offer(i);
                    } else {
                        buffer.poll();
                    }
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                for (int i = 0; i < operationCount; i++) {
                    if (i % 3 == 0) {
                        buffer.offer(i + 10000);
                    } else {
                        buffer.poll();
                    }
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        thread1.start();
        thread2.start();
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        // Buffer should be in a consistent state
        assertTrue(buffer.size() >= 0);
        assertTrue(buffer.size() <= buffer.getCapacity());
    }
}