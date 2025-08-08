package com.odyssey.world.save.stats;

import java.util.UUID;

/**
 * Summary of player statistics for quick overview.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class StatsSummary {
    private final UUID playerId;
    private final int maritimeLevel;
    private final MaritimeRank maritimeRank;
    private final long distanceSailed;
    private final long treasuresFound;
    private final long battlesWon;
    private final int achievementCount;
    private final long totalPlayTime;
    
    public StatsSummary(UUID playerId, int maritimeLevel, MaritimeRank maritimeRank,
                       long distanceSailed, long treasuresFound, long battlesWon,
                       int achievementCount, long totalPlayTime) {
        this.playerId = playerId;
        this.maritimeLevel = maritimeLevel;
        this.maritimeRank = maritimeRank;
        this.distanceSailed = distanceSailed;
        this.treasuresFound = treasuresFound;
        this.battlesWon = battlesWon;
        this.achievementCount = achievementCount;
        this.totalPlayTime = totalPlayTime;
    }
    
    public UUID getPlayerId() { return playerId; }
    public int getMaritimeLevel() { return maritimeLevel; }
    public MaritimeRank getMaritimeRank() { return maritimeRank; }
    public long getDistanceSailed() { return distanceSailed; }
    public long getTreasuresFound() { return treasuresFound; }
    public long getBattlesWon() { return battlesWon; }
    public int getAchievementCount() { return achievementCount; }
    public long getTotalPlayTime() { return totalPlayTime; }
}