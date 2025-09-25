package com.odyssey.achievement;

import com.odyssey.util.Logger;

/**
 * Represents an individual achievement in the game.
 */
public class Achievement {
    private static final Logger LOGGER = Logger.getLogger(Achievement.class);
    
    /** Unique identifier for the achievement */
    private final String id;
    
    /** Display title of the achievement */
    private final String title;
    
    /** Description of what needs to be done */
    private final String description;
    
    /** Category this achievement belongs to */
    private final AchievementCategory category;
    
    /** Points awarded for unlocking this achievement */
    private final int points;
    
    /** Maximum progress needed to unlock (1 for simple achievements) */
    private final int maxProgress;
    
    /** Whether this achievement is hidden until unlocked */
    private final boolean hidden;
    
    /** Whether this achievement can only be unlocked once */
    private final boolean unique;
    
    /** Timestamp when this achievement was created */
    private final long createdTime;
    
    /**
     * Achievement categories for organization.
     */
    public enum AchievementCategory {
        TUTORIAL("Tutorial"),
        EXPLORATION("Exploration"),
        COMBAT("Combat"),
        TRADING("Trading"),
        CRAFTING("Crafting"),
        SOCIAL("Social"),
        SPECIAL("Special");
        
        private final String displayName;
        
        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Creates a new Achievement.
     * 
     * @param id Unique identifier
     * @param title Display title
     * @param description Achievement description
     * @param category Achievement category
     * @param points Points awarded
     * @param maxProgress Maximum progress needed
     */
    public Achievement(String id, String title, String description, 
                      AchievementCategory category, int points, int maxProgress) {
        this(id, title, description, category, points, maxProgress, false, true);
    }
    
    /**
     * Creates a new Achievement with full configuration.
     * 
     * @param id Unique identifier
     * @param title Display title
     * @param description Achievement description
     * @param category Achievement category
     * @param points Points awarded
     * @param maxProgress Maximum progress needed
     * @param hidden Whether achievement is hidden
     * @param unique Whether achievement can only be unlocked once
     */
    public Achievement(String id, String title, String description, 
                      AchievementCategory category, int points, int maxProgress,
                      boolean hidden, boolean unique) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.points = Math.max(0, points);
        this.maxProgress = Math.max(1, maxProgress);
        this.hidden = hidden;
        this.unique = unique;
        this.createdTime = System.currentTimeMillis();
        
        LOGGER.debug("Created achievement '{}': {} ({} points)", id, title, points);
    }
    
    /**
     * Gets the achievement ID.
     * 
     * @return The unique identifier
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the achievement title.
     * 
     * @return The display title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the achievement description.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the achievement category.
     * 
     * @return The category
     */
    public AchievementCategory getCategory() {
        return category;
    }
    
    /**
     * Gets the points awarded for this achievement.
     * 
     * @return The point value
     */
    public int getPoints() {
        return points;
    }
    
    /**
     * Gets the maximum progress needed to unlock.
     * 
     * @return The maximum progress value
     */
    public int getMaxProgress() {
        return maxProgress;
    }
    
    /**
     * Checks if this achievement is hidden.
     * 
     * @return true if hidden until unlocked
     */
    public boolean isHidden() {
        return hidden;
    }
    
    /**
     * Checks if this achievement is unique.
     * 
     * @return true if can only be unlocked once
     */
    public boolean isUnique() {
        return unique;
    }
    
    /**
     * Gets the creation timestamp.
     * 
     * @return Creation time in milliseconds
     */
    public long getCreatedTime() {
        return createdTime;
    }
    
    /**
     * Checks if this is a simple achievement (maxProgress = 1).
     * 
     * @return true if simple achievement
     */
    public boolean isSimple() {
        return maxProgress == 1;
    }
    
    /**
     * Gets the progress percentage for a given progress value.
     * 
     * @param currentProgress The current progress
     * @return Progress percentage (0.0 to 1.0)
     */
    public float getProgressPercentage(int currentProgress) {
        if (maxProgress <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.max(0.0f, (float) currentProgress / maxProgress));
    }
    
    /**
     * Formats the progress as a string.
     * 
     * @param currentProgress The current progress
     * @return Formatted progress string
     */
    public String formatProgress(int currentProgress) {
        if (isSimple()) {
            return currentProgress >= maxProgress ? "Complete" : "Incomplete";
        }
        return String.format("%d / %d", Math.min(currentProgress, maxProgress), maxProgress);
    }
    
    /**
     * Updates the achievement (for time-based achievements).
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Override in subclasses for time-based achievements
    }
    
    /**
     * Validates the achievement configuration.
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() &&
               title != null && !title.trim().isEmpty() &&
               description != null && !description.trim().isEmpty() &&
               category != null &&
               points >= 0 &&
               maxProgress > 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Achievement that = (Achievement) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("Achievement{id='%s', title='%s', category=%s, points=%d, maxProgress=%d}", 
                           id, title, category, points, maxProgress);
    }
}