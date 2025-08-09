package com.odyssey.world.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Handles cloud synchronization with zipped snapshots.
 * Provides hooks for various cloud storage providers and manages sync state.
 */
public class CloudSyncHooks {
    private static final Logger logger = LoggerFactory.getLogger(CloudSyncHooks.class);
    
    private final Map<String, CloudProvider> providers;
    private final ScheduledExecutorService scheduler;
    private final SyncStateManager stateManager;
    private final Path localSyncPath;
    
    public CloudSyncHooks(Path localSyncPath) {
        this.localSyncPath = localSyncPath;
        this.providers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.stateManager = new SyncStateManager(localSyncPath.resolve("sync_state.json"));
        
        // Ensure sync directory exists
        try {
            Files.createDirectories(localSyncPath);
        } catch (IOException e) {
            logger.error("Failed to create sync directory", e);
        }
    }
    
    /**
     * Registers a cloud provider for synchronization.
     */
    public void registerProvider(String name, CloudProvider provider) {
        providers.put(name, provider);
        logger.info("Registered cloud provider: {}", name);
    }
    
    /**
     * Starts automatic synchronization with the specified interval.
     */
    public void startAutoSync(String providerName, Duration interval) {
        CloudProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        
        scheduler.scheduleAtFixedRate(
            () -> performSync(providerName),
            0,
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
        
        logger.info("Started auto-sync for provider {} with interval {}", providerName, interval);
    }
    
    /**
     * Performs a manual synchronization with a specific provider.
     */
    public CompletableFuture<SyncResult> performSync(String providerName) {
        return CompletableFuture.supplyAsync(() -> {
            CloudProvider provider = providers.get(providerName);
            if (provider == null) {
                return new SyncResult(false, "Unknown provider: " + providerName, 0, 0);
            }
            
            try {
                logger.info("Starting sync with provider: {}", providerName);
                
                SyncProgress progress = new SyncProgress();
                SyncState state = stateManager.loadState(providerName);
                
                // Get remote snapshots
                List<CloudSnapshot> remoteSnapshots = provider.listSnapshots();
                progress.setTotalRemoteSnapshots(remoteSnapshots.size());
                
                // Get local snapshots
                List<LocalSnapshot> localSnapshots = scanLocalSnapshots();
                progress.setTotalLocalSnapshots(localSnapshots.size());
                
                int uploaded = 0;
                int downloaded = 0;
                
                // Upload new local snapshots
                for (LocalSnapshot local : localSnapshots) {
                    if (!hasRemoteSnapshot(remoteSnapshots, local)) {
                        provider.uploadSnapshot(local.path(), local.name());
                        uploaded++;
                        progress.incrementUploaded();
                        
                        // Update state
                        state.addUploadedSnapshot(local.name(), local.timestamp());
                    }
                }
                
                // Download new remote snapshots
                for (CloudSnapshot remote : remoteSnapshots) {
                    if (!hasLocalSnapshot(localSnapshots, remote)) {
                        Path downloadPath = localSyncPath.resolve("downloads").resolve(remote.name());
                        Files.createDirectories(downloadPath.getParent());
                        
                        provider.downloadSnapshot(remote.name(), downloadPath);
                        downloaded++;
                        progress.incrementDownloaded();
                        
                        // Update state
                        state.addDownloadedSnapshot(remote.name(), remote.timestamp());
                    }
                }
                
                // Save updated state
                stateManager.saveState(providerName, state);
                
                logger.info("Sync completed. Uploaded: {}, Downloaded: {}", uploaded, downloaded);
                return new SyncResult(true, "Sync completed successfully", uploaded, downloaded);
                
            } catch (Exception e) {
                logger.error("Sync failed for provider: " + providerName, e);
                return new SyncResult(false, "Sync failed: " + e.getMessage(), 0, 0);
            }
        });
    }
    
    /**
     * Creates a snapshot of a world and prepares it for upload.
     */
    public CompletableFuture<SnapshotResult> createSnapshot(Path worldPath, String snapshotName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (snapshotName == null || snapshotName.isEmpty()) {
                    snapshotName = generateSnapshotName(worldPath);
                }
                
                Path snapshotPath = localSyncPath.resolve("snapshots").resolve(snapshotName + ".zip");
                Files.createDirectories(snapshotPath.getParent());
                
                logger.info("Creating snapshot: {} from world: {}", snapshotName, worldPath);
                
                SnapshotProgress progress = new SnapshotProgress();
                long totalSize = calculateWorldSize(worldPath, progress);
                progress.setTotalBytes(totalSize);
                
                try (FileOutputStream fos = new FileOutputStream(snapshotPath.toFile());
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    
                    zos.setLevel(6); // Balanced compression
                    
                    // Add snapshot metadata
                    addSnapshotMetadata(zos, worldPath, snapshotName);
                    
                    // Add world files
                    addWorldFilesToSnapshot(zos, worldPath, worldPath, progress);
                    
                    zos.finish();
                }
                
                long snapshotSize = Files.size(snapshotPath);
                double compressionRatio = (double) snapshotSize / totalSize;
                
                logger.info("Snapshot created: {} ({}% of original size)", 
                           snapshotName, String.format("%.1f", compressionRatio * 100));
                
                return new SnapshotResult(true, "Snapshot created successfully", 
                                        snapshotPath, snapshotSize, compressionRatio);
                
            } catch (Exception e) {
                logger.error("Failed to create snapshot", e);
                return new SnapshotResult(false, "Failed to create snapshot: " + e.getMessage(), 
                                        null, 0, 0);
            }
        });
    }
    
    /**
     * Restores a world from a snapshot.
     */
    public CompletableFuture<RestoreResult> restoreFromSnapshot(Path snapshotPath, Path targetWorldPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Restoring world from snapshot: {} to: {}", snapshotPath, targetWorldPath);
                
                // Create backup of existing world if it exists
                Path backupPath = null;
                if (Files.exists(targetWorldPath)) {
                    backupPath = createBackup(targetWorldPath);
                }
                
                try {
                    RestoreProgress progress = new RestoreProgress();
                    
                    // Clear target directory
                    if (Files.exists(targetWorldPath)) {
                        deleteDirectory(targetWorldPath);
                    }
                    Files.createDirectories(targetWorldPath);
                    
                    // Extract snapshot
                    try (FileInputStream fis = new FileInputStream(snapshotPath.toFile());
                         ZipInputStream zis = new ZipInputStream(fis)) {
                        
                        ZipEntry entry;
                        byte[] buffer = new byte[8192];
                        
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.getName().equals("snapshot_metadata.json")) {
                                // Skip metadata file
                                zis.closeEntry();
                                continue;
                            }
                            
                            Path filePath = targetWorldPath.resolve(entry.getName());
                            Files.createDirectories(filePath.getParent());
                            
                            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                                int bytesRead;
                                while ((bytesRead = zis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                    progress.addProcessedBytes(bytesRead);
                                }
                            }
                            
                            // Set file modification time
                            Files.setLastModifiedTime(filePath, FileTime.fromMillis(entry.getTime()));
                            
                            progress.incrementProcessedFiles();
                            zis.closeEntry();
                        }
                    }
                    
                    logger.info("World restored successfully from snapshot");
                    return new RestoreResult(true, "World restored successfully", 
                                           progress.getProcessedFiles(), progress.getProcessedBytes());
                    
                } catch (Exception e) {
                    // Restore backup on failure
                    if (backupPath != null) {
                        restoreBackup(targetWorldPath, backupPath);
                    }
                    throw e;
                }
                
            } catch (Exception e) {
                logger.error("Failed to restore from snapshot", e);
                return new RestoreResult(false, "Failed to restore: " + e.getMessage(), 0, 0);
            }
        });
    }
    
    /**
     * Lists available snapshots from all providers.
     */
    public Map<String, List<CloudSnapshot>> listAllSnapshots() {
        Map<String, List<CloudSnapshot>> allSnapshots = new HashMap<>();
        
        for (Map.Entry<String, CloudProvider> entry : providers.entrySet()) {
            try {
                List<CloudSnapshot> snapshots = entry.getValue().listSnapshots();
                allSnapshots.put(entry.getKey(), snapshots);
            } catch (Exception e) {
                logger.warn("Failed to list snapshots for provider: {}", entry.getKey(), e);
                allSnapshots.put(entry.getKey(), Collections.emptyList());
            }
        }
        
        return allSnapshots;
    }
    
    /**
     * Shuts down the cloud sync service.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Cloud sync service shut down");
    }
    
    // Private helper methods
    
    private List<LocalSnapshot> scanLocalSnapshots() throws IOException {
        List<LocalSnapshot> snapshots = new ArrayList<>();
        Path snapshotsDir = localSyncPath.resolve("snapshots");
        
        if (!Files.exists(snapshotsDir)) {
            return snapshots;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotsDir, "*.zip")) {
            for (Path snapshotFile : stream) {
                String name = snapshotFile.getFileName().toString();
                long timestamp = Files.getLastModifiedTime(snapshotFile).toMillis();
                long size = Files.size(snapshotFile);
                
                snapshots.add(new LocalSnapshot(name, timestamp, size, snapshotFile));
            }
        }
        
        return snapshots;
    }
    
    private boolean hasRemoteSnapshot(List<CloudSnapshot> remoteSnapshots, LocalSnapshot local) {
        return remoteSnapshots.stream()
            .anyMatch(remote -> remote.name().equals(local.name()));
    }
    
    private boolean hasLocalSnapshot(List<LocalSnapshot> localSnapshots, CloudSnapshot remote) {
        return localSnapshots.stream()
            .anyMatch(local -> local.name().equals(remote.name()));
    }
    
    private String generateSnapshotName(Path worldPath) {
        String worldName = worldPath.getFileName().toString();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            .replace(":", "-").replace(".", "-");
        return worldName + "_" + timestamp;
    }
    
    private long calculateWorldSize(Path worldPath, SnapshotProgress progress) throws IOException {
        return Files.walk(worldPath)
            .filter(Files::isRegularFile)
            .mapToLong(file -> {
                try {
                    long size = Files.size(file);
                    progress.incrementTotalFiles();
                    return size;
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }
    
    private void addSnapshotMetadata(ZipOutputStream zos, Path worldPath, String snapshotName) throws IOException {
        String metadata = String.format("""
            {
                "snapshotName": "%s",
                "worldName": "%s",
                "timestamp": %d,
                "version": "1.0",
                "creator": "Odyssey Cloud Sync"
            }
            """, snapshotName, worldPath.getFileName().toString(), System.currentTimeMillis());
        
        ZipEntry entry = new ZipEntry("snapshot_metadata.json");
        entry.setTime(System.currentTimeMillis());
        zos.putNextEntry(entry);
        zos.write(metadata.getBytes("UTF-8"));
        zos.closeEntry();
    }
    
    private void addWorldFilesToSnapshot(ZipOutputStream zos, Path worldPath, Path basePath, 
                                       SnapshotProgress progress) throws IOException {
        
        Files.walk(worldPath)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    String relativePath = basePath.relativize(file).toString().replace('\\', '/');
                    
                    ZipEntry entry = new ZipEntry(relativePath);
                    entry.setTime(Files.getLastModifiedTime(file).toMillis());
                    zos.putNextEntry(entry);
                    
                    long fileSize = Files.copy(file, zos);
                    zos.closeEntry();
                    
                    progress.incrementProcessedFiles();
                    progress.addProcessedBytes(fileSize);
                    
                } catch (IOException e) {
                    logger.warn("Failed to add file to snapshot: {}", file, e);
                }
            });
    }
    
    private Path createBackup(Path worldPath) throws IOException {
        Path backupPath = worldPath.getParent().resolve(worldPath.getFileName() + "_backup_" + System.currentTimeMillis());
        copyDirectory(worldPath, backupPath);
        return backupPath;
    }
    
    private void restoreBackup(Path worldPath, Path backupPath) throws IOException {
        deleteDirectory(worldPath);
        copyDirectory(backupPath, worldPath);
        deleteDirectory(backupPath);
    }
    
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy: " + sourcePath, e);
            }
        });
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: {}", path, e);
                    }
                });
        }
    }
    
    // Interface and record definitions
    
    /**
     * Interface for cloud storage providers.
     */
    public interface CloudProvider {
        List<CloudSnapshot> listSnapshots() throws IOException;
        void uploadSnapshot(Path localPath, String remoteName) throws IOException;
        void downloadSnapshot(String remoteName, Path localPath) throws IOException;
        void deleteSnapshot(String remoteName) throws IOException;
        boolean isConnected();
        String getProviderName();
    }
    
    // Record classes
    public record CloudSnapshot(String name, long timestamp, long size, String checksum) {}
    
    public record LocalSnapshot(String name, long timestamp, long size, Path path) {}
    
    public record SyncResult(boolean success, String message, int uploaded, int downloaded) {}
    
    public record SnapshotResult(boolean success, String message, Path snapshotPath, 
                                long size, double compressionRatio) {}
    
    public record RestoreResult(boolean success, String message, int filesRestored, long bytesRestored) {}
    
    // Progress tracking classes
    public static class SyncProgress {
        private int totalRemoteSnapshots = 0;
        private int totalLocalSnapshots = 0;
        private int uploaded = 0;
        private int downloaded = 0;
        
        public void setTotalRemoteSnapshots(int total) { this.totalRemoteSnapshots = total; }
        public void setTotalLocalSnapshots(int total) { this.totalLocalSnapshots = total; }
        public void incrementUploaded() { this.uploaded++; }
        public void incrementDownloaded() { this.downloaded++; }
        
        public int getTotalRemoteSnapshots() { return totalRemoteSnapshots; }
        public int getTotalLocalSnapshots() { return totalLocalSnapshots; }
        public int getUploaded() { return uploaded; }
        public int getDownloaded() { return downloaded; }
    }
    
    public static class SnapshotProgress {
        private long totalBytes = 0;
        private long processedBytes = 0;
        private int totalFiles = 0;
        private int processedFiles = 0;
        
        public void setTotalBytes(long total) { this.totalBytes = total; }
        public void addProcessedBytes(long bytes) { this.processedBytes += bytes; }
        public void incrementTotalFiles() { this.totalFiles++; }
        public void incrementProcessedFiles() { this.processedFiles++; }
        
        public double getProgressPercentage() {
            return totalBytes > 0 ? (double) processedBytes / totalBytes * 100.0 : 0.0;
        }
        
        public long getTotalBytes() { return totalBytes; }
        public long getProcessedBytes() { return processedBytes; }
        public int getTotalFiles() { return totalFiles; }
        public int getProcessedFiles() { return processedFiles; }
    }
    
    public static class RestoreProgress {
        private long processedBytes = 0;
        private int processedFiles = 0;
        
        public void addProcessedBytes(long bytes) { this.processedBytes += bytes; }
        public void incrementProcessedFiles() { this.processedFiles++; }
        
        public long getProcessedBytes() { return processedBytes; }
        public int getProcessedFiles() { return processedFiles; }
    }
    
    // Sync state management
    private static class SyncStateManager {
        private final Path stateFile;
        
        public SyncStateManager(Path stateFile) {
            this.stateFile = stateFile;
        }
        
        public SyncState loadState(String providerName) {
            // Placeholder implementation - would use JSON parsing in real implementation
            return new SyncState();
        }
        
        public void saveState(String providerName, SyncState state) {
            // Placeholder implementation - would use JSON serialization in real implementation
        }
    }
    
    private static class SyncState {
        private final Map<String, Long> uploadedSnapshots = new HashMap<>();
        private final Map<String, Long> downloadedSnapshots = new HashMap<>();
        
        public void addUploadedSnapshot(String name, long timestamp) {
            uploadedSnapshots.put(name, timestamp);
        }
        
        public void addDownloadedSnapshot(String name, long timestamp) {
            downloadedSnapshots.put(name, timestamp);
        }
        
        public Map<String, Long> getUploadedSnapshots() { return uploadedSnapshots; }
        public Map<String, Long> getDownloadedSnapshots() { return downloadedSnapshots; }
    }
}