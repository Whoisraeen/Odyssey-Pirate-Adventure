package com.odyssey.core.jobs;

/**
 * Represents a unit of work that can be executed by the job system.
 * Jobs are immutable and thread-safe by design.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface Job {
    /**
     * Executes the job's work. This method should be thread-safe and
     * should not modify shared state without proper synchronization.
     * 
     * @throws Exception if the job execution fails
     */
    void execute() throws Exception;
    
    /**
     * Returns the priority of this job. Higher values indicate higher priority.
     * Default priority is 0 (normal priority).
     * 
     * @return the job priority
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Returns a human-readable description of this job for debugging purposes.
     * 
     * @return job description
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
    
    /**
     * Returns whether this job can be cancelled before execution.
     * 
     * @return true if the job can be cancelled, false otherwise
     */
    default boolean isCancellable() {
        return true;
    }
}