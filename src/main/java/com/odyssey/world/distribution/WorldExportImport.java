package com.odyssey.world.distribution;

import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.*;

/**
 * Handles world export and import operations using .odysseyWorld archive format.
 * Supports compression, metadata, and progress tracking.
 */
public class WorldExportImport {
    private static final Logger logger = LoggerFactory.getLogger(WorldExportImport.class);
    
    private static final String ARCHIVE_EXTENSION = ".odysseyWorld";
    private static final String METADATA_FILE = "world_metadata.json";
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Exports a world to a .odysseyWorld archive file.
     */
    public CompletableFuture<ExportResult> exportWorld(Path worldPath, Path outputPath, ExportOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(worldPath)) {
                    return new ExportResult(false, "World path does not exist: " + worldPath, 0);
                }
                
                // Ensure output has correct extension
                if (!outputPath.toString().endsWith(ARCHIVE_EXTENSION)) {
                    outputPath = outputPath.resolveSibling(outputPath.getFileName() + ARCHIVE_EXTENSION);
                }
                
                logger.info("Exporting world from {} to {}", worldPath, outputPath);
                
                ExportProgress progress = new ExportProgress();
                long totalSize = calculateWorldSize(worldPath, progress);
                progress.setTotalBytes(totalSize);
                
                try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    
                    // Set compression level
                    zos.setLevel(options.compressionLevel());
                    
                    // Add metadata first
                    addMetadata(zos, worldPath, options);
                    
                    // Add world files
                    addWorldFiles(zos, worldPath, worldPath, progress, options);
                    
