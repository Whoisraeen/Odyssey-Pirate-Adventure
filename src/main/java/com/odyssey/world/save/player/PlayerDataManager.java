package com.odyssey.world.save.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages player data storage and retrieval for the world save system.
 * Handles player inventory, position, stats, achievements, and other persistent data.
 * 
 * <p>Player data is stored in JSON format with automatic backup and recovery.
 * Each player has a unique UUID-based file in the playerdata directory.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerDataManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDataManager.class);
    
    private final Path playerDataDir;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Cache for loaded player data
    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
    
    // Dirty tracking for efficient saving
    private final Map<UUID, Boolean> dirtyPlayers = new ConcurrentHashMap<>();
    
    /**
     * Creates a new player data manager.
     * 
     * @param worldSaveDir the world save directory
     * @throws IOException if the player data directory cannot be created
     */
    public PlayerDataManager(Path worldSaveDir) throws IOException {
        this.playerDataDir = worldSaveDir.resolve("playerdata");
        
        // Create player data directory
        Files.createDirectories(playerDataDir);
        
        // Configure Gson for pretty printing and null handling
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        
        logger.info("Initialized player data manager at: {}", playerDataDir);
    }
    
    /**
     * Loads player data from disk or creates new data if not found.
     * 
     * @param playerId the player's UUID
     * @return the player data
     * @throws IOException if the data cannot be loaded
     */
    public PlayerData loadPlayerData(UUID playerId) throws IOException {
        lock.readLock().lock();
        try {
            // Check cache first
            PlayerData cached = playerCache.get(playerId);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        lock.writeLock().lock();
        try {
            // Double-check cache after acquiring write lock
            PlayerData cached = playerCache.get(playerId);
            if (cached != null) {
                return cached;
            }
            
            Path playerFile = getPlayerFile(playerId);
            PlayerData playerData;
            
            if (Files.exists(playerFile)) {
                // Load existing player data
                try {
                    String json = Files.readString(playerFile);
                    playerData = gson.fromJson(json, PlayerData.class);
                    
                    // Validate loaded data
                    if (playerData == null || !playerId.equals(playerData.getPlayerId())) {
                        logger.warn("Invalid player data for {}, creating new", playerId);
                        playerData = createNewPlayerData(playerId);
                    } else {
                        logger.debug("Loaded player data for: {}", playerId);
                    }
                    
                } catch (JsonSyntaxException e) {
                    logger.error("Corrupted player data for {}, creating new", playerId, e);
                    
                    // Try to load backup
                    playerData = loadBackup(playerId);
                    if (playerData == null) {
                        playerData = createNewPlayerData(playerId);
                    }
                }
            } else {
                // Create new player data
                playerData = createNewPlayerData(playerId);
                logger.info("Created new player data for: {}", playerId);
            }
            
            // Cache the data
            playerCache.put(playerId, playerData);
            dirtyPlayers.put(playerId, false);
            
            return playerData;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Saves player data to disk.
     * 
     * @param playerId the player's UUID
     * @throws IOException if the data cannot be saved
     */
    public void savePlayerData(UUID playerId) throws IOException {
        lock.readLock().lock();
        try {
            PlayerData playerData = playerCache.get(playerId);
            if (playerData == null) {
                logger.warn("Attempted to save non-loaded player data: {}", playerId);
                return;
            }
            
            // Check if data is dirty
            Boolean isDirty = dirtyPlayers.get(playerId);
            if (isDirty == null || !isDirty) {
                return; // No changes to save
            }
            
            Path playerFile = getPlayerFile(playerId);
            Path backupFile = getBackupFile(playerId);
            Path tempFile = playerFile.resolveSibling(playerFile.getFileName() + ".tmp");
            
            try {
                // Update last saved timestamp
                playerData.setLastSaved(System.currentTimeMillis());
                
                // Serialize to JSON
                String json = gson.toJson(playerData);
                
                // Write to temporary file first
                Files.writeString(tempFile, json);
                
                // Create backup of existing file
                if (Files.exists(playerFile)) {
                    Files.copy(playerFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Atomically replace the original file
                Files.move(tempFile, playerFile, StandardCopyOption.REPLACE_EXISTING);
                
                // Mark as clean
                dirtyPlayers.put(playerId, false);
                
                logger.debug("Saved player data for: {}", playerId);
                
            } catch (IOException e) {
                // Clean up temp file on error
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupError) {
                    logger.warn("Failed to clean up temp file: {}", tempFile, cleanupError);
                }
                throw e;
            }
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Saves all loaded and dirty player data.
     * 
     * @throws IOException if any player data cannot be saved
     */
    public void saveAllPlayerData() throws IOException {
        lock.readLock().lock();
        try {
            for (UUID playerId : playerCache.keySet()) {
                Boolean isDirty = dirtyPlayers.get(playerId);
                if (isDirty != null && isDirty) {
                    savePlayerData(playerId);
                }
            }
            
            logger.debug("Saved all dirty player data");
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Marks player data as dirty (needing save).
     * 
     * @param playerId the player's UUID
     */
    public void markDirty(UUID playerId) {
        dirtyPlayers.put(playerId, true);
    }
    
    /**
     * Unloads player data from cache.
     * 
     * @param playerId the player's UUID
     * @throws IOException if the data cannot be saved before unloading
     */
    public void unloadPlayerData(UUID playerId) throws IOException {
        lock.writeLock().lock();
        try {
            // Save if dirty
            Boolean isDirty = dirtyPlayers.get(playerId);
            if (isDirty != null && isDirty) {
                savePlayerData(playerId);
            }
            
            // Remove from cache
            playerCache.remove(playerId);
            dirtyPlayers.remove(playerId);
            
            logger.debug("Unloaded player data for: {}", playerId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets cached player data without loading from disk.
     * 
     * @param playerId the player's UUID
     * @return the cached player data, or null if not loaded
     */
    public PlayerData getCachedPlayerData(UUID playerId) {
        return playerCache.get(playerId);
    }
    
    /**
     * Checks if player data exists on disk.
     * 
     * @param playerId the player's UUID
     * @return true if player data exists
     */
    public boolean hasPlayerData(UUID playerId) {
        return Files.exists(getPlayerFile(playerId));
    }
    
    /**
     * Gets the number of cached players.
     * 
     * @return the cache size
     */
    public int getCacheSize() {
        return playerCache.size();
    }
    
    /**
     * Gets the number of dirty players.
     * 
     * @return the dirty count
     */
    public int getDirtyCount() {
        return (int) dirtyPlayers.values().stream().mapToInt(dirty -> dirty ? 1 : 0).sum();
    }
    
    /**
     * Creates new player data with default values.
     * 
     * @param playerId the player's UUID
     * @return the new player data
     */
    private PlayerData createNewPlayerData(UUID playerId) {
        PlayerData playerData = new PlayerData(playerId);
        
        // Set default spawn position (0, 64, 0)
        playerData.setPosition(0.0, 64.0, 0.0);
        playerData.setRotation(0.0f, 0.0f);
        
        // Set default game mode and health
        playerData.setGameMode("survival");
        playerData.setHealth(20.0f);
        playerData.setFoodLevel(20);
        
        // Initialize empty inventory
        playerData.initializeInventory();
        
        // Set creation time
        long now = System.currentTimeMillis();
        playerData.setFirstPlayed(now);
        playerData.setLastPlayed(now);
        playerData.setLastSaved(now);
        
        return playerData;
    }
    
    /**
     * Attempts to load player data from backup file.
     * 
     * @param playerId the player's UUID
     * @return the backup player data, or null if not available
     */
    private PlayerData loadBackup(UUID playerId) {
        Path backupFile = getBackupFile(playerId);
        if (!Files.exists(backupFile)) {
            return null;
        }
        
        try {
            String json = Files.readString(backupFile);
            PlayerData playerData = gson.fromJson(json, PlayerData.class);
            
            if (playerData != null && playerId.equals(playerData.getPlayerId())) {
                logger.info("Restored player data from backup for: {}", playerId);
                return playerData;
            }
        } catch (Exception e) {
            logger.warn("Failed to load backup for player {}", playerId, e);
        }
        
        return null;
    }
    
    /**
     * Gets the file path for a player's data.
     * 
     * @param playerId the player's UUID
     * @return the file path
     */
    private Path getPlayerFile(UUID playerId) {
        return playerDataDir.resolve(playerId.toString() + ".json");
    }
    
    /**
     * Gets the backup file path for a player's data.
     * 
     * @param playerId the player's UUID
     * @return the backup file path
     */
    private Path getBackupFile(UUID playerId) {
        return playerDataDir.resolve(playerId.toString() + ".json.bak");
    }
}