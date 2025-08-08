package com.odyssey.world.save.level;

import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Manages automatic backup system for world metadata with rollback capability.
 * Implements level.dat_old functionality similar to Minecraft.
 * 
 * <p>Features:
 * <ul>
 * <li>Automatic backup before each save</li>
 * <li>Rollback capability to previous version</li>
 * <li>Corruption detection and recovery</li>
 * <li>Timestamped backup history</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class LevelMetadataBackup {
    private static final Logger logger = LoggerFactory.getLogger(LevelMetadataBackup.class);
    
    private final Path worldDirectory;
    private final Path levelDataFile;
    private final Path levelDataOldFile;
    private final Path backupDirectory;
    
    /**
     * Creates a new level metadata backup manager.
     * 
     * @param worldDirectory the world directory
     */
    public LevelMetadataBackup(Path worldDirectory) {
        this.worldDirectory = worldDirectory;
        this.levelDataFile = worldDirectory.resolve(WorldSaveFormat.LEVEL_DATA_FILE);
        this.levelDataOldFile = worldDirectory.resolve(WorldSaveFormat.LEVEL_DATA_OLD_FILE);
        this.backupDirectory = worldDirectory.resolve("level_backups");
        
        try {
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            logger.error("Failed to create level backup directory", e);
        }
    }
    
    /**
     * Creates a backup of the current level data before saving.
     * 
     * @throws IOException if backup creation fails
     */
    public void createBackupBeforeSave() throws IOException {
        if (!Files.exists(levelDataFile)) {
            logger.debug("No existing level data to backup");
            return;
        }
        
        // Create level.dat_old backup
        Files.copy(levelDataFile, levelDataOldFile, StandardCopyOption.REPLACE_EXISTING);
        
        // Create timestamped backup
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(":", "-").replace(".", "-");
        Path timestampedBackup = backupDirectory.resolve("level-" + timestamp + ".json");
        Files.copy(levelDataFile, timestampedBackup, StandardCopyOption.REPLACE_EXISTING);
        
        logger.debug("Created level data backup: {}", timestampedBackup.getFileName());
        
        // Clean up old backups (keep last 10)
        cleanupOldBackups();
    }
    
    /**
     * Attempts to restore from backup if current level data is corrupted.
     * 
     * @return true if restoration was successful
     */
    public boolean restoreFromBackup() {
        try {
            // Try level.dat_old first
            if (Files.exists(levelDataOldFile)) {
                Files.copy(levelDataOldFile, levelDataFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Restored level data from level.dat_old");
                return true;
            }
            
            // Try most recent timestamped backup
            Path mostRecentBackup = findMostRecentBackup();
            if (mostRecentBackup != null) {
                Files.copy(mostRecentBackup, levelDataFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Restored level data from backup: {}", mostRecentBackup.getFileName());
                return true;
            }
            
            logger.warn("No backup files found for restoration");
            return false;
            
        } catch (IOException e) {
            logger.error("Failed to restore level data from backup", e);
            return false;
        }
    }
    
    /**
     * Validates the integrity of the current level data file.
     * 
     * @return true if the file is valid
     */
    public boolean validateLevelData() {
        try {
            if (!Files.exists(levelDataFile)) {
                return false;
            }
            
            // Basic validation - check if file is readable and not empty
            if (Files.size(levelDataFile) == 0) {
                logger.warn("Level data file is empty");
                return false;
            }
            
            // Try to read the file content
            String content = Files.readString(levelDataFile);
            if (content.trim().isEmpty()) {
                logger.warn("Level data file contains no content");
                return false;
            }
            
            // Basic JSON validation (starts with { and ends with })
            content = content.trim();
            if (!content.startsWith("{") || !content.endsWith("}")) {
                logger.warn("Level data file does not appear to be valid JSON");
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to validate level data file", e);
            return false;
        }
    }
    
    /**
     * Finds the most recent backup file.
     * 
     * @return the path to the most recent backup, or null if none found
     */
    private Path findMostRecentBackup() throws IOException {
        if (!Files.exists(backupDirectory)) {
            return null;
        }
        
        return Files.list(backupDirectory)
                .filter(path -> path.getFileName().toString().startsWith("level-"))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .max((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .orElse(null);
    }
    
    /**
     * Cleans up old backup files, keeping only the most recent ones.
     */
    private void cleanupOldBackups() {
        try {
            if (!Files.exists(backupDirectory)) {
                return;
            }
            
            Files.list(backupDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("level-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .skip(10) // Keep 10 most recent
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            logger.debug("Deleted old level backup: {}", path.getFileName());
                        } catch (IOException e) {
                            logger.warn("Failed to delete old backup: {}", path, e);
                        }
                    });
                    
        } catch (IOException e) {
            logger.error("Failed to cleanup old level backups", e);
        }
    }
    
    /**
     * Gets the path to the level data file.
     * 
     * @return the level data file path
     */
    public Path getLevelDataFile() {
        return levelDataFile;
    }
    
    /**
     * Gets the path to the level.dat_old file.
     * 
     * @return the level.dat_old file path
     */
    public Path getLevelDataOldFile() {
        return levelDataOldFile;
    }
    
    /**
     * Gets the backup directory path.
     * 
     * @return the backup directory path
     */
    public Path getBackupDirectory() {
        return backupDirectory;
    }
}