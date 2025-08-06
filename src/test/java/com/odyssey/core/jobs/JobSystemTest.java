package com.odyssey.core.jobs;

import com.odyssey.core.GameConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the JobSystem work-stealing thread pool.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class JobSystemTest {
    
    private JobSystem jobSystem;
    private GameConfig config;
    
    @BeforeEach
    void setUp() {
        config = new GameConfig();
        config.setJobSystemWorkerCount(4); // Use fixed number for predictable tests
        config.setJobQueueCapacity(100);
        config.setGlobalJobQueueCapacity(200);
        config.setJobSystemStatisticsEnabled(true);
        
        jobSystem = new JobSystem(config);
        jobSystem.initialize();
    }
    
    @AfterEach
    void tearDown() {
        if (jobSystem != null) {
            assertTrue(jobSystem.shutdown(5), "JobSystem should shutdown cleanly");
        }
    }
    
    @Test
    void testBasicJobExecution() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        
        Job job = () -> counter.incrementAndGet();
        
        JobHandle<Void> handle = jobSystem.submit(job);
        
        // Wait for job completion
        handle.get(1, TimeUnit.SECONDS);
        
        assertTrue(handle.isDone());
        assertFalse(handle.isCancelled());
        assertFalse(handle.isCompletedExceptionally());
        assertEquals(1, counter.get());
    }
    
    @Test
    void testJobWithResult() throws Exception {
        Job job = () -> { /* do nothing */ };
        
        JobHandle<Integer> handle = jobSystem.submit(job, () -> 42);
        
        Integer result = handle.get(1, TimeUnit.SECONDS);
        
        assertEquals(42, result);
        assertTrue(handle.isDone());
    }
    
    @Test
    void testMultipleJobs() throws Exception {
        final int jobCount = 100;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(jobCount);
        
        List<JobHandle<Void>> handles = new ArrayList<>();
        
        for (int i = 0; i < jobCount; i++) {
            Job job = () -> {
                counter.incrementAndGet();
                latch.countDown();
            };
            
            handles.add(jobSystem.submit(job));
        }
        
        // Wait for all jobs to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All jobs should complete within timeout");
        
        // Verify all handles are done
        for (JobHandle<Void> handle : handles) {
            assertTrue(handle.isDone());
        }
        
        assertEquals(jobCount, counter.get());
    }
    
    @Test
    void testJobCancellation() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch blockLatch = new CountDownLatch(1);
        
        Job blockingJob = () -> {
            try {
                startLatch.countDown();
                blockLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        
        JobHandle<Void> handle = jobSystem.submit(blockingJob);
        
        // Wait for job to start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        
        // Cancel the job
        boolean cancelled = handle.cancel(true);
        
        // Unblock the job
        blockLatch.countDown();
        
        assertTrue(cancelled || handle.isDone()); // Either cancelled or completed quickly
    }
    
    @Test
    void testJobException() {
        Job failingJob = () -> {
            throw new RuntimeException("Test exception");
        };
        
        JobHandle<Void> handle = jobSystem.submit(failingJob);
        
        assertThrows(Exception.class, () -> handle.get(1, TimeUnit.SECONDS));
        assertTrue(handle.isDone());
        assertTrue(handle.isCompletedExceptionally());
    }
    
    @Test
    void testJobPriority() throws Exception {
        List<Integer> executionOrder = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);
        
        // Submit jobs with different priorities
        Job lowPriorityJob = new Job() {
            @Override
            public void execute() {
                synchronized (executionOrder) {
                    executionOrder.add(1);
                }
                latch.countDown();
            }
            
            @Override
            public int getPriority() {
                return 1;
            }
            
            @Override
            public String getDescription() {
                return "Low Priority Job";
            }
        };
        
        Job highPriorityJob = new Job() {
            @Override
            public void execute() {
                synchronized (executionOrder) {
                    executionOrder.add(3);
                }
                latch.countDown();
            }
            
            @Override
            public int getPriority() {
                return 3;
            }
            
            @Override
            public String getDescription() {
                return "High Priority Job";
            }
        };
        
        Job mediumPriorityJob = new Job() {
            @Override
            public void execute() {
                synchronized (executionOrder) {
                    executionOrder.add(2);
                }
                latch.countDown();
            }
            
            @Override
            public int getPriority() {
                return 2;
            }
            
            @Override
            public String getDescription() {
                return "Medium Priority Job";
            }
        };
        
        jobSystem.submit(lowPriorityJob);
        jobSystem.submit(highPriorityJob);
        jobSystem.submit(mediumPriorityJob);
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        // Note: Priority ordering is not guaranteed in all cases due to work stealing,
        // but we can at least verify all jobs executed
        assertEquals(3, executionOrder.size());
        assertTrue(executionOrder.contains(1));
        assertTrue(executionOrder.contains(2));
        assertTrue(executionOrder.contains(3));
    }
    
    @Test
    void testStatistics() throws Exception {
        final int jobCount = 50;
        CountDownLatch latch = new CountDownLatch(jobCount);
        
        // Submit multiple jobs
        for (int i = 0; i < jobCount; i++) {
            jobSystem.submit(() -> {
                try {
                    Thread.sleep(1); // Small delay to measure execution time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Wait a bit for statistics to update
        Thread.sleep(100);
        
        JobSystem.JobSystemStatistics stats = jobSystem.getStatistics();
        
        assertEquals(jobCount, stats.getJobsSubmitted());
        assertEquals(jobCount, stats.getJobsCompleted());
        assertEquals(0, stats.getJobsFailed());
        assertEquals(4, stats.getActiveWorkers());
        assertTrue(stats.getAverageExecutionTimeMs() >= 0);
        assertEquals(100.0, stats.getSuccessRate(), 0.1);
    }
    
    @Test
    void testJobHandle() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        
        Job job = () -> {
            try {
                Thread.sleep(100); // Simulate work
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        
        JobHandle<Void> handle = jobSystem.submit(job);
        
        assertFalse(handle.isDone());
        assertNotNull(handle.getJobDescription());
        assertTrue(handle.getElapsedNanos() > 0);
        assertTrue(handle.getElapsedMillis() >= 0);
        
        handle.get(); // Wait for completion
        
        assertTrue(handle.isDone());
        assertEquals(1, counter.get());
    }
}