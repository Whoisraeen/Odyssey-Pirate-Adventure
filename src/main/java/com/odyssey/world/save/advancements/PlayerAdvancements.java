package com.odyssey.world.save.advancements;

import com.odyssey.world.save.advancements.AdvancementStats;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents individual player advancement progress and completion data.
 * Stores advancement progress, completion times, and criteria tracking.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerAdvancements {
    private UUID playerId;
    private long createdTime;
    private long lastUpdated;
    
    // Map of advancement ID to progress data
    private Map<String, AdvancementProgress> advancements;
    
    // Map of completed advancement ID to completion time
    private Map<String, Long> completedAdvancements;
    
    /**
     * Creates new player advancements.
     * 
     * @param playerId the player UUID
     */
    public PlayerAdvancements(UUID playerId) {
        this.playerId = playerId;
        this.createdTime = Instant.now().toEpochMilli();
        this.lastUpdated = this.createdTime;
        this.advancements = new HashMap<>();
        this.completedAdvancements = new HashMap<>();
    }
    
    /**
     * Default constructor for JSON deserialization.
     */
    public PlayerAdvancements() {
        this.advancements = new HashMap<>();
        this.completedAdvancements = new HashMap<>();
    }
    
    /**
     * Gets the player UUID.
     * 
     * @return the player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the creation timestamp.
     * 
     * @return the creation timestamp
     */
    public long getCreatedTime() {
        return createdTime;
    }
    
    /**
     * Gets the last updated timestamp.
     * 
     * @return the last updated timestamp
     */
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Updates progress for an advancement criteria.
     * 
     * @param advancementId the advancement identifier
     * @param criteriaId the criteria identifier
     * @param progress the progress value
     */
    public void updateProgress(String advancementId, String criteriaId, int progress) {
        AdvancementProgress advancementProgress = advancements.computeIfAbsent(
            advancementId, id -> new AdvancementProgress(advancementId)
        );
        
        advancementProgress.updateCriteria(criteriaId, progress);
        updateTimestamp();
    }
    
    /**
     * Marks an advancement as completed.
     * 
     * @param advancementId the advancement identifier
     */
    public void completeAdvancement(String advancementId) {
        if (!completedAdvancements.containsKey(advancementId)) {
            long completionTime = Instant.now().toEpochMilli();
            completedAdvancements.put(advancementId, completionTime);
            
            // Ensure progress exists
            advancements.computeIfAbsent(advancementId, id -> new AdvancementProgress(advancementId))
                       .setCompleted(true);
            
            updateTimestamp();
        }
    }
    
    /**
     * Revokes an advancement completion.
     * 
     * @param advancementId the advancement identifier
     */
    public void revokeAdvancement(String advancementId) {
        completedAdvancements.remove(advancementId);
        
        AdvancementProgress progress = advancements.get(advancementId);
        if (progress != null) {
            progress.setCompleted(false);
        }
        
        updateTimestamp();
    }
    
    /**
     * Checks if an advancement is completed.
     * 
     * @param advancementId the advancement identifier
     * @return true if the advancement is completed
     */
    public boolean isCompleted(String advancementId) {
        return completedAdvancements.containsKey(advancementId);
    }
    
    /**
     * Gets the progress for an advancement.
     * 
     * @param advancementId the advancement identifier
     * @return the advancement progress, or null if not started
     */
    public AdvancementProgress getProgress(String advancementId) {
        return advancements.get(advancementId);
    }
    
    /**
     * Gets all advancement progress data.
     * 
     * @return map of advancement IDs to progress data
     */
    public Map<String, AdvancementProgress> getAllProgress() {
        return new HashMap<>(advancements);
    }
    
    /**
     * Gets all completed advancements.
     * 
     * @return map of advancement IDs to completion times
     */
    public Map<String, Long> getCompletedAdvancements() {
        return new HashMap<>(completedAdvancements);
    }
    
    /**
     * Gets the number of completed advancements.
     * 
     * @return the completion count
     */
    public int getCompletedCount() {
        return completedAdvancements.size();
    }
    
    /**
     * Gets the completion time for an advancement.
     * 
     * @param advancementId the advancement identifier
     * @return the completion time, or null if not completed
     */
    public Long getCompletionTime(String advancementId) {
        return completedAdvancements.get(advancementId);
    }
    
    /**
     * Checks if the player has started working on an advancement.
     * 
     * @param advancementId the advancement identifier
     * @return true if the advancement has been started
     */
    public boolean hasStarted(String advancementId) {
        return advancements.containsKey(advancementId);
    }
    
    /**
     * Gets the progress percentage for an advancement.
     * 
     * @param advancementId the advancement identifier
     * @param totalCriteria the total number of criteria
     * @return the progress percentage (0.0 to 1.0)
     */
    public double getProgressPercentage(String advancementId, int totalCriteria) {
        AdvancementProgress progress = advancements.get(advancementId);
        if (progress == null || totalCriteria == 0) {
            return 0.0;
        }
        
        int completedCriteria = progress.getCompletedCriteriaCount();
        return (double) completedCriteria / totalCriteria;
    }
    
    /**
     * Resets all progress for an advancement.
     * 
     * @param advancementId the advancement identifier
     */
    public void resetAdvancement(String advancementId) {
        advancements.remove(advancementId);
        completedAdvancements.remove(advancementId);
        updateTimestamp();
    }
    
    /**
     * Resets all advancement progress.
     */
    public void resetAll() {
        advancements.clear();
        completedAdvancements.clear();
        updateTimestamp();
    }
    
    /**
     * Checks if the advancement data is empty.
     * 
     * @return true if no advancements have been started or completed
     */
    public boolean isEmpty() {
        return advancements.isEmpty() && completedAdvancements.isEmpty();
    }
    
    /**
     * Gets advancement statistics.
     * 
     * @return advancement statistics
     */
    public AdvancementStats getStats() {
        int started = advancements.size();
        int completed = completedAdvancements.size();
        int inProgress = started - completed;
        
        return new AdvancementStats(started, inProgress, completed);
    }
    
    /**
     * Updates the last updated timestamp.
     */
    private void updateTimestamp() {
        this.lastUpdated = Instant.now().toEpochMilli();
    }
    
    @Override
    public String toString() {
        return String.format("PlayerAdvancements{playerId=%s, completed=%d, inProgress=%d}", 
            playerId, completedAdvancements.size(), advancements.size() - completedAdvancements.size());
    }
}