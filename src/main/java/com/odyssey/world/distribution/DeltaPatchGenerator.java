package com.odyssey.world.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Generates binary diffs (delta patches) for efficient world updates.
 * Uses a rolling hash algorithm for efficient binary diffing.
 */
public class DeltaPatchGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DeltaPatchGenerator.class);
    
    private static final int BLOCK_SIZE = 4096;
    private static final String PATCH_EXTENSION = ".odysseyPatch";
    private static final int PATCH_VERSION = 1;
    private static final byte[] PATCH_MAGIC = {'O', 'D', 'Y', 'P'};
    
    /**
     * Generates a delta patch between two world versions.
     */
    public CompletableFuture<PatchResult> generatePatch(Path oldWorldPath, Path newWorldPath, Path patchPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Generating patch from {} to {}", oldWorldPath, newWorldPath);
                
                PatchProgress progress = new PatchProgress();
                List<FileDelta> deltas = new ArrayList<>();
                
                // Analyze file differences
                Map<String, FileInfo> oldFiles = scanFiles(oldWorldPath);
                Map<String, FileInfo> newFiles = scanFiles(newWorldPath);
                
                progress.setTotalFiles(newFiles.size());
                
                // Find added, modified, and deleted files
                Set<String> allPaths = new HashSet<>();
                allPaths.addAll(oldFiles.keySet());
                allPaths.addAll(newFiles.keySet());
                
                for (String relativePath : allPaths) {
                    FileInfo oldFile = oldFiles.get(relativePath);
                    FileInfo newFile = newFiles.get(relativePath);
                    
                    if (oldFile == null) {
                        // File added
                        deltas.add(createAddedFileDelta(relativePath, newWorldPath.resolve(relativePath)));
                    } else if (newFile == null) {
                        // File deleted
                        deltas.add(createDeletedFileDelta(relativePath));
                    } else if (!Arrays.equals(oldFile.hash(), newFile.hash())) {
                        // File modified
                        deltas.add(createModifiedFileDelta(relativePath, 
                                                         oldWorldPath.resolve(relativePath),
                                                         newWorldPath.resolve(relativePath)));
                    }
                    
                    progress.incrementProcessedFiles();
                }
                
                // Write patch file
                writePatchFile(patchPath, deltas, oldFiles, newFiles);
                
                long patchSize = Files.size(patchPath);
                logger.info("Patch generated successfully. Size: {} bytes, {} deltas", patchSize, deltas.size());
                
                return new PatchResult(true, "Patch generated successfully", patchSize, deltas.size());
                
            } catch (Exception e) {
                logger.error("Failed to generate patch", e);
                return new PatchResult(false, "Failed to generate patch: " + e.getMessage(), 0, 0);
            }
        });
    }
    
    /**
     * Applies a delta patch to a world.
     */
    public CompletableFuture<ApplyResult> applyPatch(Path worldPath, Path patchPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Applying patch {} to world {}", patchPath, worldPath);
                
                // Create backup
                Path backupPath = createBackup(worldPath);
                
                try {
                    PatchData patchData = readPatchFile(patchPath);
                    ApplyProgress progress = new ApplyProgress();
                    progress.setTotalDeltas(patchData.deltas().size());
                    
                    int appliedDeltas = 0;
                    for (FileDelta delta : patchData.deltas()) {
                        applyDelta(worldPath, delta);
                        appliedDeltas++;
                        progress.incrementAppliedDeltas();
                    }
                    
                    logger.info("Patch applied successfully. {} deltas applied", appliedDeltas);
                    return new ApplyResult(true, "Patch applied successfully", appliedDeltas);
                    
                } catch (Exception e) {
                    // Restore backup on failure
                    restoreBackup(worldPath, backupPath);
                    throw e;
                }
                
            } catch (Exception e) {
                logger.error("Failed to apply patch", e);
                return new ApplyResult(false, "Failed to apply patch: " + e.getMessage(), 0);
            }
        });
    }
    
    /**
     * Validates a patch file without applying it.
     */
    public PatchValidation validatePatch(Path patchPath, Path worldPath) throws IOException {
        PatchData patchData = readPatchFile(patchPath);
        Map<String, FileInfo> currentFiles = scanFiles(worldPath);
        
        List<String> issues = new ArrayList<>();
        
        for (FileDelta delta : patchData.deltas()) {
            switch (delta.operation()) {
                case MODIFY, DELETE -> {
                    FileInfo currentFile = currentFiles.get(delta.relativePath());
                    if (currentFile == null) {
                        issues.add("File not found for " + delta.operation() + ": " + delta.relativePath());
                    } else if (delta.operation() == DeltaOperation.MODIFY && 
                              !Arrays.equals(currentFile.hash(), delta.oldHash())) {
                        issues.add("Hash mismatch for file: " + delta.relativePath());
                    }
                }
                case ADD -> {
                    if (currentFiles.containsKey(delta.relativePath())) {
                        issues.add("File already exists for ADD: " + delta.relativePath());
                    }
                }
            }
        }
        
        return new PatchValidation(issues.isEmpty(), issues);
    }
    
    /**
     * Scans files in a directory and creates a map of file information.
     */
    private Map<String, FileInfo> scanFiles(Path directory) throws IOException {
        Map<String, FileInfo> files = new HashMap<>();
        
        if (!Files.exists(directory)) {
            return files;
        }
        
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    String relativePath = directory.relativize(file).toString().replace('\\', '/');
                    byte[] hash = calculateFileHash(file);
                    long size = Files.size(file);
                    long lastModified = Files.getLastModifiedTime(file).toMillis();
                    
                    files.put(relativePath, new FileInfo(hash, size, lastModified));
                } catch (IOException e) {
                    logger.warn("Failed to process file: {}", file, e);
                }
            });
        
        return files;
    }
    
    /**
     * Creates a delta for an added file.
     */
    private FileDelta createAddedFileDelta(String relativePath, Path filePath) throws IOException {
        byte[] content = Files.readAllBytes(filePath);
        byte[] hash = calculateHash(content);
        
        return new FileDelta(
            DeltaOperation.ADD,
            relativePath,
            null, // No old hash for added files
            hash,
            content,
            null // No binary diff for added files
        );
    }
    
    /**
     * Creates a delta for a deleted file.
     */
    private FileDelta createDeletedFileDelta(String relativePath) {
        return new FileDelta(
            DeltaOperation.DELETE,
            relativePath,
            null, // Hash not needed for deletion
            null,
            null,
            null
        );
    }
    
    /**
     * Creates a delta for a modified file using binary diff.
     */
    private FileDelta createModifiedFileDelta(String relativePath, Path oldFile, Path newFile) throws IOException {
        byte[] oldContent = Files.readAllBytes(oldFile);
        byte[] newContent = Files.readAllBytes(newFile);
        
        byte[] oldHash = calculateHash(oldContent);
        byte[] newHash = calculateHash(newContent);
        
        // Generate binary diff
        byte[] binaryDiff = generateBinaryDiff(oldContent, newContent);
        
        return new FileDelta(
            DeltaOperation.MODIFY,
            relativePath,
            oldHash,
            newHash,
            null, // Full content not stored for modified files
            binaryDiff
        );
    }
    
    /**
     * Generates a binary diff between two byte arrays using a simple algorithm.
     */
    private byte[] generateBinaryDiff(byte[] oldData, byte[] newData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos))) {
            
            // Simple diff format: [operation][offset][length][data]
            // Operation: 0=copy from old, 1=insert new data
            
            int oldPos = 0;
            int newPos = 0;
            
            while (newPos < newData.length) {
                // Find longest match in old data
                int bestOldPos = -1;
                int bestLength = 0;
                
                for (int searchPos = 0; searchPos <= oldData.length - 1; searchPos++) {
                    int length = 0;
                    while (searchPos + length < oldData.length && 
                           newPos + length < newData.length &&
                           oldData[searchPos + length] == newData[newPos + length]) {
                        length++;
                    }
                    
                    if (length > bestLength && length >= 4) { // Minimum match length
                        bestOldPos = searchPos;
                        bestLength = length;
                    }
                }
                
                if (bestLength > 0) {
                    // Copy from old data
                    dos.writeByte(0); // Copy operation
                    dos.writeInt(bestOldPos);
                    dos.writeInt(bestLength);
                    newPos += bestLength;
                } else {
                    // Insert new data
                    dos.writeByte(1); // Insert operation
                    dos.writeInt(1); // Length of 1 byte
                    dos.writeByte(newData[newPos]);
                    newPos++;
                }
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Applies a binary diff to reconstruct the new data.
     */
    private byte[] applyBinaryDiff(byte[] oldData, byte[] diff) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(diff)))) {
            while (dis.available() > 0) {
                byte operation = dis.readByte();
                
                if (operation == 0) {
                    // Copy from old data
                    int offset = dis.readInt();
                    int length = dis.readInt();
                    result.write(oldData, offset, length);
                } else if (operation == 1) {
                    // Insert new data
                    int length = dis.readInt();
                    for (int i = 0; i < length; i++) {
                        result.write(dis.readByte());
                    }
                }
            }
        }
        
        return result.toByteArray();
    }
    
    /**
     * Writes a patch file containing all deltas.
     */
    private void writePatchFile(Path patchPath, List<FileDelta> deltas, 
                               Map<String, FileInfo> oldFiles, Map<String, FileInfo> newFiles) throws IOException {
        
        try (DataOutputStream dos = new DataOutputStream(
                new GZIPOutputStream(Files.newOutputStream(patchPath)))) {
            
            // Write header
            dos.write(PATCH_MAGIC);
            dos.writeInt(PATCH_VERSION);
            dos.writeLong(System.currentTimeMillis());
            dos.writeInt(deltas.size());
            
            // Write deltas
            for (FileDelta delta : deltas) {
                writeDelta(dos, delta);
            }
        }
    }
    
    /**
     * Reads a patch file and returns the patch data.
     */
    private PatchData readPatchFile(Path patchPath) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new GZIPInputStream(Files.newInputStream(patchPath)))) {
            
            // Read header
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (!Arrays.equals(magic, PATCH_MAGIC)) {
                throw new IOException("Invalid patch file magic");
            }
            
            int version = dis.readInt();
            if (version != PATCH_VERSION) {
                throw new IOException("Unsupported patch version: " + version);
            }
            
            long timestamp = dis.readLong();
            int deltaCount = dis.readInt();
            
            // Read deltas
            List<FileDelta> deltas = new ArrayList<>();
            for (int i = 0; i < deltaCount; i++) {
                deltas.add(readDelta(dis));
            }
            
            return new PatchData(version, timestamp, deltas);
        }
    }
    
    /**
     * Writes a single delta to the output stream.
     */
    private void writeDelta(DataOutputStream dos, FileDelta delta) throws IOException {
        dos.writeByte(delta.operation().ordinal());
        dos.writeUTF(delta.relativePath());
        
        // Write old hash
        if (delta.oldHash() != null) {
            dos.writeBoolean(true);
            dos.writeInt(delta.oldHash().length);
            dos.write(delta.oldHash());
        } else {
            dos.writeBoolean(false);
        }
        
        // Write new hash
        if (delta.newHash() != null) {
            dos.writeBoolean(true);
            dos.writeInt(delta.newHash().length);
            dos.write(delta.newHash());
        } else {
            dos.writeBoolean(false);
        }
        
        // Write content or binary diff
        if (delta.content() != null) {
            dos.writeBoolean(true);
            dos.writeInt(delta.content().length);
            dos.write(delta.content());
        } else if (delta.binaryDiff() != null) {
            dos.writeBoolean(true);
            dos.writeInt(delta.binaryDiff().length);
            dos.write(delta.binaryDiff());
        } else {
            dos.writeBoolean(false);
        }
    }
    
    /**
     * Reads a single delta from the input stream.
     */
    private FileDelta readDelta(DataInputStream dis) throws IOException {
        DeltaOperation operation = DeltaOperation.values()[dis.readByte()];
        String relativePath = dis.readUTF();
        
        // Read old hash
        byte[] oldHash = null;
        if (dis.readBoolean()) {
            int length = dis.readInt();
            oldHash = new byte[length];
            dis.readFully(oldHash);
        }
        
        // Read new hash
        byte[] newHash = null;
        if (dis.readBoolean()) {
            int length = dis.readInt();
            newHash = new byte[length];
            dis.readFully(newHash);
        }
        
        // Read content or binary diff
        byte[] data = null;
        if (dis.readBoolean()) {
            int length = dis.readInt();
            data = new byte[length];
            dis.readFully(data);
        }
        
        // Determine if data is content or binary diff
        byte[] content = null;
        byte[] binaryDiff = null;
        if (operation == DeltaOperation.ADD) {
            content = data;
        } else if (operation == DeltaOperation.MODIFY) {
            binaryDiff = data;
        }
        
        return new FileDelta(operation, relativePath, oldHash, newHash, content, binaryDiff);
    }
    
    /**
     * Applies a single delta to the world.
     */
    private void applyDelta(Path worldPath, FileDelta delta) throws IOException {
        Path filePath = worldPath.resolve(delta.relativePath());
        
        switch (delta.operation()) {
            case ADD -> {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, delta.content());
            }
            case DELETE -> {
                Files.deleteIfExists(filePath);
            }
            case MODIFY -> {
                byte[] oldContent = Files.readAllBytes(filePath);
                byte[] newContent = applyBinaryDiff(oldContent, delta.binaryDiff());
                Files.write(filePath, newContent);
            }
        }
    }
    
    /**
     * Creates a backup of the world directory.
     */
    private Path createBackup(Path worldPath) throws IOException {
        Path backupPath = worldPath.getParent().resolve(worldPath.getFileName() + "_backup_" + System.currentTimeMillis());
        copyDirectory(worldPath, backupPath);
        return backupPath;
    }
    
    /**
     * Restores a backup.
     */
    private void restoreBackup(Path worldPath, Path backupPath) throws IOException {
        deleteDirectory(worldPath);
        copyDirectory(backupPath, worldPath);
        deleteDirectory(backupPath);
    }
    
    /**
     * Copies a directory recursively.
     */
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
    
    /**
     * Deletes a directory recursively.
     */
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
    
    /**
     * Calculates the SHA-256 hash of a file.
     */
    private byte[] calculateFileHash(Path file) throws IOException {
        return calculateHash(Files.readAllBytes(file));
    }
    
    /**
     * Calculates the SHA-256 hash of a byte array.
     */
    private byte[] calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    // Enums and record classes
    public enum DeltaOperation {
        ADD, MODIFY, DELETE
    }
    
    public record FileInfo(byte[] hash, long size, long lastModified) {}
    
    public record FileDelta(DeltaOperation operation, String relativePath, 
                           byte[] oldHash, byte[] newHash, byte[] content, byte[] binaryDiff) {}
    
    public record PatchData(int version, long timestamp, List<FileDelta> deltas) {}
    
    public record PatchResult(boolean success, String message, long patchSize, int deltaCount) {}
    
    public record ApplyResult(boolean success, String message, int appliedDeltas) {}
    
    public record PatchValidation(boolean valid, List<String> issues) {}
    
    // Progress tracking classes
    public static class PatchProgress {
        private int totalFiles = 0;
        private int processedFiles = 0;
        
        public void setTotalFiles(int total) { this.totalFiles = total; }
        public void incrementProcessedFiles() { this.processedFiles++; }
        
        public double getProgressPercentage() {
            return totalFiles > 0 ? (double) processedFiles / totalFiles * 100.0 : 0.0;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getProcessedFiles() { return processedFiles; }
    }
    
    public static class ApplyProgress {
        private int totalDeltas = 0;
        private int appliedDeltas = 0;
        
        public void setTotalDeltas(int total) { this.totalDeltas = total; }
        public void incrementAppliedDeltas() { this.appliedDeltas++; }
        
        public double getProgressPercentage() {
            return totalDeltas > 0 ? (double) appliedDeltas / totalDeltas * 100.0 : 0.0;
        }
        
        public int getTotalDeltas() { return totalDeltas; }
        public int getAppliedDeltas() { return appliedDeltas; }
    }
}