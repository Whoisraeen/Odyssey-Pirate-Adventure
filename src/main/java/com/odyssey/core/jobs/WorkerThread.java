package com.odyssey.core.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A worker thread that executes jobs from work-stealing queues.
 * Each worker has its own local queue and can steal work from other workers
 * when its local queue is empty.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WorkerThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(WorkerThread.class);
    
    private final int workerId;
    private final WorkStealingJobQueue localQueue;
    private final JobSystem jobSystem;
    
    // Statistics
    private long jobsExecuted = 0;
    private long jobsStolen = 0;
    private long jobsFromGlobal = 0;
    
    /**
     * Creates a new worker thread.
     * 
     * @param workerId the unique identifier for this worker
     * @param localQueue the local work queue for this worker
     * @param jobSystem the parent job system
     */
    public WorkerThread(int workerId, WorkStealingJobQueue localQueue, JobSystem jobSystem) {
        super("JobWorker-" + workerId);
        this.workerId = workerId;
        this.localQueue = localQueue;
        this.jobSystem = jobSystem;
        
        // Set as daemon thread so it doesn't prevent JVM shutdown
        setDaemon(true);
        
        logger.debug("Created worker thread: {}", getName());
    }
    
    @Override
    public void run() {
        logger.debug("Worker {} starting", getName());
        
        try {
            while (jobSystem.isRunning() && !isInterrupted()) {
                WorkStealingJobQueue.JobWrapper job = findWork();
                
                if (job != null) {
                    executeJob(job);
                } else {
                    // No work available, sleep briefly to avoid busy-waiting
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in worker {}: {}", getName(), e.getMessage(), e);
        } finally {
            logger.debug("Worker {} stopping (executed: {}, stolen: {}, global: {})", 
                        getName(), jobsExecuted, jobsStolen, jobsFromGlobal);
        }
    }
    
    /**
     * Attempts to find work using the work-stealing algorithm:
     * 1. Check local queue first
     * 2. Try to steal from other workers
     * 3. Check global queue
     * 
     * @return a job to execute, or null if no work is available
     */
    private WorkStealingJobQueue.JobWrapper findWork() {
        // First, try local queue (LIFO for better cache locality)
        WorkStealingJobQueue.JobWrapper job = localQueue.takeLocal();
        if (job != null) {
            return job;
        }
        
        // Try to steal from other workers (FIFO to avoid conflicts)
        job = tryStealWork();
        if (job != null) {
            jobsStolen++;
            return job;
        }
        
        // Finally, check global queue
        BlockingQueue<WorkStealingJobQueue.JobWrapper> globalQueue = jobSystem.getGlobalQueue();
        try {
            job = globalQueue.poll(1, TimeUnit.MILLISECONDS);
            if (job != null) {
                jobsFromGlobal++;
                return job;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        
        return null;
    }
    
    /**
     * Attempts to steal work from other worker queues.
     * 
     * @return a stolen job, or null if no work was available to steal
     */
    private WorkStealingJobQueue.JobWrapper tryStealWork() {
        // Try a few times to steal work, but don't be too aggressive
        int maxAttempts = Math.min(4, jobSystem.workers.size());
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            WorkStealingJobQueue targetQueue = jobSystem.getWorkerQueue(workerId);
            if (targetQueue != null) {
                WorkStealingJobQueue.JobWrapper job = targetQueue.steal();
                if (job != null) {
                    logger.trace("Worker {} stole job: {}", getName(), job.getDescription());
                    return job;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Executes a job and handles any exceptions.
     * 
     * @param jobWrapper the job wrapper to execute
     */
    private void executeJob(WorkStealingJobQueue.JobWrapper jobWrapper) {
        if (jobWrapper.isCancelled()) {
            logger.trace("Skipping cancelled job: {}", jobWrapper.getDescription());
            return;
        }
        
        Job job = jobWrapper.getJob();
        String description = job.getDescription();
        
        try {
            long startTime = System.nanoTime();
            
            logger.trace("Worker {} executing job: {}", getName(), description);
            job.execute();
            
            long executionTime = System.nanoTime() - startTime;
            jobsExecuted++;
            
            if (logger.isTraceEnabled()) {
                logger.trace("Worker {} completed job: {} in {:.2f}ms", 
                           getName(), description, executionTime / 1_000_000.0);
            }
            
        } catch (InterruptedException e) {
            logger.debug("Worker {} interrupted while executing job: {}", getName(), description);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Job {} failed in worker {}: {}", description, getName(), e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("Job execution stack trace:", e);
            }
        }
    }
    
    /**
     * Returns statistics for this worker thread.
     * 
     * @return worker statistics
     */
    public WorkerStatistics getStatistics() {
        return new WorkerStatistics(workerId, getName(), jobsExecuted, jobsStolen, 
                                   jobsFromGlobal, localQueue.size());
    }
    
    /**
     * Statistics for a worker thread.
     */
    public static class WorkerStatistics {
        private final int workerId;
        private final String name;
        private final long jobsExecuted;
        private final long jobsStolen;
        private final long jobsFromGlobal;
        private final int queueSize;
        
        public WorkerStatistics(int workerId, String name, long jobsExecuted, 
                              long jobsStolen, long jobsFromGlobal, int queueSize) {
            this.workerId = workerId;
            this.name = name;
            this.jobsExecuted = jobsExecuted;
            this.jobsStolen = jobsStolen;
            this.jobsFromGlobal = jobsFromGlobal;
            this.queueSize = queueSize;
        }
        
        public int getWorkerId() { return workerId; }
        public String getName() { return name; }
        public long getJobsExecuted() { return jobsExecuted; }
        public long getJobsStolen() { return jobsStolen; }
        public long getJobsFromGlobal() { return jobsFromGlobal; }
        public int getQueueSize() { return queueSize; }
        public long getTotalJobs() { return jobsExecuted + jobsStolen + jobsFromGlobal; }
    }
}