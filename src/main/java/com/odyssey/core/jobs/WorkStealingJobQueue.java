package com.odyssey.core.jobs;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A work-stealing job queue that allows jobs to be added to one end
 * and stolen from the other end. This implementation provides
 * efficient work distribution for thread pools.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WorkStealingJobQueue {
    private final BlockingDeque<JobWrapper> jobs;
    private final AtomicInteger size;
    private final int maxCapacity;
    
    /**
     * Creates a new work-stealing job queue with the specified capacity.
     * 
     * @param maxCapacity the maximum number of jobs this queue can hold
     */
    public WorkStealingJobQueue(int maxCapacity) {
        this.jobs = new LinkedBlockingDeque<>(maxCapacity);
        this.size = new AtomicInteger(0);
        this.maxCapacity = maxCapacity;
    }
    
    /**
     * Adds a job to the tail of this queue (local work submission).
     * This is the preferred method for the owning thread to submit work.
     * 
     * @param job the job to add
     * @return true if the job was added, false if the queue is full
     */
    public boolean addLocal(JobWrapper job) {
        if (jobs.offerLast(job)) {
            size.incrementAndGet();
            return true;
        }
        return false;
    }
    
    /**
     * Removes and returns a job from the tail of this queue (local work retrieval).
     * This is the preferred method for the owning thread to get its own work.
     * 
     * @return a job or null if the queue is empty
     */
    public JobWrapper takeLocal() {
        JobWrapper job = jobs.pollLast();
        if (job != null) {
            size.decrementAndGet();
        }
        return job;
    }
    
    /**
     * Attempts to steal a job from the head of this queue (work stealing).
     * This method is used by other threads to steal work from this queue.
     * 
     * @return a stolen job or null if the queue is empty
     */
    public JobWrapper steal() {
        JobWrapper job = jobs.pollFirst();
        if (job != null) {
            size.decrementAndGet();
        }
        return job;
    }
    
    /**
     * Returns the current number of jobs in this queue.
     * Note: This is an approximation due to concurrent access.
     * 
     * @return the approximate number of jobs
     */
    public int size() {
        return size.get();
    }
    
    /**
     * Returns whether this queue is empty.
     * Note: This is an approximation due to concurrent access.
     * 
     * @return true if the queue appears empty, false otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Returns whether this queue is at capacity.
     * 
     * @return true if the queue is full, false otherwise
     */
    public boolean isFull() {
        return size() >= maxCapacity;
    }
    
    /**
     * Returns the maximum capacity of this queue.
     * 
     * @return the maximum capacity
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    /**
     * Clears all jobs from this queue.
     */
    public void clear() {
        jobs.clear();
        size.set(0);
    }
    
    /**
     * Wrapper class that holds a job along with metadata.
     */
    public static class JobWrapper {
        private final Job job;
        private final int priority;
        private final long submitTimeNanos;
        private final String description;
        private volatile boolean cancelled = false;
        
        /**
         * Creates a new job wrapper.
         * 
         * @param job the job to wrap
         */
        public JobWrapper(Job job) {
            this.job = job;
            this.priority = job.getPriority();
            this.description = job.getDescription();
            this.submitTimeNanos = System.nanoTime();
        }
        
        /**
         * Returns the wrapped job.
         * 
         * @return the job
         */
        public Job getJob() {
            return job;
        }
        
        /**
         * Returns the job priority.
         * 
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }
        
        /**
         * Returns the job description.
         * 
         * @return the description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Returns the time this job was submitted.
         * 
         * @return submit time in nanoseconds
         */
        public long getSubmitTimeNanos() {
            return submitTimeNanos;
        }
        
        /**
         * Returns the age of this job in nanoseconds.
         * 
         * @return age in nanoseconds
         */
        public long getAgeNanos() {
            return System.nanoTime() - submitTimeNanos;
        }
        
        /**
         * Marks this job as cancelled.
         */
        public void cancel() {
            this.cancelled = true;
        }
        
        /**
         * Returns whether this job has been cancelled.
         * 
         * @return true if cancelled, false otherwise
         */
        public boolean isCancelled() {
            return cancelled;
        }
    }
}