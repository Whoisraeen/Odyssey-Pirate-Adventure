package com.odyssey.quest;

import com.odyssey.util.Logger;

/**
 * Represents a single objective within a quest.
 */
public class QuestObjective {
    private static final Logger LOGGER = Logger.getLogger(QuestObjective.class);
    
    /** Unique identifier for this objective */
    private final String id;
    
    /** Description of the objective */
    private final String description;
    
    /** Current progress towards completion */
    private int currentProgress;
    
    /** Required progress for completion */
    private final int requiredProgress;
    
    /** Whether this objective is completed */
    private boolean completed;
    
    /** Whether this objective is optional */
    private boolean optional;
    
    /** Objective type */
    private ObjectiveType type;
    
    /**
     * Types of quest objectives.
     */
    public enum ObjectiveType {
        KILL_ENEMIES,
        COLLECT_ITEMS,
        VISIT_LOCATION,
        TALK_TO_NPC,
        CRAFT_ITEMS,
        TRADE_ITEMS,
        SURVIVE_TIME,
        REACH_LEVEL,
        COMPLETE_QUEST,
        CUSTOM
    }
    
    /**
     * Creates a new QuestObjective.
     * 
     * @param id Unique objective identifier
     * @param description Objective description
     * @param requiredProgress Required progress for completion
     */
    public QuestObjective(String id, String description, int requiredProgress) {
        this.id = id;
        this.description = description;
        this.requiredProgress = requiredProgress;
        this.currentProgress = 0;
        this.completed = false;
        this.optional = false;
        this.type = ObjectiveType.CUSTOM;
    }
    
    /**
     * Creates a new QuestObjective with type.
     * 
     * @param id Unique objective identifier
     * @param description Objective description
     * @param requiredProgress Required progress for completion
     * @param type Objective type
     */
    public QuestObjective(String id, String description, int requiredProgress, ObjectiveType type) {
        this(id, description, requiredProgress);
        this.type = type;
    }
    
    /**
     * Gets the objective ID.
     * 
     * @return Objective ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the objective description.
     * 
     * @return Objective description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the current progress.
     * 
     * @return Current progress
     */
    public int getCurrentProgress() {
        return currentProgress;
    }
    
    /**
     * Gets the required progress.
     * 
     * @return Required progress
     */
    public int getRequiredProgress() {
        return requiredProgress;
    }
    
    /**
     * Gets the objective type.
     * 
     * @return Objective type
     */
    public ObjectiveType getType() {
        return type;
    }
    
    /**
     * Sets the objective type.
     * 
     * @param type Objective type
     */
    public void setType(ObjectiveType type) {
        this.type = type;
    }
    
    /**
     * Checks if the objective is completed.
     * 
     * @return true if completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Checks if the objective is optional.
     * 
     * @return true if optional
     */
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * Sets whether the objective is optional.
     * 
     * @param optional true if optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    /**
     * Sets the current progress.
     * 
     * @param progress New progress value
     */
    public void setProgress(int progress) {
        int oldProgress = this.currentProgress;
        this.currentProgress = Math.max(0, Math.min(progress, requiredProgress));
        
        // Check for completion
        if (!completed && this.currentProgress >= requiredProgress) {
            completed = true;
            LOGGER.info("Objective '{}' completed: {}", id, description);
        }
        
        if (oldProgress != this.currentProgress) {
            LOGGER.debug("Objective '{}' progress: {} -> {}", id, oldProgress, this.currentProgress);
        }
    }
    
    /**
     * Advances the progress by a certain amount.
     * 
     * @param amount Amount to advance
     */
    public void advanceProgress(int amount) {
        setProgress(currentProgress + amount);
    }
    
    /**
     * Gets the progress as a percentage.
     * 
     * @return Progress percentage (0.0 to 1.0)
     */
    public float getProgressPercentage() {
        if (requiredProgress <= 0) {
            return completed ? 1.0f : 0.0f;
        }
        return (float) currentProgress / requiredProgress;
    }
    
    /**
     * Gets a formatted progress string.
     * 
     * @return Progress string (e.g., "3/5")
     */
    public String getProgressString() {
        return currentProgress + "/" + requiredProgress;
    }
    
    /**
     * Resets the objective progress.
     */
    public void reset() {
        currentProgress = 0;
        completed = false;
        LOGGER.debug("Reset objective '{}'", id);
    }
    
    /**
     * Forces completion of the objective.
     */
    public void forceComplete() {
        currentProgress = requiredProgress;
        completed = true;
        LOGGER.info("Force completed objective '{}': {}", id, description);
    }
    
    /**
     * Updates the objective (called each frame).
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Override in subclasses for time-based objectives
    }
    
    /**
     * Gets a formatted string representation of the objective.
     * 
     * @return Formatted objective string
     */
    @Override
    public String toString() {
        String status = completed ? "COMPLETED" : "IN_PROGRESS";
        String optionalText = optional ? " (Optional)" : "";
        return String.format("Objective[%s]: %s - %s (%s)%s", 
            id, description, getProgressString(), status, optionalText);
    }
}