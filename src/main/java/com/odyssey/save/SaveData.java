package com.odyssey.save;

import com.odyssey.core.GameState;
import com.odyssey.ship.Ship;
import org.joml.Vector3f;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a complete game save containing all necessary data to restore game state.
 * This class is serializable and contains all player progress, world state, and game configuration.
 */
public class SaveData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Save metadata
    private final UUID saveId;
    private final String saveName;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private String gameVersion;
    private long playtimeSeconds;
    
    // Player data
    private PlayerSaveData playerData;
    
    // World data
    private WorldSaveData worldData;
    
    // Ships data
    private List<Ship> playerShips;
    private String activeShipId;
    
    // Game state
    private GameState gameState;
    private Map<String, Object> gameFlags;
    
    // Progress tracking
    private Map<String, Integer> questProgress;
    private Map<String, Boolean> achievements;
    private Map<String, Object> statistics;
    
    /**
     * Constructor for creating a new save.
     */
    public SaveData(String saveName, String gameVersion) {
        this.saveId = UUID.randomUUID();
        this.saveName = saveName;
        this.gameVersion = gameVersion;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.playtimeSeconds = 0;
        this.gameState = GameState.MAIN_MENU;
    }
    
    /**
     * Update the last modified timestamp.
     */
    public void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    // Getters and setters
    
    public UUID getSaveId() {
        return saveId;
    }
    
    public String getSaveName() {
        return saveName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public String getGameVersion() {
        return gameVersion;
    }
    
    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }
    
    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }
    
    public void setPlaytimeSeconds(long playtimeSeconds) {
        this.playtimeSeconds = playtimeSeconds;
    }
    
    public void addPlaytime(long seconds) {
        this.playtimeSeconds += seconds;
    }
    
    public PlayerSaveData getPlayerData() {
        return playerData;
    }
    
    public void setPlayerData(PlayerSaveData playerData) {
        this.playerData = playerData;
    }
    
    public WorldSaveData getWorldData() {
        return worldData;
    }
    
    public void setWorldData(WorldSaveData worldData) {
        this.worldData = worldData;
    }
    
    public List<Ship> getPlayerShips() {
        return playerShips;
    }
    
    public void setPlayerShips(List<Ship> playerShips) {
        this.playerShips = playerShips;
    }
    
    /**
     * Alias for getPlayerShips() for compatibility.
     */
    public List<Ship> getShips() {
        return getPlayerShips();
    }
    
    public String getActiveShipId() {
        return activeShipId;
    }
    
    public void setActiveShipId(String activeShipId) {
        this.activeShipId = activeShipId;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
    
    public Map<String, Object> getGameFlags() {
        return gameFlags;
    }
    
    public void setGameFlags(Map<String, Object> gameFlags) {
        this.gameFlags = gameFlags;
    }
    
    public Map<String, Integer> getQuestProgress() {
        return questProgress;
    }
    
    public void setQuestProgress(Map<String, Integer> questProgress) {
        this.questProgress = questProgress;
    }
    
    public Map<String, Boolean> getAchievements() {
        return achievements;
    }
    
    public void setAchievements(Map<String, Boolean> achievements) {
        this.achievements = achievements;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    /**
     * Get a formatted string representation of playtime.
     */
    public String getFormattedPlaytime() {
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        long seconds = playtimeSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Check if this save is compatible with the current game version.
     */
    public boolean isCompatible(String currentVersion) {
        // Simple version compatibility check
        // In a real implementation, this would be more sophisticated
        return gameVersion != null && gameVersion.equals(currentVersion);
    }
    
    @Override
    public String toString() {
        return String.format("SaveData{name='%s', created=%s, playtime=%s, version=%s}", 
                           saveName, createdAt, getFormattedPlaytime(), gameVersion);
    }
    
    /**
     * Inner class for player-specific save data.
     */
    public static class PlayerSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String playerName;
        private transient Vector3f position;
        private transient Vector3f rotation;
        private transient Vector3f velocity;
        private float health;
        private float maxHealth;
        private int level;
        private long experience;
        private Map<String, Integer> inventory;
        private Map<String, Object> playerStats;
        
        // Additional player state data
        private boolean isOnShip;
        private String shipId;
        private transient Vector3f spawnPoint;
        private long gameTime;
        
        // Constructors, getters, and setters
        public PlayerSaveData() {}
        
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        
        public Vector3f getPosition() { return position; }
        public void setPosition(Vector3f position) { this.position = position; }
        
        public Vector3f getRotation() { return rotation; }
        public void setRotation(Vector3f rotation) { this.rotation = rotation; }
        
        public Vector3f getVelocity() { return velocity; }
        public void setVelocity(Vector3f velocity) { this.velocity = velocity; }
        
        public float getHealth() { return health; }
        public void setHealth(float health) { this.health = health; }
        
        public float getMaxHealth() { return maxHealth; }
        public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
        
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        
        public long getExperience() { return experience; }
        public void setExperience(long experience) { this.experience = experience; }
        
        public Map<String, Integer> getInventory() { return inventory; }
        public void setInventory(Map<String, Integer> inventory) { this.inventory = inventory; }
        
        public Map<String, Object> getPlayerStats() { return playerStats; }
        public void setPlayerStats(Map<String, Object> playerStats) { this.playerStats = playerStats; }
        
        public boolean isOnShip() { return isOnShip; }
        public void setOnShip(boolean onShip) { this.isOnShip = onShip; }
        
        public String getShipId() { return shipId; }
        public void setShipId(String shipId) { this.shipId = shipId; }
        
        public Vector3f getSpawnPoint() { return spawnPoint; }
        public void setSpawnPoint(Vector3f spawnPoint) { this.spawnPoint = spawnPoint; }
        
        public long getGameTime() { return gameTime; }
        public void setGameTime(long gameTime) { this.gameTime = gameTime; }
    }
    
    /**
     * Inner class for world-specific save data.
     */
    public static class WorldSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String worldName;
        private long worldSeed;
        private long gameTime;
        private String weatherState;
        private Map<String, Object> worldFlags;
        private byte[] chunkData; // Compressed chunk data
        
        // Constructors, getters, and setters
        public WorldSaveData() {}
        
        public String getWorldName() { return worldName; }
        public void setWorldName(String worldName) { this.worldName = worldName; }
        
        public long getWorldSeed() { return worldSeed; }
        public void setWorldSeed(long worldSeed) { this.worldSeed = worldSeed; }
        
        public long getGameTime() { return gameTime; }
        public void setGameTime(long gameTime) { this.gameTime = gameTime; }
        
        public String getWeatherState() { return weatherState; }
        public void setWeatherState(String weatherState) { this.weatherState = weatherState; }
        
        public Map<String, Object> getWorldFlags() { return worldFlags; }
        public void setWorldFlags(Map<String, Object> worldFlags) { this.worldFlags = worldFlags; }
        
        public byte[] getChunkData() { return chunkData; }
        public void setChunkData(byte[] chunkData) { this.chunkData = chunkData; }
    }
}