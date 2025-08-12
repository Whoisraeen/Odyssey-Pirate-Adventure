package com.odyssey.world.save.advancements;

/**
 * Statistics for player advancement progress in The Odyssey.
 * Tracks counts of started, in-progress, and completed advancements.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AdvancementStats {
    private final int started;
    private final int inProgress;
    private final int completed;
    private final int total;
    
    /**
     * Creates advancement statistics.
     * 
     * @param started number of advancements started
     * @param inProgress number of advancements in progress
     * @param completed number of advancements completed
     */
    public AdvancementStats(int started, int inProgress, int completed) {
        this.started = Math.max(0, started);
        this.inProgress = Math.max(0, inProgress);
        this.completed = Math.max(0, completed);
        this.total = this.started + this.inProgress + this.completed;
    }
    
    /**
     * Creates advancement statistics with total count.
     * 
     * @param started number of advancements started
     * @param inProgress number of advancements in progress
     * @param completed number of advancements completed
     * @param total total number of available advancements
     */
    public AdvancementStats(int started, int inProgress, int completed, int total) {
        this.started = Math.max(0, started);
        this.inProgress = Math.max(0, inProgress);
        this.completed = Math.max(0, completed);
        this.total = Math.max(0, total);
    }
    
    /**
     * Gets the number of advancements started.
     * 
     * @return the started count
     */
    public int getStarted() {
        return started;
    }
    
    /**
     * Gets the number of advancements in progress.
     * 
     * @return the in-progress count
     */
    public int getInProgress() {
        return inProgress;
    }
    
    /**
     * Gets the number of advancements completed.
     * 
     * @return the completed count
     */
    public int getCompleted() {
        return completed;
    }
    
    /**
     * Gets the total number of advancements.
     * 
     * @return the total count
     */
    public int getTotal() {
        return total;
    }
    
    /**
     * Gets the number of advancements not yet started.
     * 
     * @return the not started count
     */
    public int getNotStarted() {
        return Math.max(0, total - started - inProgress - completed);
    }
    
    /**
     * Gets the completion percentage.
     * 
     * @return completion percentage (0.0 to 1.0)
     */
    public double getCompletionPercentage() {
        if (total == 0) {
            return 0.0;
        }
        return (double) completed / total;
    }
    
    /**
     * Gets the progress percentage (started + in-progress + completed).
     * 
     * @return progress percentage (0.0 to 1.0)
     */
    public double getProgressPercentage() {
        if (total == 0) {
            return 0.0;
        }
        return (double) (started + inProgress + completed) / total;
    }
    
    /**
     * Checks if all advancements are completed.
     * 
     * @return true if all advancements are completed
     */
    public boolean isAllCompleted() {
        return total > 0 && completed == total;
    }
    
    /**
     * Checks if no advancements have been started.
     * 
     * @return true if no advancements have been started
     */
    public boolean isNoneStarted() {
        return started == 0 && inProgress == 0 && completed == 0;
    }
    
    /**
     * Creates a summary string of the advancement statistics.
     * 
     * @return formatted statistics string
     */
    public String getSummary() {
        return String.format("Advancements: %d/%d completed (%.1f%%), %d in progress, %d started", 
            completed, total, getCompletionPercentage() * 100, inProgress, started);
    }
    
    @Override
    public String toString() {
        return String.format("AdvancementStats{started=%d, inProgress=%d, completed=%d, total=%d}", 
            started, inProgress, completed, total);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AdvancementStats that = (AdvancementStats) obj;
        return started == that.started &&
               inProgress == that.inProgress &&
               completed == that.completed &&
               total == that.total;
    }
    
    @Override
    public int hashCode() {
        int result = started;
        result = 31 * result + inProgress;
        result = 31 * result + completed;
        result = 31 * result + total;
        return result;
    }
}