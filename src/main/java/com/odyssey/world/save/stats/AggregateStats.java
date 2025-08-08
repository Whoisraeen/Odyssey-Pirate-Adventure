package com.odyssey.world.save.stats;

import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregate statistics across all players on the server.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AggregateStats {
    private int totalPlayers;
    private Map<StatType, Long> totalStats;
    private Map<StatType, Long> averageStats;
    private Map<StatType, Long> maxStats;
    private Map<MaritimeAchievement, Integer> achievementCounts;
    
    public AggregateStats() {
        this.totalPlayers = 0;
        this.totalStats = new EnumMap<>(StatType.class);
        this.averageStats = new EnumMap<>(StatType.class);
        this.maxStats = new EnumMap<>(StatType.class);
        this.achievementCounts = new EnumMap<>(MaritimeAchievement.class);
        
        // Initialize with zeros
        for (StatType statType : StatType.values()) {
            totalStats.put(statType, 0L);
            averageStats.put(statType, 0L);
            maxStats.put(statType, 0L);
        }
        
        for (MaritimeAchievement achievement : MaritimeAchievement.values()) {
            achievementCounts.put(achievement, 0);
        }
    }
    
    public void addPlayerStats(PlayerStats playerStats) {
        totalPlayers++;
        
        // Update totals and maximums
        for (StatType statType : StatType.values()) {
            long playerValue = playerStats.getStat(statType);
            long currentTotal = totalStats.get(statType);
            long currentMax = maxStats.get(statType);
            
            totalStats.put(statType, currentTotal + playerValue);
            if (playerValue > currentMax) {
                maxStats.put(statType, playerValue);
            }
        }
        
        // Update achievement counts
        for (MaritimeAchievement achievement : MaritimeAchievement.values()) {
            if (playerStats.hasAchievement(achievement)) {
                int currentCount = achievementCounts.get(achievement);
                achievementCounts.put(achievement, currentCount + 1);
            }
        }
        
        // Recalculate averages
        calculateAverages();
    }
    
    private void calculateAverages() {
        if (totalPlayers == 0) return;
        
        for (StatType statType : StatType.values()) {
            long total = totalStats.get(statType);
            long average = total / totalPlayers;
            averageStats.put(statType, average);
        }
    }
    
    public int getTotalPlayers() { return totalPlayers; }
    public Map<StatType, Long> getTotalStats() { return new EnumMap<>(totalStats); }
    public Map<StatType, Long> getAverageStats() { return new EnumMap<>(averageStats); }
    public Map<StatType, Long> getMaxStats() { return new EnumMap<>(maxStats); }
    public Map<MaritimeAchievement, Integer> getAchievementCounts() { return new EnumMap<>(achievementCounts); }
}