package com.odyssey.world.save.advancements;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks progress for a specific advancement.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AdvancementProgress {
    private String advancementId;
    private boolean completed;
    private long startTime;
    private long completionTime;
    
    // Map of criteria ID to progress value
    private Map<String, Integer> criteriaProgress;
    
    // Map of criteria ID to completion time
    private Map<String, Long> criteriaCompletionTimes;
    
    public AdvancementProgress(String advancementId) {
        this.advancementId = advancementId;
        this.completed = false;
        this.startTime = Instant.now().toEpochMilli();
        this.completionTime = 0;
        this.criteriaProgress = new HashMap<>();
        this.criteriaCompletionTimes = new HashMap<>();
    }
    
    public AdvancementProgress() {
        this.criteriaProgress = new HashMap<>();
        this.criteriaCompletionTimes = new HashMap<>();
    }
    
    public void updateCriteria(String criteriaId, int progress) {
        criteriaProgress.put(criteriaId, progress);
        
        // Mark criteria as completed if progress indicates completion
        if (progress > 0 && !criteriaCompletionTimes.containsKey(criteriaId)) {
            criteriaCompletionTimes.put(criteriaId, Instant.now().toEpochMilli());
        }
    }
    
    public int getCriteriaProgress(String criteriaId) {
        return criteriaProgress.getOrDefault(criteriaId, 0);
    }
    
    public boolean isCriteriaCompleted(String criteriaId) {
        return criteriaCompletionTimes.containsKey(criteriaId);
    }
    
    public int getCompletedCriteriaCount() {
        return criteriaCompletionTimes.size();
    }
    
    public String getAdvancementId() { return advancementId; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { 
        this.completed = completed;
        if (completed && completionTime == 0) {
            this.completionTime = Instant.now().toEpochMilli();
        }
    }
    public long getStartTime() { return startTime; }
    public long getCompletionTime() { return completionTime; }
    public Map<String, Integer> getCriteriaProgress() { return new HashMap<>(criteriaProgress); }
    public Map<String, Long> getCriteriaCompletionTimes() { return new HashMap<>(criteriaCompletionTimes); }
}