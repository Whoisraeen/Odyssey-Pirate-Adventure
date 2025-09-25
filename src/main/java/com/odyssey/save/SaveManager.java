package com.odyssey.save;

import com.odyssey.core.GameConfig;
import com.odyssey.core.GameEngine;
import com.odyssey.core.GameState;
import com.odyssey.ship.Ship;
import com.odyssey.world.WorldManager;
import com.odyssey.player.PlayerManager;
import com.odyssey.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages game save and load operations.
 * Handles file I/O, compression, validation, and provides both synchronous and asynchronous operations.
 */
public class SaveManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveManager.class);
    
    // Save system constants
    private static final String SAVE_DIRECTORY = "saves";
    private static final String SAVE_EXTENSION = ".odyssey";
    private static final String BACKUP_EXTENSION = ".backup";
    private static final String TEMP_EXTENSION = ".tmp";
    private static final int MAX_BACKUP_COUNT = 5;
    private static final String GAME_VERSION = "1.0.0"; // This should come from build info
    
    // File paths
    private final Path saveDirectory;
    private final Path backupDirectory;
    
    // Dependencies
    private final GameConfig config;
    private GameEngine gameEngine;
    private WorldManager worldManager;
    
    // Threading
    private final ExecutorService saveExecutor;
    
    // Cache
    private final Map<String, SaveData> saveCache;
    private List<SaveInfo> availableSaves;
    
    /**
     * Constructor for SaveManager.
     */
    public SaveManager(GameConfig config) {
        this.config = config;
        this.saveCache = new HashMap<>();
        this.saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SaveManager");
            thread.setDaemon(true);
            return thread;
        });
        
        // Initialize save directories
        this.saveDirectory = Paths.get(SAVE_DIRECTORY);
        this.backupDirectory = saveDirectory.resolve("backups");
        
        try {
            Files.createDirectories(saveDirectory);
            Files.createDirectories(backupDirectory);
            LOGGER.info("Save directories initialized: {}", saveDirectory.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create save directories", e);
            throw new RuntimeException("Could not initialize save system", e);
        }
        
        // Load available saves
        refreshSaveList();
    }
    
    /**
     * Set the game engine reference (called after initialization).
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }
    
    /**
     * Set the world manager reference (called after initialization).
     */
    public void setWorldManager(WorldManager worldManager) {
        this.worldManager = worldManager;
    }
    
    /**
     * Create a new save with the current game state.
     */
    public CompletableFuture<Boolean> saveGame(String saveName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Creating save: {}", saveName);
                
                // Create save data
                SaveData saveData = createSaveData(saveName);
                
                // Generate filename
                String filename = generateSaveFilename(saveName);
                Path savePath = saveDirectory.resolve(filename);
                Path tempPath = saveDirectory.resolve(filename + TEMP_EXTENSION);
                
                // Create backup if save already exists
                if (Files.exists(savePath)) {
                    createBackup(savePath);
                }
                
                // Write to temporary file first
                writeSaveData(saveData, tempPath);
                
                // Atomic move to final location
                Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update cache
                saveCache.put(saveName, saveData);
                refreshSaveList();
                
                LOGGER.info("Save created successfully: {}", saveName);
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Failed to save game: {}", saveName, e);
                return false;
            }
        }, saveExecutor);
    }
    
    /**
     * Load a game save.
     */
    public CompletableFuture<SaveData> loadGame(String saveName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Loading save: {}", saveName);
                
                // Check cache first
                if (saveCache.containsKey(saveName)) {
                    LOGGER.debug("Loading from cache: {}", saveName);
                    return saveCache.get(saveName);
                }
                
                // Find save file
                String filename = generateSaveFilename(saveName);
                Path savePath = saveDirectory.resolve(filename);
                
                if (!Files.exists(savePath)) {
                    throw new FileNotFoundException("Save file not found: " + saveName);
                }
                
                // Load save data
                SaveData saveData = readSaveData(savePath);
                
                // Validate save
                if (!validateSaveData(saveData)) {
                    throw new IOException("Save data validation failed: " + saveName);
                }
                
                // Update cache
                saveCache.put(saveName, saveData);
                
                LOGGER.info("Save loaded successfully: {}", saveName);
                return saveData;
                
            } catch (Exception e) {
                LOGGER.error("Failed to load game: {}", saveName, e);
                throw new RuntimeException("Failed to load save: " + saveName, e);
            }
        }, saveExecutor);
    }
    
    /**
     * Delete a save file.
     */
    public boolean deleteSave(String saveName) {
        try {
            String filename = generateSaveFilename(saveName);
            Path savePath = saveDirectory.resolve(filename);
            
            if (Files.exists(savePath)) {
                Files.delete(savePath);
                saveCache.remove(saveName);
                refreshSaveList();
                LOGGER.info("Save deleted: {}", saveName);
                return true;
            }
            
            return false;
        } catch (IOException e) {
            LOGGER.error("Failed to delete save: {}", saveName, e);
            return false;
        }
    }
    
    /**
     * Get list of available saves.
     */
    public List<SaveInfo> getAvailableSaves() {
        return new ArrayList<>(availableSaves);
    }
    
    /**
     * Refresh the list of available saves.
     */
    public void refreshSaveList() {
        availableSaves = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDirectory, "*" + SAVE_EXTENSION)) {
            for (Path savePath : stream) {
                try {
                    SaveInfo info = createSaveInfo(savePath);
                    if (info != null) {
                        availableSaves.add(info);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to read save info: {}", savePath, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to refresh save list", e);
        }
        
        // Sort by last modified (newest first)
        availableSaves.sort((a, b) -> b.getLastModified().compareTo(a.getLastModified()));
        
        LOGGER.debug("Found {} saves", availableSaves.size());
    }
    
    /**
     * Create SaveData from current game state.
     */
    private SaveData createSaveData(String saveName) {
        SaveData saveData = new SaveData(saveName, GAME_VERSION);
        
        if (gameEngine != null) {
            // Set game state
            saveData.setGameState(gameEngine.getGameState());
            
            // Create player data from PlayerManager
            PlayerManager playerManager = PlayerManager.getInstance();
            Player currentPlayer = playerManager.getCurrentPlayer();
            
            SaveData.PlayerSaveData playerData = new SaveData.PlayerSaveData();
            if (currentPlayer != null) {
                // Set actual player data
                playerData.setPlayerName(currentPlayer.getPlayerName());
                playerData.setPosition(currentPlayer.getPosition());
                playerData.setRotation(currentPlayer.getRotation());
                playerData.setVelocity(currentPlayer.getVelocity());
                playerData.setHealth(currentPlayer.getHealth());
                playerData.setMaxHealth(currentPlayer.getMaxHealth());
                playerData.setLevel(currentPlayer.getLevel());
                playerData.setExperience(currentPlayer.getExperience());
                playerData.setInventory(currentPlayer.getInventory());
                playerData.setPlayerStats(currentPlayer.getPlayerStats());
                playerData.setOnShip(currentPlayer.isOnShip());
                playerData.setShipId(currentPlayer.getShipId());
                playerData.setSpawnPoint(currentPlayer.getSpawnPoint());
                playerData.setGameTime(currentPlayer.getGameTime());
            } else {
                // Default player data if no current player
                playerData.setPlayerName("Player");
                playerData.setPosition(new org.joml.Vector3f(0, 64, 0));
                playerData.setRotation(new org.joml.Vector3f(0, 0, 0));
                playerData.setVelocity(new org.joml.Vector3f(0, 0, 0));
                playerData.setHealth(100.0f);
                playerData.setMaxHealth(100.0f);
                playerData.setLevel(1);
                playerData.setExperience(0);
                playerData.setInventory(new HashMap<>());
                playerData.setPlayerStats(new HashMap<>());
                playerData.setOnShip(false);
                playerData.setShipId(null);
                playerData.setSpawnPoint(new org.joml.Vector3f(0, 64, 0));
                playerData.setGameTime(0);
            }
            saveData.setPlayerData(playerData);
            
            // Create world data
            if (worldManager != null) {
                SaveData.WorldSaveData worldData = new SaveData.WorldSaveData();
                worldData.setWorldName("default");
                worldData.setWorldSeed(gameEngine.getWorldSeed()); // Get actual seed from GameEngine
                worldData.setGameTime(gameEngine.getGameTime()); // Get actual game time from GameEngine
                
                // Set world flags from game engine
                try {
                    Map<String, Object> worldFlags = gameEngine.getWorldFlags();
                    if (worldFlags != null) {
                        worldData.setWorldFlags(new HashMap<>(worldFlags));
                    } else {
                        worldData.setWorldFlags(new HashMap<>());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to get world flags, using empty map", e);
                    worldData.setWorldFlags(new HashMap<>());
                }
                
                // Set weather state
                try {
                    String weatherState = gameEngine.getWeatherState();
                    worldData.setWeatherState(weatherState != null ? weatherState : "clear");
                } catch (Exception e) {
                    LOGGER.warn("Failed to get weather state, using default", e);
                    worldData.setWeatherState("clear");
                }
                
                // Serialize and compress chunk data
                try {
                    byte[] chunkData = serializeChunkData(worldManager);
                    worldData.setChunkData(chunkData);
                } catch (Exception e) {
                    LOGGER.error("Failed to serialize chunk data", e);
                    worldData.setChunkData(new byte[0]);
                }
                
                saveData.setWorldData(worldData);
            }
            
            // Set ships data
            try {
                List<Ship> playerShips = gameEngine.getPlayerShips();
                if (playerShips != null) {
                    saveData.setPlayerShips(new ArrayList<>(playerShips));
                } else {
                    saveData.setPlayerShips(new ArrayList<>());
                }
                
                String activeShipId = gameEngine.getActiveShipId();
                saveData.setActiveShipId(activeShipId);
            } catch (Exception e) {
                LOGGER.warn("Failed to get ship data, using empty list", e);
                saveData.setPlayerShips(new ArrayList<>());
                saveData.setActiveShipId(null);
            }
            
            // Set game flags
            try {
                Map<String, Object> gameFlags = gameEngine.getGameFlags();
                if (gameFlags != null) {
                    saveData.setGameFlags(new HashMap<>(gameFlags));
                } else {
                    saveData.setGameFlags(new HashMap<>());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get game flags, using empty map", e);
                saveData.setGameFlags(new HashMap<>());
            }
            
            // Set quest progress
            try {
                Map<String, Integer> questProgress = gameEngine.getQuestProgress();
                if (questProgress != null) {
                    saveData.setQuestProgress(new HashMap<>(questProgress));
                } else {
                    saveData.setQuestProgress(new HashMap<>());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get quest progress, using empty map", e);
                saveData.setQuestProgress(new HashMap<>());
            }
            
            // Set achievements
            try {
                Map<String, Boolean> achievements = gameEngine.getAchievements();
                if (achievements != null) {
                    saveData.setAchievements(new HashMap<>(achievements));
                } else {
                    saveData.setAchievements(new HashMap<>());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get achievements, using empty map", e);
                saveData.setAchievements(new HashMap<>());
            }
            
            // Set statistics
            try {
                Map<String, Object> statistics = gameEngine.getStatistics();
                if (statistics != null) {
                    saveData.setStatistics(new HashMap<>(statistics));
                } else {
                    saveData.setStatistics(new HashMap<>());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get statistics, using empty map", e);
                saveData.setStatistics(new HashMap<>());
            }
            
            // Set playtime
            try {
                long playtime = gameEngine.getPlaytimeSeconds();
                saveData.setPlaytimeSeconds(playtime);
            } catch (Exception e) {
                LOGGER.warn("Failed to get playtime, using 0", e);
                saveData.setPlaytimeSeconds(0);
            }
        }
        
        saveData.updateLastModified();
        return saveData;
    }
    
    /**
     * Write save data to file with compression.
     */
    private void writeSaveData(SaveData saveData, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            oos.writeObject(saveData);
            oos.flush();
        }
    }
    
    /**
     * Read save data from file with decompression.
     */
    private SaveData readSaveData(Path filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            
            return (SaveData) ois.readObject();
        }
    }
    
    /**
     * Validate save data integrity.
     */
    private boolean validateSaveData(SaveData saveData) {
        if (saveData == null) return false;
        if (saveData.getSaveId() == null) return false;
        if (saveData.getSaveName() == null || saveData.getSaveName().trim().isEmpty()) return false;
        if (saveData.getGameVersion() == null) return false;
        
        // Check version compatibility
        if (!saveData.isCompatible(GAME_VERSION)) {
            LOGGER.warn("Save version mismatch: save={}, current={}", 
                       saveData.getGameVersion(), GAME_VERSION);
            // For now, allow loading different versions with a warning
        }
        
        return true;
    }
    
    /**
     * Create backup of existing save.
     */
    private void createBackup(Path savePath) throws IOException {
        String filename = savePath.getFileName().toString();
        String backupName = filename.replace(SAVE_EXTENSION, 
            "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + BACKUP_EXTENSION);
        
        Path backupPath = backupDirectory.resolve(backupName);
        Files.copy(savePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Clean up old backups
        cleanupOldBackups(filename);
    }
    
    /**
     * Clean up old backup files.
     */
    private void cleanupOldBackups(String originalFilename) throws IOException {
        String baseFilename = originalFilename.replace(SAVE_EXTENSION, "");
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDirectory, 
                baseFilename + "_*" + BACKUP_EXTENSION)) {
            
            List<Path> backups = new ArrayList<>();
            stream.forEach(backups::add);
            
            if (backups.size() > MAX_BACKUP_COUNT) {
                // Sort by creation time (oldest first)
                backups.sort((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                    } catch (IOException e) {
                        return 0;
                    }
                });
                
                // Delete oldest backups
                for (int i = 0; i < backups.size() - MAX_BACKUP_COUNT; i++) {
                    Files.deleteIfExists(backups.get(i));
                }
            }
        }
    }
    
    /**
     * Generate filename for save.
     */
    private String generateSaveFilename(String saveName) {
        // Sanitize save name for filename
        String sanitized = saveName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return sanitized + SAVE_EXTENSION;
    }
    
    /**
     * Create SaveInfo from save file.
     */
    private SaveInfo createSaveInfo(Path savePath) {
        try {
            SaveData saveData = readSaveData(savePath);
            return new SaveInfo(
                saveData.getSaveName(),
                saveData.getLastModified(),
                saveData.getFormattedPlaytime(),
                saveData.getGameVersion(),
                Files.size(savePath)
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to create save info for: {}", savePath, e);
            return null;
        }
    }
    
    /**
     * Serialize chunk data for saving.
     */
    private byte[] serializeChunkData(WorldManager worldManager) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            // Get loaded chunks from world manager
            Map<String, Object> chunkMap = new HashMap<>();
            
            if (worldManager.getCurrentWorld() != null) {
                // Get chunk data from the world
                // This is a placeholder - actual implementation would depend on World class structure
                Map<String, Object> loadedChunks = worldManager.getCurrentWorld().getLoadedChunks();
                if (loadedChunks != null) {
                    chunkMap.putAll(loadedChunks);
                }
            }
            
            // Write chunk count first
            oos.writeInt(chunkMap.size());
            
            // Write each chunk
            for (Map.Entry<String, Object> entry : chunkMap.entrySet()) {
                oos.writeUTF(entry.getKey()); // Chunk coordinates as string
                oos.writeObject(entry.getValue()); // Chunk data
            }
            
            oos.flush();
            gzos.finish();
            
            LOGGER.debug("Serialized {} chunks, compressed size: {} bytes", 
                        chunkMap.size(), baos.size());
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            LOGGER.error("Failed to serialize chunk data", e);
            throw new IOException("Chunk serialization failed", e);
        }
    }
    
    /**
     * Deserialize chunk data for loading.
     */
    private Map<String, Object> deserializeChunkData(byte[] chunkData) throws IOException, ClassNotFoundException {
        if (chunkData == null || chunkData.length == 0) {
            return new HashMap<>();
        }
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(chunkData);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            
            Map<String, Object> chunkMap = new HashMap<>();
            
            // Read chunk count
            int chunkCount = ois.readInt();
            
            // Read each chunk
            for (int i = 0; i < chunkCount; i++) {
                String chunkKey = ois.readUTF();
                Object chunkData_obj = ois.readObject();
                chunkMap.put(chunkKey, chunkData_obj);
            }
            
            LOGGER.debug("Deserialized {} chunks from {} bytes", 
                        chunkCount, chunkData.length);
            
            return chunkMap;
            
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize chunk data", e);
            throw new IOException("Chunk deserialization failed", e);
        }
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        saveExecutor.shutdown();
        saveCache.clear();
        LOGGER.info("SaveManager cleanup complete");
    }
    
    /**
     * Validate a save file by attempting to read and verify its contents.
     * 
     * @param saveName The name of the save to validate
     * @return true if the save file is valid, false otherwise
     */
    public boolean validateSaveFile(String saveName) {
        try {
            String filename = generateSaveFilename(saveName);
            Path savePath = saveDirectory.resolve(filename);
            
            // Check if file exists
            if (!Files.exists(savePath)) {
                LOGGER.warn("Save file does not exist: {}", savePath);
                return false;
            }
            
            // Check if file is readable
            if (!Files.isReadable(savePath)) {
                LOGGER.warn("Save file is not readable: {}", savePath);
                return false;
            }
            
            // Try to read and validate the save data
            SaveData saveData = readSaveData(savePath);
            return validateSaveData(saveData);
            
        } catch (Exception e) {
            LOGGER.warn("Save file validation failed for '{}': {}", saveName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the file path for a save by name.
     * 
     * @param saveName The name of the save
     * @return The full path to the save file
     */
    public Path getSaveFilePath(String saveName) {
        String filename = generateSaveFilename(saveName);
        return saveDirectory.resolve(filename);
    }

    /**
     * Information about a save file.
     */
    public static class SaveInfo {
        private final String name;
        private final LocalDateTime lastModified;
        private final String playtime;
        private final String gameVersion;
        private final long fileSize;
        private final long playtimeSeconds;
        
        public SaveInfo(String name, LocalDateTime lastModified, String playtime, 
                       String gameVersion, long fileSize) {
            this.name = name;
            this.lastModified = lastModified;
            this.playtime = playtime;
            this.gameVersion = gameVersion;
            this.fileSize = fileSize;
            // Parse playtime seconds from formatted string for compatibility
            this.playtimeSeconds = parsePlaytimeSeconds(playtime);
        }
        
        /**
         * Parse playtime seconds from formatted playtime string.
         */
        private long parsePlaytimeSeconds(String formattedPlaytime) {
            try {
                // Expected format: "Xh Ym" or similar
                if (formattedPlaytime != null && formattedPlaytime.contains("h") && formattedPlaytime.contains("m")) {
                    String[] parts = formattedPlaytime.split("h");
                    if (parts.length >= 2) {
                        long hours = Long.parseLong(parts[0].trim());
                        String minutePart = parts[1].trim().replace("m", "").trim();
                        long minutes = Long.parseLong(minutePart);
                        return hours * 3600 + minutes * 60;
                    }
                }
            } catch (Exception e) {
                // If parsing fails, return 0
            }
            return 0;
        }
        
        public String getName() { return name; }
        public LocalDateTime getLastModified() { return lastModified; }
        public String getPlaytime() { return playtime; }
        public String getGameVersion() { return gameVersion; }
        public long getFileSize() { return fileSize; }
        public long getPlaytimeSeconds() { return playtimeSeconds; }
        
        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
        
        @Override
        public String toString() {
            return String.format("SaveInfo{name='%s', modified=%s, playtime=%s, version=%s, size=%s}",
                               name, lastModified, playtime, gameVersion, getFormattedFileSize());
        }
    }
}