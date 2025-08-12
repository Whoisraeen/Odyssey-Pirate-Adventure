package com.odyssey.world.save.advancements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.odyssey.world.save.advancements.Advancement;
import com.odyssey.world.save.advancements.AdvancementProgress;
import com.odyssey.world.save.advancements.AdvancementReward;
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
 * Manages per-player advancement storage in the advancements/ folder.
 * Handles advancement progress, completion tracking, and rewards for maritime gameplay.
 * 
 * <p>Features:
 * <ul>
 * <li>Per-player advancement files in JSON format</li>
 * <li>Maritime-specific advancement trees</li>
 * <li>Progress tracking and completion detection</li>
 * <li>Thread-safe operations</li>
 * <li>Reward management</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerAdvancementManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerAdvancementManager.class);
    
    private static final String ADVANCEMENT_FILE_EXTENSION = ".json";
    
    private final Path advancementsDirectory;
    private final Gson gson;
    private final Map<UUID, PlayerAdvancements> loadedAdvancements;
    private final ReadWriteLock advancementLock;
    private final AdvancementRegistry advancementRegistry;
    
    /**
     * Creates a new player advancement manager.
     * 
     * @param worldDirectory the world directory
     */
    public PlayerAdvancementManager(Path worldDirectory) {
        this.advancementsDirectory = worldDirectory.resolve(WorldSaveFormat.ADVANCEMENTS_DIR);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        this.loadedAdvancements = new ConcurrentHashMap<>();
        this.advancementLock = new ReentrantReadWriteLock();
        this.advancementRegistry = new AdvancementRegistry();
        
        initializeDirectory();
        initializeAdvancements();
    }
    
    /**
     * Initializes the advancements directory structure.
     */
    private void initializeDirectory() {
        try {
            Files.createDirectories(advancementsDirectory);
            logger.debug("Initialized advancements directory: {}", advancementsDirectory);
        } catch (IOException e) {
            logger.error("Failed to create advancements directory", e);
        }
    }
    
    /**
     * Initializes the advancement registry with maritime advancements.
     */
    private void initializeAdvancements() {
        advancementRegistry.registerDefaultAdvancements();
        logger.info("Registered {} maritime advancements", advancementRegistry.getAdvancementCount());
    }
    
    /**
     * Gets player advancements, loading from disk if necessary.
     * 
     * @param playerId the player UUID
     * @return the player advancements
     */
    public PlayerAdvancements getPlayerAdvancements(UUID playerId) {
        advancementLock.readLock().lock();
        try {
            return loadedAdvancements.computeIfAbsent(playerId, this::loadPlayerAdvancements);
        } finally {
            advancementLock.readLock().unlock();
        }
    }
    
    /**
     * Updates advancement progress for a player.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     * @param criteriaId the criteria identifier
     * @param progress the progress value
     */
    public void updateProgress(UUID playerId, String advancementId, String criteriaId, int progress) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        boolean wasCompleted = advancements.isCompleted(advancementId);
        
        advancements.updateProgress(advancementId, criteriaId, progress);
        
        // Check if advancement was just completed
        if (!wasCompleted && advancements.isCompleted(advancementId)) {
            onAdvancementCompleted(playerId, advancementId);
        }
        
        savePlayerAdvancements(playerId, advancements);
    }
    
    /**
     * Grants an advancement to a player.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     */
    public void grantAdvancement(UUID playerId, String advancementId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        
        if (!advancements.isCompleted(advancementId)) {
            advancements.completeAdvancement(advancementId);
            onAdvancementCompleted(playerId, advancementId);
            savePlayerAdvancements(playerId, advancements);
        }
    }
    
    /**
     * Revokes an advancement from a player.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     */
    public void revokeAdvancement(UUID playerId, String advancementId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        advancements.revokeAdvancement(advancementId);
        savePlayerAdvancements(playerId, advancements);
    }
    
    /**
     * Checks if a player has completed an advancement.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     * @return true if the advancement is completed
     */
    public boolean hasAdvancement(UUID playerId, String advancementId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        return advancements.isCompleted(advancementId);
    }
    
    /**
     * Gets the progress of an advancement for a player.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     * @return the advancement progress
     */
    public AdvancementProgress getProgress(UUID playerId, String advancementId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        return advancements.getProgress(advancementId);
    }
    
    /**
     * Gets all completed advancements for a player.
     * 
     * @param playerId the player UUID
     * @return map of completed advancement IDs to completion times
     */
    public Map<String, Long> getCompletedAdvancements(UUID playerId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        return advancements.getCompletedAdvancements();
    }
    
    /**
     * Gets the advancement completion percentage for a player.
     * 
     * @param playerId the player UUID
     * @return the completion percentage (0.0 to 1.0)
     */
    public double getCompletionPercentage(UUID playerId) {
        PlayerAdvancements advancements = getPlayerAdvancements(playerId);
        int totalAdvancements = advancementRegistry.getAdvancementCount();
        int completedAdvancements = advancements.getCompletedCount();
        
        return totalAdvancements > 0 ? (double) completedAdvancements / totalAdvancements : 0.0;
    }
    
    /**
     * Gets available advancements for a player (those that can be worked on).
     * 
     * @param playerId the player UUID
     * @return map of available advancement IDs to their definitions
     */
    public Map<String, Advancement> getAvailableAdvancements(UUID playerId) {
        PlayerAdvancements playerAdvancements = getPlayerAdvancements(playerId);
        return advancementRegistry.getAvailableAdvancements(playerAdvancements);
    }
    
    /**
     * Called when an advancement is completed.
     * 
     * @param playerId the player UUID
     * @param advancementId the advancement identifier
     */
    private void onAdvancementCompleted(UUID playerId, String advancementId) {
        Advancement advancement = advancementRegistry.getAdvancement(advancementId);
        if (advancement != null) {
            logger.info("Player {} completed advancement: {}", playerId, advancement.getTitle());
            
            // Grant rewards
            grantRewards(playerId, advancement);
            
            // Check for newly available advancements
            checkUnlockedAdvancements(playerId);
        }
    }
    
    /**
     * Grants rewards for completing an advancement.
     * 
     * @param playerId the player UUID
     * @param advancement the completed advancement
     */
    private void grantRewards(UUID playerId, Advancement advancement) {
        for (AdvancementReward reward : advancement.getRewards()) {
            switch (reward.getType()) {
                case EXPERIENCE:
                    // Grant experience points
                    logger.debug("Granting {} experience to player {}", reward.getValue(), playerId);
                    break;
                case GOLD:
                    // Grant gold
                    logger.debug("Granting {} gold to player {}", reward.getValue(), playerId);
                    break;
                case ITEM:
                    // Grant items
                    logger.debug("Granting item {} to player {}", reward.getItemId(), playerId);
                    break;
                case TITLE:
                    // Grant title
                    logger.debug("Granting title '{}' to player {}", reward.getTitle(), playerId);
                    break;
            }
        }
    }
    
    /**
     * Checks for newly unlocked advancements after completing one.
     * 
     * @param playerId the player UUID
     */
    private void checkUnlockedAdvancements(UUID playerId) {
        PlayerAdvancements playerAdvancements = getPlayerAdvancements(playerId);
        Map<String, Advancement> newlyAvailable = advancementRegistry.getNewlyAvailableAdvancements(playerAdvancements);
        
        if (!newlyAvailable.isEmpty()) {
            logger.debug("Player {} unlocked {} new advancements", playerId, newlyAvailable.size());
        }
    }
    
    /**
     * Saves player advancements to disk.
     * 
     * @param playerId the player UUID
     * @param advancements the player advancements
     */
    public void savePlayerAdvancements(UUID playerId, PlayerAdvancements advancements) {
        Path advancementFile = getAdvancementFile(playerId);
        
        try {
            String json = gson.toJson(advancements);
            Files.writeString(advancementFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Saved advancements for player: {}", playerId);
        } catch (IOException e) {
            logger.error("Failed to save advancements for player: {}", playerId, e);
        }
    }
    
    /**
     * Loads player advancements from disk.
     * 
     * @param playerId the player UUID
     * @return the loaded player advancements
     */
    private PlayerAdvancements loadPlayerAdvancements(UUID playerId) {
        Path advancementFile = getAdvancementFile(playerId);
        
        if (!Files.exists(advancementFile)) {
            return new PlayerAdvancements(playerId);
        }
        
        try {
            String json = Files.readString(advancementFile);
            PlayerAdvancements advancements = gson.fromJson(json, PlayerAdvancements.class);
            logger.debug("Loaded advancements for player: {}", playerId);
            return advancements != null ? advancements : new PlayerAdvancements(playerId);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load advancements for player: {}", playerId, e);
            return new PlayerAdvancements(playerId);
        }
    }
    
    /**
     * Unloads player advancements from memory.
     * 
     * @param playerId the player UUID
     */
    public void unloadPlayerAdvancements(UUID playerId) {
        advancementLock.writeLock().lock();
        try {
            PlayerAdvancements advancements = loadedAdvancements.remove(playerId);
            if (advancements != null) {
                savePlayerAdvancements(playerId, advancements);
                logger.debug("Unloaded advancements for player: {}", playerId);
            }
        } finally {
            advancementLock.writeLock().unlock();
        }
    }
    
    /**
     * Saves all loaded player advancements.
     */
    public void saveAllAdvancements() {
        advancementLock.readLock().lock();
        try {
            for (Map.Entry<UUID, PlayerAdvancements> entry : loadedAdvancements.entrySet()) {
                savePlayerAdvancements(entry.getKey(), entry.getValue());
            }
            logger.info("Saved advancements for {} players", loadedAdvancements.size());
        } finally {
            advancementLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the advancement file path for a player.
     * 
     * @param playerId the player UUID
     * @return the advancement file path
     */
    private Path getAdvancementFile(UUID playerId) {
        return advancementsDirectory.resolve(playerId.toString() + ADVANCEMENT_FILE_EXTENSION);
    }
    
    /**
     * Gets the advancement registry.
     * 
     * @return the advancement registry
     */
    public AdvancementRegistry getAdvancementRegistry() {
        return advancementRegistry;
    }
    
    /**
     * Cleans up old or corrupted advancement files.
     */
    public void cleanup() {
        try {
            Files.walk(advancementsDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(ADVANCEMENT_FILE_EXTENSION))
                .forEach(this::cleanupAdvancementFile);
        } catch (IOException e) {
            logger.error("Failed to cleanup advancement files", e);
        }
    }
    
    /**
     * Cleans up a specific advancement file.
     * 
     * @param advancementFile the advancement file to clean up
     */
    private void cleanupAdvancementFile(Path advancementFile) {
        try {
            String json = Files.readString(advancementFile);
            PlayerAdvancements advancements = gson.fromJson(json, PlayerAdvancements.class);
            
            if (advancements == null || advancements.isEmpty()) {
                Files.deleteIfExists(advancementFile);
                logger.debug("Deleted empty advancement file: {}", advancementFile.getFileName());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.warn("Failed to cleanup advancement file: {}", advancementFile.getFileName(), e);
        }
    }
    
    /**
     * Closes the advancement manager and saves all data.
     */
    public void close() {
        saveAllAdvancements();
        loadedAdvancements.clear();
        logger.info("Player advancement manager closed");
    }
}