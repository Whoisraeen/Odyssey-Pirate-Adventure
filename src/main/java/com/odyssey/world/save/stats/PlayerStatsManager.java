package com.odyssey.world.save.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages per-player statistics storage in the stats/ folder.
 * Handles stat tracking, persistence, and aggregation for maritime gameplay.
 * 
 * <p>Features:
 * <ul>
 * <li>Per-player stat files in JSON format</li>
 * <li>Maritime-specific statistics</li>
 * <li>Automatic stat persistence</li>
 * <li>Thread-safe operations</li>
 * <li>Stat aggregation and leaderboards</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerStatsManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerStatsManager.class);
    
    private static final String STATS_FILE_EXTENSION = ".json";
    
    private final Path statsDirectory;
    private final Gson gson;
    private final Map<UUID, PlayerStats> loadedStats;
    private final ReadWriteLock statsLock;
    
    /**
     * Creates a new player stats manager.
     * 
     * @param worldDirectory the world directory
     */
    public PlayerStatsManager(Path worldDirectory) {
        this.statsDirectory = worldDirectory.resolve(WorldSaveFormat.STATS_DIR);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        this.loadedStats = new ConcurrentHashMap<>();
        this.statsLock = new ReentrantReadWriteLock();
        
        initializeDirectory();
    }
    
    /**
     * Initializes the stats directory structure.
     */
    private void initializeDirectory() {
        try {
            Files.createDirectories(statsDirectory);
            logger.debug("Initialized stats directory: {}", statsDirectory);
        } catch (IOException e) {
            logger.error("Failed to create stats directory", e);
        }
    }
    
    /**
     * Gets player statistics, loading from disk if necessary.
     * 
     * @param playerId the player UUID
     * @return the player statistics
     */
    public PlayerStats getPlayerStats(UUID playerId) {
        statsLock.readLock().lock();
        try {
            return loadedStats.computeIfAbsent(playerId, this::loadPlayerStats);
        } finally {
            statsLock.readLock().unlock();
        }
    }
    
    /**
     * Increments a statistic for a player.
     * 
     * @param playerId the player UUID
     * @param statType the statistic type
     * @param amount the amount to increment
     */
    public void incrementStat(UUID playerId, StatType statType, long amount) {
        PlayerStats stats = getPlayerStats(playerId);
        stats.incrementStat(statType, amount);
        savePlayerStats(playerId, stats);
    }
    
    /**
     * Sets a statistic value for a player.
     * 
     * @param playerId the player UUID
     * @param statType the statistic type
     * @param value the value to set
     */
    public void setStat(UUID playerId, StatType statType, long value) {
        PlayerStats stats = getPlayerStats(playerId);
        stats.setStat(statType, value);
        savePlayerStats(playerId, stats);
    }
    
    /**
     * Gets a statistic value for a player.
     * 
     * @param playerId the player UUID
     * @param statType the statistic type
     * @return the statistic value
     */
    public long getStat(UUID playerId, StatType statType) {
        PlayerStats stats = getPlayerStats(playerId);
        return stats.getStat(statType);
    }
    
    /**
     * Adds a custom statistic for a player.
     * 
     * @param playerId the player UUID
     * @param statName the custom statistic name
     * @param value the value to add
     */
    public void addCustomStat(UUID playerId, String statName, long value) {
        PlayerStats stats = getPlayerStats(playerId);
        stats.addCustomStat(statName, value);
        savePlayerStats(playerId, stats);
    }
    
    /**
     * Gets a custom statistic for a player.
     * 
     * @param playerId the player UUID
     * @param statName the custom statistic name
     * @return the statistic value, or 0 if not found
     */
    public long getCustomStat(UUID playerId, String statName) {
        PlayerStats stats = getPlayerStats(playerId);
        return stats.getCustomStat(statName);
    }
    
    /**
     * Records a maritime achievement for a player.
     * 
     * @param playerId the player UUID
     * @param achievement the achievement type
     * @param value the achievement value (distance, time, etc.)
     */
    public void recordAchievement(UUID playerId, MaritimeAchievement achievement, long value) {
        PlayerStats stats = getPlayerStats(playerId);
        
        // Update relevant statistics
        switch (achievement) {
            case LONGEST_VOYAGE:
                if (value > stats.getStat(StatType.LONGEST_VOYAGE_DISTANCE)) {
                    stats.setStat(StatType.LONGEST_VOYAGE_DISTANCE, value);
                }
                break;
            case DEEPEST_DIVE:
                if (value > stats.getStat(StatType.DEEPEST_DIVE)) {
                    stats.setStat(StatType.DEEPEST_DIVE, value);
                }
                break;
            case FASTEST_SHIP:
                if (value > stats.getStat(StatType.FASTEST_SHIP_SPEED)) {
                    stats.setStat(StatType.FASTEST_SHIP_SPEED, value);
                }
                break;
            case LARGEST_TREASURE:
                if (value > stats.getStat(StatType.LARGEST_TREASURE_FOUND)) {
                    stats.setStat(StatType.LARGEST_TREASURE_FOUND, value);
                }
                break;
        }
        
        stats.recordAchievement(achievement, value);
        savePlayerStats(playerId, stats);
    }
    
    /**
     * Saves player statistics to disk.
     * 
     * @param playerId the player UUID
     * @param stats the player statistics
     */
    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        Path statsFile = getStatsFile(playerId);
        
        try {
            String json = gson.toJson(stats);
            Files.writeString(statsFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Saved stats for player: {}", playerId);
        } catch (IOException e) {
            logger.error("Failed to save stats for player: {}", playerId, e);
        }
    }
    
    /**
     * Loads player statistics from disk.
     * 
     * @param playerId the player UUID
     * @return the loaded player statistics
     */
    private PlayerStats loadPlayerStats(UUID playerId) {
        Path statsFile = getStatsFile(playerId);
        
        if (!Files.exists(statsFile)) {
            return new PlayerStats(playerId);
        }
        
        try {
            String json = Files.readString(statsFile);
            PlayerStats stats = gson.fromJson(json, PlayerStats.class);
            logger.debug("Loaded stats for player: {}", playerId);
            return stats != null ? stats : new PlayerStats(playerId);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load stats for player: {}", playerId, e);
            return new PlayerStats(playerId);
        }
    }
    
    /**
     * Unloads player statistics from memory.
     * 
     * @param playerId the player UUID
     */
    public void unloadPlayerStats(UUID playerId) {
        statsLock.writeLock().lock();
        try {
            PlayerStats stats = loadedStats.remove(playerId);
            if (stats != null) {
                savePlayerStats(playerId, stats);
                logger.debug("Unloaded stats for player: {}", playerId);
            }
        } finally {
            statsLock.writeLock().unlock();
        }
    }
    
    /**
     * Saves all loaded player statistics.
     */
    public void saveAllStats() {
        statsLock.readLock().lock();
        try {
            for (Map.Entry<UUID, PlayerStats> entry : loadedStats.entrySet()) {
                savePlayerStats(entry.getKey(), entry.getValue());
            }
            logger.info("Saved stats for {} players", loadedStats.size());
        } finally {
            statsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the top players for a specific statistic.
     * 
     * @param statType the statistic type
     * @param limit the maximum number of players to return
     * @return map of player IDs to statistic values, sorted by value descending
     */
    public Map<UUID, Long> getTopPlayers(StatType statType, int limit) {
        Map<UUID, Long> topPlayers = new ConcurrentHashMap<>();
        
        try {
            Files.walk(statsDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(STATS_FILE_EXTENSION))
                .limit(1000) // Prevent excessive memory usage
                .forEach(statsFile -> {
                    try {
                        String fileName = statsFile.getFileName().toString();
                        String uuidString = fileName.substring(0, fileName.length() - STATS_FILE_EXTENSION.length());
                        UUID playerId = UUID.fromString(uuidString);
                        
                        PlayerStats stats = loadPlayerStats(playerId);
                        long statValue = stats.getStat(statType);
                        if (statValue > 0) {
                            topPlayers.put(playerId, statValue);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to process stats file: {}", statsFile.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to scan stats directory for leaderboard", e);
        }
        
        return topPlayers.entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(ConcurrentHashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()), 
                    ConcurrentHashMap::putAll);
    }
    
    /**
     * Gets aggregate statistics across all players.
     * 
     * @return aggregate statistics
     */
    public AggregateStats getAggregateStats() {
        AggregateStats aggregate = new AggregateStats();
        
        try {
            Files.walk(statsDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(STATS_FILE_EXTENSION))
                .forEach(statsFile -> {
                    try {
                        String fileName = statsFile.getFileName().toString();
                        String uuidString = fileName.substring(0, fileName.length() - STATS_FILE_EXTENSION.length());
                        UUID playerId = UUID.fromString(uuidString);
                        
                        PlayerStats stats = loadPlayerStats(playerId);
                        aggregate.addPlayerStats(stats);
                    } catch (Exception e) {
                        logger.warn("Failed to process stats file for aggregation: {}", statsFile.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to scan stats directory for aggregation", e);
        }
        
        return aggregate;
    }
    
    /**
     * Gets the stats file path for a player.
     * 
     * @param playerId the player UUID
     * @return the stats file path
     */
    private Path getStatsFile(UUID playerId) {
        return statsDirectory.resolve(playerId.toString() + STATS_FILE_EXTENSION);
    }
    
    /**
     * Cleans up old or corrupted stats files.
     */
    public void cleanup() {
        try {
            Files.walk(statsDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(STATS_FILE_EXTENSION))
                .forEach(this::cleanupStatsFile);
        } catch (IOException e) {
            logger.error("Failed to cleanup stats files", e);
        }
    }
    
    /**
     * Cleans up a specific stats file.
     * 
     * @param statsFile the stats file to clean up
     */
    private void cleanupStatsFile(Path statsFile) {
        try {
            String json = Files.readString(statsFile);
            PlayerStats stats = gson.fromJson(json, PlayerStats.class);
            
            if (stats == null || stats.isEmpty()) {
                Files.deleteIfExists(statsFile);
                logger.debug("Deleted empty stats file: {}", statsFile.getFileName());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.warn("Failed to cleanup stats file: {}", statsFile.getFileName(), e);
        }
    }
    
    /**
     * Closes the stats manager and saves all data.
     */
    public void close() {
        saveAllStats();
        loadedStats.clear();
        logger.info("Player stats manager closed");
    }
}