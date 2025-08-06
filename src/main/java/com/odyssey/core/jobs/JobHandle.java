package com.odyssey.core.jobs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A handle that represents the execution state of a submitted job.
 * Provides methods to check completion status, wait for completion,
 * and retrieve results or exceptions.
 * 
 * @param <T> the result type of the job
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class JobHandle<T> {
    private final CompletableFuture<T> future;
    private final String jobDescription;
    private final long submitTimeNanos;
    
    /**
     * Creates a new job handle.
     * 
     * @param future the underlying CompletableFuture
     * @param jobDescription description of the job for debugging
     */
    public JobHandle(CompletableFuture<T> future, String jobDescription) {
        this.future = future;
        this.jobDescription = jobDescription;
        this.submitTimeNanos = System.nanoTime();
    }
    
    /**
     * Returns whether the job has completed (either successfully or exceptionally).
     * 
     * @return true if the job is done, false otherwise
     */
    public boolean isDone() {
        return future.isDone();
    }
    
    /**
     * Returns whether the job was cancelled before completion.
     * 
     * @return true if the job was cancelled, false otherwise
     */
    public boolean isCancelled() {
        return future.isCancelled();
    }
    
    /**
     * Returns whether the job completed exceptionally.
     * 
     * @return true if the job completed with an exception, false otherwise
     */
    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }
    
    /**
     * Attempts to cancel the job. Returns true if the job was successfully
     * cancelled, false if it was already completed or could not be cancelled.
     * 
     * @param mayInterruptIfRunning whether to interrupt the job if it's currently running
     * @return true if the job was cancelled, false otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }
    
    /**
     * Waits for the job to complete and returns the result.
     * 
     * @return the job result
     * @throws InterruptedException if the current thread was interrupted
     * @throws ExecutionException if the job completed exceptionally
     */
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }
    
    /**
     * Waits for the job to complete with a timeout and returns the result.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the job result
     * @throws InterruptedException if the current thread was interrupted
     * @throws ExecutionException if the job completed exceptionally
     * @throws TimeoutException if the wait timed out
     */
    public T get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
    
    /**
     * Returns the result value (or null) if completed successfully,
     * otherwise returns null.
     * 
     * @return the result value or null
     */
    public T getNow() {
        return future.getNow(null);
    }
    
    /**
     * Returns the job description for debugging purposes.
     * 
     * @return the job description
     */
    public String getJobDescription() {
        return jobDescription;
    }
    
    /**
     * Returns the time elapsed since the job was submitted, in nanoseconds.
     * 
     * @return elapsed time in nanoseconds
     */
    public long getElapsedNanos() {
        return System.nanoTime() - submitTimeNanos;
    }
    
    /**
     * Returns the time elapsed since the job was submitted, in milliseconds.
     * 
     * @return elapsed time in milliseconds
     */
    public double getElapsedMillis() {
        return getElapsedNanos() / 1_000_000.0;
    }
    
    /**
     * Adds a callback to be executed when the job completes.
     * 
     * @param callback the callback to execute
     * @return this handle for method chaining
     */
    public JobHandle<T> whenComplete(JobCallback<T> callback) {
        future.whenComplete((result, throwable) -> {
            try {
                callback.onComplete(result, throwable);
            } catch (Exception e) {
                // Log callback exceptions but don't propagate them
                System.err.println("Error in job callback for " + jobDescription + ": " + e.getMessage());
            }
        });
        return this;
    }
    
    /**
     * Callback interface for job completion.
     * 
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface JobCallback<T> {
        /**
         * Called when the job completes.
         * 
         * @param result the job result (null if failed or cancelled)
         * @param throwable the exception if the job failed (null if successful)
         */
        void onComplete(T result, Throwable throwable);
    }
}