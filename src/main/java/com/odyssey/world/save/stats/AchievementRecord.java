package com.odyssey.world.save.stats;

/**
 * Record of an achievement unlocked by a player.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AchievementRecord {
    private long bestValue;
    private long unlockedTime;
    
    /**
     * Creates a new achievement record.
     * 
     * @param bestValue the best value achieved
     * @param unlockedTime the time when unlocked
     */
    public AchievementRecord(long bestValue, long unlockedTime) {
        this.bestValue = bestValue;
        this.unlockedTime = unlockedTime;
    }
    
    /**
     * Default constructor for JSON deserialization.
     */
    public AchievementRecord() {
    }
    
    public long getBestValue() {
        return bestValue;
    }
    
    public void setBestValue(long bestValue) {
        this.bestValue = bestValue;
    }
    
    public long getUnlockedTime() {
        return unlockedTime;
    }
    
    public void setUnlockedTime(long unlockedTime) {
        this.unlockedTime = unlockedTime;
    }
}