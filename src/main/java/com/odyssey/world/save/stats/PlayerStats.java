package com.odyssey.world.save.stats;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents individual player statistics for maritime gameplay.
 * Stores both standard and custom statistics with timestamps.
 * 
 * <p>Features:
 * <ul>
 * <li>Standard maritime statistics</li>
 * <li>Custom statistic support</li>
 * <li>Achievement tracking</li>
 * <li>Timestamp management</li>
 * <li>JSON serialization support</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerStats {
    private UUID playerId;
    private long createdTime;
    private long lastUpdated;
    private long totalPlayTime;
    
    // Standard statistics
    private Map<StatType, Long> stats;
    
    // Custom statistics
    private Map<String, Long> customStats;
    
    // Achievement records
    private Map<MaritimeAchievement, AchievementRecord> achievements;
    
    /**
     * Creates new player statistics.
     * 
     * @param playerId the player UUID
     */
    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.createdTime = Instant.now().toEpochMilli();
        this.lastUpdated = this.createdTime;
        this.totalPlayTime = 0;
        this.stats = new EnumMap<>(StatType.class);
        this.customStats = new HashMap<>();
        this.achievements = new EnumMap<>(MaritimeAchievement.class);
        
        initializeDefaultStats();
    }
    
    /**
     * Default constructor for JSON deserialization.
     */
    public PlayerStats() {
        this.stats = new EnumMap<>(StatType.class);
        this.customStats = new HashMap<>();
        this.achievements = new EnumMap<>(MaritimeAchievement.class);
    }
    
    /**
     * Initializes default statistic values.
     */
    private void initializeDefaultStats() {
        for (StatType statType : StatType.values()) {
            stats.put(statType, 0L);
        }
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
     * Gets the total play time in milliseconds.
     * 
     * @return the total play time
     */
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    /**
     * Sets the total play time.
     * 
     * @param totalPlayTime the total play time in milliseconds
     */
    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
        updateTimestamp();
    }
    
    /**
     * Adds play time to the total.
     * 
     * @param playTime the play time to add in milliseconds
     */
    public void addPlayTime(long playTime) {
        this.totalPlayTime += playTime;
        updateTimestamp();
    }
    
    /**
     * Gets a statistic value.
     * 
     * @param statType the statistic type
     * @return the statistic value
     */
    public long getStat(StatType statType) {
        return stats.getOrDefault(statType, 0L);
    }
    
    /**
     * Sets a statistic value.
     * 
     * @param statType the statistic type
     * @param value the value to set
     */
    public void setStat(StatType statType, long value) {
        stats.put(statType, value);
        updateTimestamp();
    }
    
    /**
     * Increments a statistic value.
     * 
     * @param statType the statistic type
     * @param amount the amount to increment
     */
    public void incrementStat(StatType statType, long amount) {
        long currentValue = getStat(statType);
        setStat(statType, currentValue + amount);
    }
    
    /**
     * Gets all standard statistics.
     * 
     * @return map of statistics
     */
    public Map<StatType, Long> getStats() {
        return new EnumMap<>(stats);
    }
    
    /**
     * Gets a custom statistic value.
     * 
     * @param statName the custom statistic name
     * @return the statistic value, or 0 if not found
     */
    public long getCustomStat(String statName) {
        return customStats.getOrDefault(statName, 0L);
    }
    
    /**
     * Sets a custom statistic value.
     * 
     * @param statName the custom statistic name
     * @param value the value to set
     */
    public void setCustomStat(String statName, long value) {
        customStats.put(statName, value);
        updateTimestamp();
    }
    
    /**
     * Adds to a custom statistic value.
     * 
     * @param statName the custom statistic name
     * @param amount the amount to add
     */
    public void addCustomStat(String statName, long amount) {
        long currentValue = getCustomStat(statName);
        setCustomStat(statName, currentValue + amount);
    }
    
    /**
     * Gets all custom statistics.
     * 
     * @return map of custom statistics
     */
    public Map<String, Long> getCustomStats() {
        return new HashMap<>(customStats);
    }
    
    /**
     * Records an achievement.
     * 
     * @param achievement the achievement type
     * @param value the achievement value
     */
    public void recordAchievement(MaritimeAchievement achievement, long value) {
        AchievementRecord record = achievements.get(achievement);
        if (record == null || value > record.getBestValue()) {
            achievements.put(achievement, new AchievementRecord(value, Instant.now().toEpochMilli()));
            updateTimestamp();
        }
    }
    
    /**
     * Gets an achievement record.
     * 
     * @param achievement the achievement type
     * @return the achievement record, or null if not achieved
     */
    public AchievementRecord getAchievement(MaritimeAchievement achievement) {
        return achievements.get(achievement);
    }
    
    /**
     * Gets all achievements.
     * 
     * @return map of achievements
     */
    public Map<MaritimeAchievement, AchievementRecord> getAchievements() {
        return new EnumMap<>(achievements);
    }
    
    /**
     * Checks if an achievement has been unlocked.
     * 
     * @param achievement the achievement type
     * @return true if the achievement has been unlocked
     */
    public boolean hasAchievement(MaritimeAchievement achievement) {
        return achievements.containsKey(achievement);
    }
    
    /**
     * Gets the number of achievements unlocked.
     * 
     * @return the number of achievements
     */
    public int getAchievementCount() {
        return achievements.size();
    }
    
    /**
     * Calculates the player's maritime experience level.
     * 
     * @return the experience level
     */
    public int getMaritimeLevel() {
        long totalExperience = getStat(StatType.DISTANCE_SAILED) / 1000 +
                              getStat(StatType.TREASURES_FOUND) * 100 +
                              getStat(StatType.SHIPS_BUILT) * 50 +
                              getStat(StatType.FISH_CAUGHT) * 10 +
                              getStat(StatType.BATTLES_WON) * 200;
        
        // Simple level calculation: sqrt(experience / 1000)
        return (int) Math.sqrt(totalExperience / 1000.0);
    }
    
    /**
     * Gets the player's maritime rank based on achievements and stats.
     * 
     * @return the maritime rank
     */
    public MaritimeRank getMaritimeRank() {
        int level = getMaritimeLevel();
        int achievementCount = getAchievementCount();
        
        if (level >= 50 && achievementCount >= 20) {
            return MaritimeRank.LEGENDARY_CAPTAIN;
        } else if (level >= 30 && achievementCount >= 15) {
            return MaritimeRank.MASTER_NAVIGATOR;
        } else if (level >= 20 && achievementCount >= 10) {
            return MaritimeRank.SEASONED_SAILOR;
        } else if (level >= 10 && achievementCount >= 5) {
            return MaritimeRank.EXPERIENCED_MARINER;
        } else if (level >= 5) {
            return MaritimeRank.APPRENTICE_SAILOR;
        } else {
            return MaritimeRank.LANDLUBBER;
        }
    }
    
    /**
     * Checks if the statistics are empty.
     * 
     * @return true if all statistics are zero or empty
     */
    public boolean isEmpty() {
        boolean hasStats = stats.values().stream().anyMatch(value -> value > 0);
        boolean hasCustomStats = !customStats.isEmpty() && customStats.values().stream().anyMatch(value -> value > 0);
        boolean hasAchievements = !achievements.isEmpty();
        
        return !hasStats && !hasCustomStats && !hasAchievements && totalPlayTime == 0;
    }
    
    /**
     * Updates the last updated timestamp.
     */
    private void updateTimestamp() {
        this.lastUpdated = Instant.now().toEpochMilli();
    }
    
    /**
     * Creates a summary of the player's statistics.
     * 
     * @return statistics summary
     */
    public StatsSummary createSummary() {
        return new StatsSummary(
            playerId,
            getMaritimeLevel(),
            getMaritimeRank(),
            getStat(StatType.DISTANCE_SAILED),
            getStat(StatType.TREASURES_FOUND),
            getStat(StatType.BATTLES_WON),
            getAchievementCount(),
            totalPlayTime
        );
    }
    
    @Override
    public String toString() {
        return String.format("PlayerStats{playerId=%s, level=%d, rank=%s, achievements=%d}", 
            playerId, getMaritimeLevel(), getMaritimeRank(), getAchievementCount());
    }
}