                    zos.finish();
                }
                
                long finalSize = Files.size(outputPath);
                double compressionRatio = (double) finalSize / totalSize;
                
                logger.info("Export completed. Original: {} bytes, Compressed: {} bytes, Ratio: {:.2f}%", 
                           totalSize, finalSize, compressionRatio * 100);
                
                return new ExportResult(true, "Export completed successfully", finalSize);
                
            } catch (Exception e) {
                logger.error("Export failed", e);
                return new ExportResult(false, "Export failed: " + e.getMessage(), 0);
            }
        });
    }
    
    /**
     * Imports a world from a .odysseyWorld archive file.
     */
    public CompletableFuture<ImportResult> importWorld(Path archivePath, Path targetPath, ImportOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(archivePath)) {
                    return new ImportResult(false, "Archive file does not exist: " + archivePath, null);
                }
                
                logger.info("Importing world from {} to {}", archivePath, targetPath);
                
                ImportProgress progress = new ImportProgress();
                WorldMetadata metadata;
                
                try (FileInputStream fis = new FileInputStream(archivePath.toFile());
                     ZipInputStream zis = new ZipInputStream(fis)) {
                    
                    // Read metadata first
                    metadata = readMetadata(zis);
                    if (metadata == null) {
                        return new ImportResult(false, "Invalid archive: missing metadata", null);
                    }
                    
                    // Validate compatibility
                    if (!isCompatible(metadata, options)) {
                        return new ImportResult(false, "Incompatible world version", metadata);
                    }
                    
                    // Create target directory
                    Files.createDirectories(targetPath);
                    
                    // Extract world files
                    extractWorldFiles(zis, targetPath, progress, options);
                }
                
                logger.info("Import completed successfully");
                return new ImportResult(true, "Import completed successfully", metadata);
                
            } catch (Exception e) {
                logger.error("Import failed", e);
                return new ImportResult(false, "Import failed: " + e.getMessage(), null);
            }
        });
    }
    
    /**
     * Lists the contents of a .odysseyWorld archive without extracting.
     */
    public ArchiveInfo inspectArchive(Path archivePath) throws IOException {
        List<ArchiveEntry> entries = new ArrayList<>();
        WorldMetadata metadata = null;
        
        try (FileInputStream fis = new FileInputStream(archivePath.toFile());
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(METADATA_FILE)) {
                    metadata = parseMetadata(readEntryContent(zis));
                } else {
                    entries.add(new ArchiveEntry(
                        entry.getName(),
                        entry.getSize(),
                        entry.getCompressedSize(),
                        entry.getTime()
                    ));
                }
                zis.closeEntry();
            }
        }
        
        return new ArchiveInfo(metadata, entries);
    }
    
    /**
     * Calculates the total size of a world directory.
     */
    private long calculateWorldSize(Path worldPath, ExportProgress progress) throws IOException {
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        Files.walk(worldPath)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    totalSize.addAndGet(Files.size(file));
                    fileCount.incrementAndGet();
                } catch (IOException e) {
                    logger.warn("Could not get size of file: {}", file, e);
                }
            });
        
        progress.setTotalFiles(fileCount.get());
        return totalSize.get();
    }
    
    /**
     * Adds metadata to the archive.
     */
    private void addMetadata(ZipOutputStream zos, Path worldPath, ExportOptions options) throws IOException {
        WorldMetadata metadata = createMetadata(worldPath, options);
        String metadataJson = serializeMetadata(metadata);
        
        ZipEntry entry = new ZipEntry(METADATA_FILE);
        entry.setTime(System.currentTimeMillis());
        zos.putNextEntry(entry);
        zos.write(metadataJson.getBytes("UTF-8"));
        zos.closeEntry();
    }
    
    /**
     * Adds world files to the archive.
     */
    private void addWorldFiles(ZipOutputStream zos, Path worldPath, Path basePath, 
                              ExportProgress progress, ExportOptions options) throws IOException {
        
        Files.walk(worldPath)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    String relativePath = basePath.relativize(file).toString().replace('\\', '/');
                    
                    // Skip files based on options
                    if (shouldSkipFile(relativePath, options)) {
                        return;
                    }
                    
                    ZipEntry entry = new ZipEntry(relativePath);
                    entry.setTime(Files.getLastModifiedTime(file).toMillis());
                    zos.putNextEntry(entry);
                    
                    Files.copy(file, zos);
                    zos.closeEntry();
                    
                    progress.incrementProcessedFiles();
                    progress.addProcessedBytes(Files.size(file));
                    
                } catch (IOException e) {
                    logger.warn("Failed to add file to archive: {}", file, e);
                }
            });
    }
    
    /**
     * Reads metadata from the archive.
     */
    private WorldMetadata readMetadata(ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(METADATA_FILE)) {
                String content = readEntryContent(zis);
                zis.closeEntry();
                return parseMetadata(content);
            }
            zis.closeEntry();
        }
        return null;
    }
    
    /**
     * Extracts world files from the archive.
     */
    private void extractWorldFiles(ZipInputStream zis, Path targetPath, 
                                  ImportProgress progress, ImportOptions options) throws IOException {
        
        ZipEntry entry;
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(METADATA_FILE)) {
                zis.closeEntry();
                continue; // Skip metadata, already processed
            }
            
            Path filePath = targetPath.resolve(entry.getName());
            
            // Ensure parent directories exist
            Files.createDirectories(filePath.getParent());
            
            // Extract file
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
    
    /**
     * Reads the content of a zip entry as a string.
     */
    private String readEntryContent(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        
        while ((bytesRead = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        return baos.toString("UTF-8");
    }
    
    /**
     * Creates metadata for the world being exported.
     */
    private WorldMetadata createMetadata(Path worldPath, ExportOptions options) {
        return new WorldMetadata(
            worldPath.getFileName().toString(),
            WorldSaveFormat.CURRENT_VERSION,
            System.currentTimeMillis(),
            options.exporterName(),
            options.description(),
            calculateWorldSize(worldPath),
            getWorldSeed(worldPath)
        );
    }
    
    /**
     * Determines if a file should be skipped during export.
     */
    private boolean shouldSkipFile(String relativePath, ExportOptions options) {
        // Skip temporary files
        if (relativePath.endsWith(".tmp") || relativePath.endsWith(".lock")) {
            return true;
        }
        
        // Skip session files if not including them
        if (!options.includeSessionData() && relativePath.contains("session")) {
            return true;
        }
        
        // Skip player data if not including it
        if (!options.includePlayerData() && relativePath.contains("players")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if the world metadata is compatible with import options.
     */
    private boolean isCompatible(WorldMetadata metadata, ImportOptions options) {
        if (metadata.version() > WorldSaveFormat.CURRENT_VERSION) {
            return false; // Future version
        }
        
        if (metadata.version() < options.minimumVersion()) {
            return false; // Too old
        }
        
        return true;
    }
    
    /**
     * Calculates the size of a world directory.
     */
    private long calculateWorldSize(Path worldPath) {
        try {
            return Files.walk(worldPath)
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Gets the world seed from level.dat.
     */
    private long getWorldSeed(Path worldPath) {
        Path levelDat = worldPath.resolve("level.dat");
        if (!Files.exists(levelDat)) {
            return 0;
        }
        
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(levelDat))) {
            dis.readInt(); // magic
            dis.readInt(); // version
            dis.readLong(); // timestamp
            return dis.readLong(); // seed
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Serializes metadata to JSON.
     */
    private String serializeMetadata(WorldMetadata metadata) {
        // Simple JSON serialization - in a real implementation, use Gson
        return String.format("""
            {
                "name": "%s",
                "version": %d,
                "exportTime": %d,
                "exporter": "%s",
                "description": "%s",
                "size": %d,
                "seed": %d
            }
            """, metadata.name(), metadata.version(), metadata.exportTime(),
                metadata.exporter(), metadata.description(), metadata.size(), metadata.seed());
    }
    
    /**
     * Parses metadata from JSON.
     */
    private WorldMetadata parseMetadata(String json) {
        // Simple JSON parsing - in a real implementation, use Gson
        // This is a placeholder implementation
        return new WorldMetadata("Unknown", 1, System.currentTimeMillis(), 
                                "Unknown", "", 0, 0);
    }
    
    // Record classes for data structures
    public record ExportOptions(int compressionLevel, boolean includePlayerData, 
                               boolean includeSessionData, String exporterName, String description) {
        public static ExportOptions defaults() {
            return new ExportOptions(6, true, false, "Odyssey", "");
        }
    }
    
    public record ImportOptions(int minimumVersion, boolean overwriteExisting) {
        public static ImportOptions defaults() {
            return new ImportOptions(1, false);
        }
    }
    
    public record ExportResult(boolean success, String message, long archiveSize) {}
    
    public record ImportResult(boolean success, String message, WorldMetadata metadata) {}
    
    public record WorldMetadata(String name, int version, long exportTime, 
                               String exporter, String description, long size, long seed) {}
    
    public record ArchiveEntry(String path, long size, long compressedSize, long modifiedTime) {}
    
    public record ArchiveInfo(WorldMetadata metadata, List<ArchiveEntry> entries) {}
    
    // Progress tracking classes
    public static class ExportProgress {
        private final AtomicLong totalBytes = new AtomicLong(0);
        private final AtomicLong processedBytes = new AtomicLong(0);
        private final AtomicLong totalFiles = new AtomicLong(0);
        private final AtomicLong processedFiles = new AtomicLong(0);
        
        public void setTotalBytes(long total) { totalBytes.set(total); }
        public void setTotalFiles(long total) { totalFiles.set(total); }
        public void addProcessedBytes(long bytes) { processedBytes.addAndGet(bytes); }
        public void incrementProcessedFiles() { processedFiles.incrementAndGet(); }
        
        public double getProgressPercentage() {
            long total = totalBytes.get();
            return total > 0 ? (double) processedBytes.get() / total * 100.0 : 0.0;
        }
        
        public long getTotalBytes() { return totalBytes.get(); }
        public long getProcessedBytes() { return processedBytes.get(); }
        public long getTotalFiles() { return totalFiles.get(); }
        public long getProcessedFiles() { return processedFiles.get(); }
    }
    
    public static class ImportProgress {
        private final AtomicLong processedBytes = new AtomicLong(0);
        private final AtomicLong processedFiles = new AtomicLong(0);
        
        public void addProcessedBytes(long bytes) { processedBytes.addAndGet(bytes); }
        public void incrementProcessedFiles() { processedFiles.incrementAndGet(); }
        
        public long getProcessedBytes() { return processedBytes.get(); }
        public long getProcessedFiles() { return processedFiles.get(); }
    }
}