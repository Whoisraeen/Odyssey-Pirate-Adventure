package com.odyssey.quest;

import com.odyssey.util.Logger;

import java.util.*;

/**
 * Represents a quest in the game with objectives, rewards, and progression tracking.
 */
public class Quest {
    private static final Logger LOGGER = Logger.getLogger(Quest.class);
    
    /** Unique identifier for this quest */
    private final String id;
    
    /** Display title of the quest */
    private final String title;
    
    /** Detailed description of the quest */
    private final String description;
    
    /** Maximum progress value for completion */
    private final int maxProgress;
    
    /** Experience reward for completion */
    private final int experienceReward;
    
    /** Gold reward for completion */
    private final int goldReward;
    
    /** Quest objectives */
    private final List<QuestObjective> objectives;
    
    /** Quest prerequisites (other quest IDs that must be completed) */
    private final List<String> prerequisites;
    
    /** Quest type/category */
    private QuestType type;
    
    /** Whether this quest can be repeated */
    private boolean repeatable;
    
    /** Quest difficulty level */
    private QuestDifficulty difficulty;
    
    /** Time limit for the quest (in seconds, 0 = no limit) */
    private long timeLimit;
    
    /** Time when quest was started */
    private long startTime;
    
    /**
     * Quest types for categorization.
     */
    public enum QuestType {
        MAIN_STORY,
        SIDE_QUEST,
        TUTORIAL,
        EXPLORATION,
        COMBAT,
        TRADING,
        CRAFTING,
        FACTION,
        DAILY,
        WEEKLY
    }
    
    /**
     * Quest difficulty levels.
     */
    public enum QuestDifficulty {
        EASY,
        MEDIUM,
        HARD,
        LEGENDARY
    }
    
    /**
     * Creates a new Quest.
     * 
     * @param id Unique quest identifier
     * @param title Quest title
     * @param description Quest description
     * @param maxProgress Maximum progress for completion
     * @param experienceReward Experience reward
     * @param goldReward Gold reward
     */
    public Quest(String id, String title, String description, int maxProgress, 
                 int experienceReward, int goldReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.maxProgress = maxProgress;
        this.experienceReward = experienceReward;
        this.goldReward = goldReward;
        this.objectives = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
        this.type = QuestType.SIDE_QUEST;
        this.repeatable = false;
        this.difficulty = QuestDifficulty.MEDIUM;
        this.timeLimit = 0;
        this.startTime = 0;
    }
    
    /**
     * Gets the quest ID.
     * 
     * @return Quest ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the quest title.
     * 
     * @return Quest title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the quest description.
     * 
     * @return Quest description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the maximum progress value.
     * 
     * @return Maximum progress
     */
    public int getMaxProgress() {
        return maxProgress;
    }
    
    /**
     * Gets the experience reward.
     * 
     * @return Experience reward
     */
    public int getExperienceReward() {
        return experienceReward;
    }
    
    /**
     * Gets the gold reward.
     * 
     * @return Gold reward
     */
    public int getGoldReward() {
        return goldReward;
    }
    
    /**
     * Gets the quest type.
     * 
     * @return Quest type
     */
    public QuestType getType() {
        return type;
    }
    
    /**
     * Sets the quest type.
     * 
     * @param type Quest type
     */
    public void setType(QuestType type) {
        this.type = type;
    }
    
    /**
     * Gets the quest difficulty.
     * 
     * @return Quest difficulty
     */
    public QuestDifficulty getDifficulty() {
        return difficulty;
    }
    
    /**
     * Sets the quest difficulty.
     * 
     * @param difficulty Quest difficulty
     */
    public void setDifficulty(QuestDifficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * Checks if the quest is repeatable.
     * 
     * @return true if repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }
    
    /**
     * Sets whether the quest is repeatable.
     * 
     * @param repeatable true if repeatable
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
    
    /**
     * Gets the time limit in seconds.
     * 
     * @return Time limit (0 = no limit)
     */
    public long getTimeLimit() {
        return timeLimit;
    }
    
    /**
     * Sets the time limit.
     * 
     * @param timeLimit Time limit in seconds (0 = no limit)
     */
    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }
    
    /**
     * Starts the quest timer.
     */
    public void start() {
        this.startTime = System.currentTimeMillis() / 1000;
        LOGGER.debug("Started quest '{}' at time {}", id, startTime);
    }
    
    /**
     * Gets the remaining time for the quest.
     * 
     * @return Remaining time in seconds, or -1 if no time limit
     */
    public long getRemainingTime() {
        if (timeLimit <= 0) {
            return -1; // No time limit
        }
        
        long currentTime = System.currentTimeMillis() / 1000;
        long elapsed = currentTime - startTime;
        return Math.max(0, timeLimit - elapsed);
    }
    
    /**
     * Checks if the quest has expired.
     * 
     * @return true if the quest has expired
     */
    public boolean hasExpired() {
        return timeLimit > 0 && getRemainingTime() <= 0;
    }
    
    /**
     * Adds an objective to the quest.
     * 
     * @param objective The objective to add
     */
    public void addObjective(QuestObjective objective) {
        objectives.add(objective);
        LOGGER.debug("Added objective '{}' to quest '{}'", objective.getDescription(), id);
    }
    
    /**
     * Gets all quest objectives.
     * 
     * @return List of objectives
     */
    public List<QuestObjective> getObjectives() {
        return new ArrayList<>(objectives);
    }
    
    /**
     * Adds a prerequisite quest.
     * 
     * @param questId The prerequisite quest ID
     */
    public void addPrerequisite(String questId) {
        prerequisites.add(questId);
        LOGGER.debug("Added prerequisite '{}' to quest '{}'", questId, id);
    }
    
    /**
     * Gets all prerequisite quest IDs.
     * 
     * @return List of prerequisite quest IDs
     */
    public List<String> getPrerequisites() {
        return new ArrayList<>(prerequisites);
    }
    
    /**
     * Checks if all prerequisites are met.
     * 
     * @param completedQuests Set of completed quest IDs
     * @return true if all prerequisites are met
     */
    public boolean arePrerequisitesMet(Set<String> completedQuests) {
        for (String prerequisite : prerequisites) {
            if (!completedQuests.contains(prerequisite)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Updates the quest (called each frame).
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Update objectives
        for (QuestObjective objective : objectives) {
            objective.update(deltaTime);
        }
        
        // Check for expiration
        if (hasExpired()) {
            LOGGER.warn("Quest '{}' has expired", id);
        }
    }
    
    /**
     * Gets a formatted string representation of the quest.
     * 
     * @return Formatted quest string
     */
    @Override
    public String toString() {
        return String.format("Quest[%s]: %s (%s) - %s", 
            id, title, type, description);
    }
    
    /**
     * Checks if this quest equals another object.
     * 
     * @param obj The object to compare
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Quest quest = (Quest) obj;
        return Objects.equals(id, quest.id);
    }
    
    /**
     * Gets the hash code for this quest.
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}