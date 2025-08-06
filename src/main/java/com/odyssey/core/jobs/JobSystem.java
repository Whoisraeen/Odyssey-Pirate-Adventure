package com.odyssey.core.jobs;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A high-performance work-stealing job system for parallel task execution.
 * This system distributes work across multiple worker threads using work-stealing
 * queues to maximize CPU utilization and minimize contention.
 * 
 * Key features:
 * - Work-stealing thread pool for load balancing
 * - Priority-based job scheduling
 * - Job cancellation support
 * - Comprehensive statistics and monitoring
 * - Graceful shutdown with configurable timeout
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class JobSystem {
    private static final Logger logger = LoggerFactory.getLogger(JobSystem.class);
    
    // Configuration
    final int numWorkers;
    private final int queueCapacity;
    private final boolean enableStatistics;
    
    // Worker management
    final List<WorkerThread> workers;
    final List<WorkStealingJobQueue> workerQueues;
    private final AtomicInteger nextWorkerIndex;
    
    // Global queue for high-priority jobs and overflow
    private final BlockingQueue<WorkStealingJobQueue.JobWrapper> globalQueue;
    
    // Lifecycle management
    private final AtomicBoolean running;
    private final AtomicBoolean shutdown;
    
    // Statistics
    private final AtomicLong jobsSubmitted;
    private final AtomicLong jobsCompleted;
    private final AtomicLong jobsFailed;
    private final AtomicLong jobsCancelled;
    private final AtomicLong totalExecutionTimeNanos;
    
    /**
     * Creates a new job system with the specified configuration.
     * 
     * @param config the game configuration
     */
    public JobSystem(GameConfig config) {
        this.numWorkers = determineWorkerCount(config);
        this.queueCapacity = config.getJobQueueCapacity();
        this.enableStatistics = config.isJobSystemStatisticsEnabled();
        
        // Initialize worker structures
        this.workers = new ArrayList<>(numWorkers);
        this.workerQueues = new ArrayList<>(numWorkers);
        this.nextWorkerIndex = new AtomicInteger(0);
        
        // Initialize global queue
        this.globalQueue = new LinkedBlockingQueue<>(config.getGlobalJobQueueCapacity());
        
        // Initialize lifecycle state
        this.running = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);
        
        // Initialize statistics
        this.jobsSubmitted = new AtomicLong(0);
        this.jobsCompleted = new AtomicLong(0);
        this.jobsFailed = new AtomicLong(0);
        this.jobsCancelled = new AtomicLong(0);
        this.totalExecutionTimeNanos = new AtomicLong(0);
        
        logger.info("JobSystem initialized with {} workers, queue capacity: {}", numWorkers, queueCapacity);
    }
    
    /**
     * Initializes and starts the job system.
     */
    public void initialize() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting JobSystem with {} worker threads", numWorkers);
            
            // Create worker queues
            for (int i = 0; i < numWorkers; i++) {
                workerQueues.add(new WorkStealingJobQueue(queueCapacity));
            }
            
            // Create and start worker threads
            for (int i = 0; i < numWorkers; i++) {
                WorkerThread worker = new WorkerThread(i, workerQueues.get(i), this);
                workers.add(worker);
                worker.start();
            }
            
            logger.info("JobSystem started successfully");
        } else {
            logger.warn("JobSystem is already running");
        }
    }
    
    /**
     * Submits a job for asynchronous execution.
     * 
     * @param job the job to execute
     * @return a handle to track the job's progress
     */
    public JobHandle<Void> submit(Job job) {
        return submit(job, () -> {
            try {
                job.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }
    
    /**
     * Submits a job that returns a result for asynchronous execution.
     * 
     * @param <T> the result type
     * @param job the job to execute
     * @param supplier the supplier that executes the job and returns a result
     * @return a handle to track the job's progress and retrieve the result
     */
    public <T> JobHandle<T> submit(Job job, Supplier<T> supplier) {
        if (shutdown.get()) {
            throw new IllegalStateException("JobSystem is shut down");
        }
        
        jobsSubmitted.incrementAndGet();
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // Create the wrapper job that will be executed
        Job wrapperJob = new Job() {
            @Override
            public void execute() throws Exception {
                try {
                    if (future.isCancelled()) {
                        future.cancel(false);
                        jobsCancelled.incrementAndGet();
                        return;
                    }
                    
                    long startTime = enableStatistics ? System.nanoTime() : 0;
                    T result = supplier.get();
                    
                    if (enableStatistics) {
                        long executionTime = System.nanoTime() - startTime;
                        totalExecutionTimeNanos.addAndGet(executionTime);
                    }
                    
                    future.complete(result);
                    jobsCompleted.incrementAndGet();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    jobsFailed.incrementAndGet();
                    logger.debug("Job {} failed with exception: {}", job.getDescription(), e.getMessage());
                }
            }
            
            @Override
            public int getPriority() {
                return job.getPriority();
            }
            
            @Override
            public String getDescription() {
                return job.getDescription();
            }
            
            @Override
            public boolean isCancellable() {
                return job.isCancellable();
            }
        };
        
        WorkStealingJobQueue.JobWrapper wrapper = new WorkStealingJobQueue.JobWrapper(wrapperJob);
        
        // Try to submit to a worker queue, fall back to global queue
        if (!trySubmitToWorkerQueue(wrapper)) {
            if (!globalQueue.offer(wrapper)) {
                // If all queues are full, execute on calling thread as last resort
                logger.warn("All job queues full, executing {} on calling thread", job.getDescription());
                try {
                    wrapper.getJob().execute();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }
        
        return new JobHandle<>(future, job.getDescription());
    }
    
    /**
     * Shuts down the job system gracefully.
     * 
     * @param timeoutSeconds the maximum time to wait for running jobs to complete
     * @return true if shutdown completed within the timeout, false otherwise
     */
    public boolean shutdown(int timeoutSeconds) {
        if (!shutdown.compareAndSet(false, true)) {
            logger.warn("JobSystem is already shutting down");
            return true;
        }
        
        logger.info("Shutting down JobSystem...");
        running.set(false);
        
        // Interrupt all worker threads
        for (WorkerThread worker : workers) {
            worker.interrupt();
        }
        
        // Wait for workers to finish
        boolean allFinished = true;
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        
        for (WorkerThread worker : workers) {
            try {
                long remainingTime = endTime - System.currentTimeMillis();
                if (remainingTime > 0) {
                    worker.join(remainingTime);
                    if (worker.isAlive()) {
                        logger.warn("Worker {} did not shut down within timeout", worker.getName());
                        allFinished = false;
                    }
                } else {
                    allFinished = false;
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for worker {} to shutdown", worker.getName());
                Thread.currentThread().interrupt();
                allFinished = false;
            }
        }
        
        // Log final statistics
        if (enableStatistics) {
            logStatistics();
        }
        
        logger.info("JobSystem shutdown {}", allFinished ? "completed" : "timed out");
        return allFinished;
    }
    
    /**
     * Returns comprehensive statistics about job system performance.
     * 
     * @return job system statistics
     */
    public JobSystemStatistics getStatistics() {
        long submitted = jobsSubmitted.get();
        long completed = jobsCompleted.get();
        long failed = jobsFailed.get();
        long cancelled = jobsCancelled.get();
        long totalTime = totalExecutionTimeNanos.get();
        
        double avgExecutionTime = completed > 0 ? (totalTime / (double) completed) / 1_000_000.0 : 0.0;
        
        int totalQueuedJobs = globalQueue.size();
        for (WorkStealingJobQueue queue : workerQueues) {
            totalQueuedJobs += queue.size();
        }
        
        return new JobSystemStatistics(
            submitted, completed, failed, cancelled,
            totalQueuedJobs, avgExecutionTime, workers.size()
        );
    }
    
    /**
     * Determines the optimal number of worker threads based on system capabilities.
     */
    private int determineWorkerCount(GameConfig config) {
        int configuredWorkers = config.getJobSystemWorkerCount();
        if (configuredWorkers > 0) {
            return configuredWorkers;
        }
        
        // Auto-detect: use all available processors minus one for the main thread,
        // but ensure we have at least 2 workers
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int optimalWorkers = Math.max(2, availableProcessors - 1);
        
        logger.info("Auto-detected {} worker threads (available processors: {})", 
                   optimalWorkers, availableProcessors);
        return optimalWorkers;
    }
    
    /**
     * Attempts to submit a job to a worker queue using round-robin distribution.
     */
    private boolean trySubmitToWorkerQueue(WorkStealingJobQueue.JobWrapper job) {
        int startIndex = nextWorkerIndex.getAndIncrement() % numWorkers;
        
        // Try each queue starting from the selected index
        for (int i = 0; i < numWorkers; i++) {
            int queueIndex = (startIndex + i) % numWorkers;
            WorkStealingJobQueue queue = workerQueues.get(queueIndex);
            
            if (queue.addLocal(job)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns a worker queue for work stealing (used by WorkerThread).
     */
    WorkStealingJobQueue getWorkerQueue(int excludeIndex) {
        if (workerQueues.isEmpty()) {
            return null;
        }
        
        // Try to find a non-empty queue, excluding the requester's queue
        for (int i = 0; i < numWorkers; i++) {
            if (i != excludeIndex && !workerQueues.get(i).isEmpty()) {
                return workerQueues.get(i);
            }
        }
        
        return null;
    }
    
    /**
     * Returns the global job queue (used by WorkerThread).
     */
    BlockingQueue<WorkStealingJobQueue.JobWrapper> getGlobalQueue() {
        return globalQueue;
    }
    
    /**
     * Returns whether the job system is running (used by WorkerThread).
     */
    boolean isRunning() {
        return running.get();
    }
    
    /**
     * Logs current job system statistics.
     */
    private void logStatistics() {
        JobSystemStatistics stats = getStatistics();
        logger.info("Job System Final Statistics:");
        logger.info("  Jobs Submitted: {}", stats.getJobsSubmitted());
        logger.info("  Jobs Completed: {}", stats.getJobsCompleted());
        logger.info("  Jobs Failed: {}", stats.getJobsFailed());
        logger.info("  Jobs Cancelled: {}", stats.getJobsCancelled());
        logger.info("  Jobs Queued: {}", stats.getQueuedJobs());
        logger.info("  Average Execution Time: {:.2f}ms", stats.getAverageExecutionTimeMs());
        logger.info("  Active Workers: {}", stats.getActiveWorkers());
    }
    
    /**
     * Statistics container for job system performance metrics.
     */
    public static class JobSystemStatistics {
        private final long jobsSubmitted;
        private final long jobsCompleted;
        private final long jobsFailed;
        private final long jobsCancelled;
        private final int queuedJobs;
        private final double averageExecutionTimeMs;
        private final int activeWorkers;
        
        public JobSystemStatistics(long jobsSubmitted, long jobsCompleted, long jobsFailed,
                                 long jobsCancelled, int queuedJobs, double averageExecutionTimeMs,
                                 int activeWorkers) {
            this.jobsSubmitted = jobsSubmitted;
            this.jobsCompleted = jobsCompleted;
            this.jobsFailed = jobsFailed;
            this.jobsCancelled = jobsCancelled;
            this.queuedJobs = queuedJobs;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.activeWorkers = activeWorkers;
        }
        
        public long getJobsSubmitted() { return jobsSubmitted; }
        public long getJobsCompleted() { return jobsCompleted; }
        public long getJobsFailed() { return jobsFailed; }
        public long getJobsCancelled() { return jobsCancelled; }
        public int getQueuedJobs() { return queuedJobs; }
        public double getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public int getActiveWorkers() { return activeWorkers; }
        
        public double getSuccessRate() {
            long total = jobsCompleted + jobsFailed + jobsCancelled;
            return total > 0 ? (jobsCompleted / (double) total) * 100.0 : 0.0;
        }
    }
}