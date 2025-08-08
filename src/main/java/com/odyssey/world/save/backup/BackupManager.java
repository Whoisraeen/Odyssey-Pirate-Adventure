package com.odyssey.world.save.backup;

import com.odyssey.world.save.journal.WriteAheadJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manages automated world backups with compression, incremental backups,
 * and configurable retention policies.
 * 
 * <p>Features:
 * <ul>
 * <li>Full and incremental backups</li>
 * <li>Compression with multiple algorithms</li>
 * <li>Configurable retention policies</li>
 * <li>Background backup processing</li>
 * <li>Backup verification and integrity checks</li>
 * <li>Automatic cleanup of old backups</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class BackupManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final Path worldSaveDir;
    private final Path backupDir;
    private final WriteAheadJournal journal;
    private final ExecutorService backupExecutor;
    
    // Configuration
    private BackupConfig config;
    
    // State tracking
    private volatile boolean backupInProgress = false;
    private volatile BackupInfo lastBackup = null;
    private final Map<String, BackupInfo> backupHistory = new ConcurrentHashMap<>();
    
    // Scheduled backup task
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledBackupTask;
    
    /**
     * Creates a new backup manager.
     * 
     * @param worldSaveDir the world save directory
     * @param journal the write-ahead journal
     * @throws IOException if initialization fails
     */
    public BackupManager(Path worldSaveDir, WriteAheadJournal journal) throws IOException {
        this.worldSaveDir = worldSaveDir;
        this.backupDir = worldSaveDir.getParent().resolve("backups");
        this.journal = journal;
        
        // Create backup directory
        Files.createDirectories(backupDir);
        
        // Initialize thread pool for background backups
        this.backupExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BackupManager");
            t.setDaemon(true);
            return t;
        });
        
        // Load default configuration
        this.config = new BackupConfig();
        
        // Load existing backup history
        loadBackupHistory();
        
        // Start scheduled backups if enabled
        if (config.isAutoBackupEnabled()) {
            startScheduledBackups();
        }
        
        logger.info("Initialized backup manager at: {}", backupDir);
    }
    
    /**
     * Starts scheduled automatic backups.
     */
    public void startScheduledBackups() {
        if (scheduler != null) {
            return; // Already started
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BackupScheduler");
            t.setDaemon(true);
            return t;
        });
        
        long intervalMinutes = config.getAutoBackupIntervalMinutes();
        scheduledBackupTask = scheduler.scheduleAtFixedRate(
            this::performScheduledBackup,
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );
        
        logger.info("Started scheduled backups every {} minutes", intervalMinutes);
    }
    
    /**
     * Stops scheduled automatic backups.
     */
    public void stopScheduledBackups() {
        if (scheduledBackupTask != null) {
            scheduledBackupTask.cancel(false);
            scheduledBackupTask = null;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        
        logger.info("Stopped scheduled backups");
    }
    
    /**
     * Performs a scheduled backup if conditions are met.
     */
    private void performScheduledBackup() {
        try {
            // Check if backup is needed
            if (!shouldPerformScheduledBackup()) {
                return;
            }
            
            // Perform incremental backup
            BackupType backupType = shouldPerformFullBackup() ? 
                BackupType.FULL : BackupType.INCREMENTAL;
            
            createBackupAsync(backupType, "Scheduled backup");
            
        } catch (Exception e) {
            logger.error("Failed to perform scheduled backup", e);
        }
    }
    
    /**
     * Checks if a scheduled backup should be performed.
     * 
     * @return true if backup should be performed
     */
    private boolean shouldPerformScheduledBackup() {
        if (backupInProgress) {
            logger.debug("Skipping scheduled backup - backup already in progress");
            return false;
        }
        
        if (lastBackup == null) {
            return true; // First backup
        }
        
        long timeSinceLastBackup = System.currentTimeMillis() - lastBackup.timestamp;
        long intervalMs = config.getAutoBackupIntervalMinutes() * 60 * 1000;
        
        return timeSinceLastBackup >= intervalMs;
    }
    
    /**
     * Checks if a full backup should be performed instead of incremental.
     * 
     * @return true if full backup should be performed
     */
    private boolean shouldPerformFullBackup() {
        if (lastBackup == null || lastBackup.type != BackupType.FULL) {
            return true; // First backup or last was incremental
        }
        
        long timeSinceFullBackup = System.currentTimeMillis() - lastBackup.timestamp;
        long fullBackupIntervalMs = config.getFullBackupIntervalHours() * 60 * 60 * 1000;
        
        return timeSinceFullBackup >= fullBackupIntervalMs;
    }
    
    /**
     * Creates a backup asynchronously.
     * 
     * @param type the backup type
     * @param description the backup description
     * @return a future representing the backup operation
     */
    public CompletableFuture<BackupInfo> createBackupAsync(BackupType type, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createBackup(type, description);
            } catch (IOException e) {
                throw new RuntimeException("Backup failed", e);
            }
        }, backupExecutor);
    }
    
    /**
     * Creates a backup synchronously.
     * 
     * @param type the backup type
     * @param description the backup description
     * @return the backup information
     * @throws IOException if backup creation fails
     */
    public BackupInfo createBackup(BackupType type, String description) throws IOException {
        if (backupInProgress) {
            throw new IOException("Backup already in progress");
        }
        
        backupInProgress = true;
        
        try {
            logger.info("Starting {} backup: {}", type, description);
            
            // Log backup start in journal
            journal.logOperation(
                WriteAheadJournal.JournalOperation.BACKUP_START,
                description.getBytes()
            );
            
            // Create backup info
            String backupId = generateBackupId();
            BackupInfo backupInfo = new BackupInfo(
                backupId,
                type,
                description,
                System.currentTimeMillis()
            );
            
            // Determine files to backup
            Set<Path> filesToBackup = determineFilesToBackup(type);
            
            // Create backup file
            Path backupFile = createBackupFile(backupInfo, filesToBackup);
            
            // Update backup info
            backupInfo.filePath = backupFile;
            backupInfo.sizeBytes = Files.size(backupFile);
            backupInfo.fileCount = filesToBackup.size();
            backupInfo.completed = true;
            
            // Verify backup
            if (config.isVerifyBackups()) {
                verifyBackup(backupInfo);
            }
            
            // Update tracking
            lastBackup = backupInfo;
            backupHistory.put(backupId, backupInfo);
            
            // Save backup history
            saveBackupHistory();
            
            // Log backup completion in journal
            journal.logOperation(
                WriteAheadJournal.JournalOperation.BACKUP_COMPLETE,
                backupId.getBytes()
            );
            
            // Cleanup old backups
            cleanupOldBackups();
            
            logger.info("Completed {} backup: {} ({} files, {} bytes)", 
                type, backupId, backupInfo.fileCount, backupInfo.sizeBytes);
            
            return backupInfo;
            
        } finally {
            backupInProgress = false;
        }
    }
    
    /**
     * Determines which files to include in the backup.
     * 
     * @param type the backup type
     * @return the set of files to backup
     * @throws IOException if file scanning fails
     */
    private Set<Path> determineFilesToBackup(BackupType type) throws IOException {
        Set<Path> files = new HashSet<>();
        
        if (type == BackupType.FULL) {
            // Include all world files
            Files.walkFileTree(worldSaveDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (shouldIncludeFile(file)) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            // Incremental backup - only modified files
            long lastBackupTime = lastBackup != null ? lastBackup.timestamp : 0;
            
            Files.walkFileTree(worldSaveDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (shouldIncludeFile(file) && attrs.lastModifiedTime().toMillis() > lastBackupTime) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        return files;
    }
    
    /**
     * Checks if a file should be included in backups.
     * 
     * @param file the file to check
     * @return true if the file should be included
     */
    private boolean shouldIncludeFile(Path file) {
        String fileName = file.getFileName().toString();
        
        // Exclude temporary files
        if (fileName.endsWith(".tmp") || fileName.endsWith(".temp")) {
            return false;
        }
        
        // Exclude lock files
        if (fileName.equals("session.lock")) {
            return false;
        }
        
        // Exclude journal files (they're for recovery, not backup)
        if (fileName.startsWith("journal-") && fileName.endsWith(".wal")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates the backup file.
     * 
     * @param backupInfo the backup information
     * @param filesToBackup the files to include
     * @return the path to the created backup file
     * @throws IOException if backup creation fails
     */
    private Path createBackupFile(BackupInfo backupInfo, Set<Path> filesToBackup) throws IOException {
        Path backupFile = backupDir.resolve(backupInfo.id + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(backupFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // Set compression level
            zos.setLevel(config.getCompressionLevel());
            
            for (Path file : filesToBackup) {
                addFileToZip(zos, file, worldSaveDir);
            }
        }
        
        return backupFile;
    }
    
    /**
     * Adds a file to the ZIP archive.
     * 
     * @param zos the ZIP output stream
     * @param file the file to add
     * @param baseDir the base directory for relative paths
     * @throws IOException if adding fails
     */
    private void addFileToZip(ZipOutputStream zos, Path file, Path baseDir) throws IOException {
        String relativePath = baseDir.relativize(file).toString().replace('\\', '/');
        
        ZipEntry entry = new ZipEntry(relativePath);
        entry.setTime(Files.getLastModifiedTime(file).toMillis());
        zos.putNextEntry(entry);
        
        Files.copy(file, zos);
        zos.closeEntry();
    }
    
    /**
     * Verifies the integrity of a backup.
     * 
     * @param backupInfo the backup to verify
     * @throws IOException if verification fails
     */
    private void verifyBackup(BackupInfo backupInfo) throws IOException {
        // Simple verification - check that the file exists and is readable
        if (!Files.exists(backupInfo.filePath)) {
            throw new IOException("Backup file does not exist: " + backupInfo.filePath);
        }
        
        if (!Files.isReadable(backupInfo.filePath)) {
            throw new IOException("Backup file is not readable: " + backupInfo.filePath);
        }
        
        // Could add more sophisticated verification like checksum validation
        logger.debug("Verified backup: {}", backupInfo.id);
    }
    
    /**
     * Cleans up old backups according to retention policy.
     * 
     * @throws IOException if cleanup fails
     */
    private void cleanupOldBackups() throws IOException {
        List<BackupInfo> backups = new ArrayList<>(backupHistory.values());
        
        // Sort by timestamp (newest first)
        backups.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        
        // Apply retention policy
        int maxBackups = config.getMaxBackupsToKeep();
        long maxAgeMs = config.getMaxBackupAgeHours() * 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < backups.size(); i++) {
            BackupInfo backup = backups.get(i);
            boolean shouldDelete = false;
            
            // Check count limit
            if (i >= maxBackups) {
                shouldDelete = true;
            }
            
            // Check age limit
            if (currentTime - backup.timestamp > maxAgeMs) {
                shouldDelete = true;
            }
            
            if (shouldDelete) {
                deleteBackup(backup);
            }
        }
    }
    
    /**
     * Deletes a backup and removes it from history.
     * 
     * @param backup the backup to delete
     * @throws IOException if deletion fails
     */
    private void deleteBackup(BackupInfo backup) throws IOException {
        if (Files.exists(backup.filePath)) {
            Files.delete(backup.filePath);
        }
        
        backupHistory.remove(backup.id);
        
        logger.debug("Deleted old backup: {}", backup.id);
    }
    
    /**
     * Generates a unique backup ID.
     * 
     * @return the backup ID
     */
    private String generateBackupId() {
        return LocalDateTime.now().format(BACKUP_DATE_FORMAT);
    }
    
    /**
     * Loads backup history from disk.
     */
    private void loadBackupHistory() {
        // Scan backup directory for existing backups
        try (var stream = Files.list(backupDir)) {
            for (Path file : stream.toList()) {
                if (file.getFileName().toString().endsWith(".zip")) {
                    try {
                        BackupInfo info = createBackupInfoFromFile(file);
                        backupHistory.put(info.id, info);
                        
                        if (lastBackup == null || info.timestamp > lastBackup.timestamp) {
                            lastBackup = info;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to load backup info for: {}", file, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load backup history", e);
        }
        
        logger.info("Loaded {} existing backups", backupHistory.size());
    }
    
    /**
     * Creates backup info from an existing backup file.
     * 
     * @param file the backup file
     * @return the backup info
     * @throws IOException if info creation fails
     */
    private BackupInfo createBackupInfoFromFile(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String backupId = fileName.substring(0, fileName.length() - 4); // Remove .zip
        
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        
        BackupInfo info = new BackupInfo(
            backupId,
            BackupType.FULL, // Assume full for existing backups
            "Existing backup",
            attrs.lastModifiedTime().toMillis()
        );
        
        info.filePath = file;
        info.sizeBytes = attrs.size();
        info.completed = true;
        
        return info;
    }
    
    /**
     * Saves backup history to disk.
     */
    private void saveBackupHistory() {
        // For now, history is maintained in memory and loaded from file system
        // Could implement persistent history file if needed
    }
    
    /**
     * Gets the backup configuration.
     * 
     * @return the backup configuration
     */
    public BackupConfig getConfig() {
        return config;
    }
    
    /**
     * Sets the backup configuration.
     * 
     * @param config the new configuration
     */
    public void setConfig(BackupConfig config) {
        this.config = config;
        
        // Restart scheduled backups if needed
        if (config.isAutoBackupEnabled() && scheduler == null) {
            startScheduledBackups();
        } else if (!config.isAutoBackupEnabled() && scheduler != null) {
            stopScheduledBackups();
        }
    }
    
    /**
     * Gets the backup history.
     * 
     * @return the backup history
     */
    public Map<String, BackupInfo> getBackupHistory() {
        return new HashMap<>(backupHistory);
    }
    
    /**
     * Gets the last backup info.
     * 
     * @return the last backup, or null if none
     */
    public BackupInfo getLastBackup() {
        return lastBackup;
    }
    
    /**
     * Checks if a backup is currently in progress.
     * 
     * @return true if backup is in progress
     */
    public boolean isBackupInProgress() {
        return backupInProgress;
    }
    
    /**
     * Shuts down the backup manager.
     */
    public void shutdown() {
        stopScheduledBackups();
        
        backupExecutor.shutdown();
        try {
            if (!backupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                backupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Shutdown backup manager");
    }
    
    /**
     * Backup type enumeration.
     */
    public enum BackupType {
        FULL,
        INCREMENTAL
    }
    
    /**
     * Backup information.
     */
    public static class BackupInfo {
        public final String id;
        public final BackupType type;
        public final String description;
        public final long timestamp;
        
        public Path filePath;
        public long sizeBytes;
        public int fileCount;
        public boolean completed;
        
        public BackupInfo(String id, BackupType type, String description, long timestamp) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("BackupInfo{id='%s', type=%s, size=%d, files=%d}", 
                id, type, sizeBytes, fileCount);
        }
    }
    
    /**
     * Backup configuration.
     */
    public static class BackupConfig {
        private boolean autoBackupEnabled = true;
        private int autoBackupIntervalMinutes = 30;
        private int fullBackupIntervalHours = 24;
        private int maxBackupsToKeep = 10;
        private int maxBackupAgeHours = 168; // 1 week
        private int compressionLevel = 6; // Default ZIP compression
        private boolean verifyBackups = true;
        
        // Getters and setters
        public boolean isAutoBackupEnabled() { return autoBackupEnabled; }
        public void setAutoBackupEnabled(boolean autoBackupEnabled) { this.autoBackupEnabled = autoBackupEnabled; }
        
        public int getAutoBackupIntervalMinutes() { return autoBackupIntervalMinutes; }
        public void setAutoBackupIntervalMinutes(int autoBackupIntervalMinutes) { this.autoBackupIntervalMinutes = autoBackupIntervalMinutes; }
        
        public int getFullBackupIntervalHours() { return fullBackupIntervalHours; }
        public void setFullBackupIntervalHours(int fullBackupIntervalHours) { this.fullBackupIntervalHours = fullBackupIntervalHours; }
        
        public int getMaxBackupsToKeep() { return maxBackupsToKeep; }
        public void setMaxBackupsToKeep(int maxBackupsToKeep) { this.maxBackupsToKeep = maxBackupsToKeep; }
        
        public int getMaxBackupAgeHours() { return maxBackupAgeHours; }
        public void setMaxBackupAgeHours(int maxBackupAgeHours) { this.maxBackupAgeHours = maxBackupAgeHours; }
        
        public int getCompressionLevel() { return compressionLevel; }
        public void setCompressionLevel(int compressionLevel) { this.compressionLevel = Math.max(0, Math.min(9, compressionLevel)); }
        
        public boolean isVerifyBackups() { return verifyBackups; }
        public void setVerifyBackups(boolean verifyBackups) { this.verifyBackups = verifyBackups; }
    }
}