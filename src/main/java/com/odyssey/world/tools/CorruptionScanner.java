package com.odyssey.world.tools;

import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

/**
 * Corruption Scanner & Repair CLI tool for detecting and fixing world data corruption.
 * Provides comprehensive scanning, validation, and repair capabilities.
 */
public class CorruptionScanner {
    private static final Logger logger = LoggerFactory.getLogger(CorruptionScanner.class);
    
    private final ExecutorService executor;
    private final ScanProgress progress;
    private final List<CorruptionIssue> issues;
    
    public CorruptionScanner() {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.progress = new ScanProgress();
        this.issues = Collections.synchronizedList(new ArrayList<>());
    }
    
    /**
     * Main CLI entry point for the corruption scanner.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        Path worldPath = Paths.get(args[1]);
        
        CorruptionScanner scanner = new CorruptionScanner();
        
        try {
            switch (command.toLowerCase()) {
                case "scan" -> {
                    ScanOptions options = parseScanOptions(args);
                    ScanResult result = scanner.scanWorld(worldPath, options).get();
                    printScanResult(result);
                }
                case "repair" -> {
                    RepairOptions options = parseRepairOptions(args);
                    RepairResult result = scanner.repairWorld(worldPath, options).get();
                    printRepairResult(result);
                }
                case "validate" -> {
                    ValidationResult result = scanner.validateWorld(worldPath).get();
                    printValidationResult(result);
                }
                case "backup" -> {
                    Path backupPath = args.length > 2 ? Paths.get(args[2]) : 
                                     worldPath.getParent().resolve(worldPath.getFileName() + "_backup");
                    BackupResult result = scanner.createBackup(worldPath, backupPath).get();
                    printBackupResult(result);
                }
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("CLI operation failed", e);
            System.exit(1);
        } finally {
            scanner.shutdown();
        }
    }
    
    /**
     * Scans a world for corruption issues.
     */
    public CompletableFuture<ScanResult> scanWorld(Path worldPath, ScanOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting corruption scan of world: {}", worldPath);
                
                if (!Files.exists(worldPath)) {
                    return new ScanResult(false, "World path does not exist", Collections.emptyList());
                }
                
                issues.clear();
                progress.reset();
                
                // Count total files to scan
                long totalFiles = Files.walk(worldPath)
                    .filter(Files::isRegularFile)
                    .count();
                progress.setTotalFiles(totalFiles);
                
                // Scan different components
                List<CompletableFuture<Void>> scanTasks = new ArrayList<>();
                
                if (options.scanLevelData()) {
                    scanTasks.add(CompletableFuture.runAsync(() -> scanLevelData(worldPath), executor));
                }
                
                if (options.scanRegionFiles()) {
                    scanTasks.add(CompletableFuture.runAsync(() -> scanRegionFiles(worldPath), executor));
                }
                
                if (options.scanPlayerData()) {
                    scanTasks.add(CompletableFuture.runAsync(() -> scanPlayerData(worldPath), executor));
                }
                
                if (options.scanSessionData()) {
                    scanTasks.add(CompletableFuture.runAsync(() -> scanSessionData(worldPath), executor));
                }
                
                if (options.scanChecksums()) {
                    scanTasks.add(CompletableFuture.runAsync(() -> scanChecksums(worldPath), executor));
                }
                
                // Wait for all scan tasks to complete
                CompletableFuture.allOf(scanTasks.toArray(new CompletableFuture[0])).join();
                
                // Sort issues by severity
                issues.sort(Comparator.comparing(CorruptionIssue::severity).reversed());
                
                logger.info("Scan completed. Found {} issues", issues.size());
                return new ScanResult(true, "Scan completed successfully", new ArrayList<>(issues));
                
            } catch (Exception e) {
                logger.error("Scan failed", e);
                return new ScanResult(false, "Scan failed: " + e.getMessage(), Collections.emptyList());
            }
        });
    }
    
    /**
     * Repairs corruption issues in a world.
     */
    public CompletableFuture<RepairResult> repairWorld(Path worldPath, RepairOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting repair of world: {}", worldPath);
                
                // First scan for issues if not already done
                if (issues.isEmpty()) {
                    ScanResult scanResult = scanWorld(worldPath, ScanOptions.defaults()).get();
                    if (!scanResult.success()) {
                        return new RepairResult(false, "Failed to scan before repair", 0, 0);
                    }
                }
                
                // Create backup if requested
                if (options.createBackup()) {
                    Path backupPath = worldPath.getParent().resolve(worldPath.getFileName() + "_repair_backup");
                    createBackup(worldPath, backupPath).get();
                }
                
                int repairedCount = 0;
                int failedCount = 0;
                
                for (CorruptionIssue issue : issues) {
                    if (issue.severity().ordinal() < options.minimumSeverity().ordinal()) {
                        continue; // Skip issues below minimum severity
                    }
                    
                    try {
                        if (repairIssue(worldPath, issue, options)) {
                            repairedCount++;
                            logger.debug("Repaired issue: {}", issue.description());
                        } else {
                            failedCount++;
                            logger.warn("Failed to repair issue: {}", issue.description());
                        }
                    } catch (Exception e) {
                        failedCount++;
                        logger.error("Error repairing issue: " + issue.description(), e);
                    }
                }
                
                logger.info("Repair completed. Repaired: {}, Failed: {}", repairedCount, failedCount);
                return new RepairResult(true, "Repair completed", repairedCount, failedCount);
                
            } catch (Exception e) {
                logger.error("Repair failed", e);
                return new RepairResult(false, "Repair failed: " + e.getMessage(), 0, 0);
            }
        });
    }
    
    /**
     * Validates world integrity without making changes.
     */
    public CompletableFuture<ValidationResult> validateWorld(Path worldPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Validating world: {}", worldPath);
                
                ValidationProgress validationProgress = new ValidationProgress();
                List<ValidationIssue> validationIssues = new ArrayList<>();
                
                // Validate world structure
                if (!Files.exists(worldPath.resolve("level.dat"))) {
                    validationIssues.add(new ValidationIssue(
                        ValidationSeverity.CRITICAL,
                        "Missing level.dat file",
                        "level.dat"
                    ));
                }
                
                // Validate region directory structure
                Path regionsDir = worldPath.resolve("regions");
                if (Files.exists(regionsDir)) {
                    validateRegionStructure(regionsDir, validationIssues, validationProgress);
                }
                
                // Validate player data
                Path playersDir = worldPath.resolve("players");
                if (Files.exists(playersDir)) {
                    validatePlayerData(playersDir, validationIssues, validationProgress);
                }
                
                // Calculate validation score
                double score = calculateValidationScore(validationIssues);
                
                logger.info("Validation completed. Score: {:.1f}%, Issues: {}", score, validationIssues.size());
                return new ValidationResult(true, "Validation completed", validationIssues, score);
                
            } catch (Exception e) {
                logger.error("Validation failed", e);
                return new ValidationResult(false, "Validation failed: " + e.getMessage(), 
                                          Collections.emptyList(), 0.0);
            }
        });
    }
    
    /**
     * Creates a backup of the world.
     */
    public CompletableFuture<BackupResult> createBackup(Path worldPath, Path backupPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Creating backup from {} to {}", worldPath, backupPath);
                
                if (Files.exists(backupPath)) {
                    deleteDirectory(backupPath);
                }
                
                Files.createDirectories(backupPath);
                
                BackupProgress backupProgress = new BackupProgress();
                long totalSize = calculateDirectorySize(worldPath);
                backupProgress.setTotalBytes(totalSize);
                
                copyDirectoryWithProgress(worldPath, backupPath, backupProgress);
                
                long backupSize = calculateDirectorySize(backupPath);
                
                logger.info("Backup created successfully. Size: {} bytes", backupSize);
                return new BackupResult(true, "Backup created successfully", backupSize);
                
            } catch (Exception e) {
                logger.error("Backup failed", e);
                return new BackupResult(false, "Backup failed: " + e.getMessage(), 0);
            }
        });
    }
    
    // Private scanning methods
    
    private void scanLevelData(Path worldPath) {
        Path levelDat = worldPath.resolve("level.dat");
        if (!Files.exists(levelDat)) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.CRITICAL,
                "Missing level.dat file",
                levelDat.toString(),
                "The world's main data file is missing"
            ));
            return;
        }
        
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(levelDat))) {
            // Validate magic number
            int magic = dis.readInt();
            if (magic != WorldSaveFormat.LEVEL_MAGIC) {
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.HIGH,
                    "Invalid level.dat magic number",
                    levelDat.toString(),
                    "Expected: " + WorldSaveFormat.LEVEL_MAGIC + ", Found: " + magic
                ));
            }
            
            // Validate version
            int version = dis.readInt();
            if (version < 1 || version > WorldSaveFormat.CURRENT_VERSION) {
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.MEDIUM,
                    "Invalid world version",
                    levelDat.toString(),
                    "Version: " + version
                ));
            }
            
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.HIGH,
                "Cannot read level.dat",
                levelDat.toString(),
                e.getMessage()
            ));
        }
        
        progress.incrementScannedFiles();
    }
    
    private void scanRegionFiles(Path worldPath) {
        Path regionsDir = worldPath.resolve("regions");
        if (!Files.exists(regionsDir)) {
            return;
        }
        
        try {
            Files.walk(regionsDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".region"))
                .forEach(this::scanRegionFile);
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.MEDIUM,
                "Cannot scan regions directory",
                regionsDir.toString(),
                e.getMessage()
            ));
        }
    }
    
    private void scanRegionFile(Path regionFile) {
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(regionFile))) {
            // Validate region header
            int magic = dis.readInt();
            if (magic != WorldSaveFormat.REGION_MAGIC) {
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.HIGH,
                    "Invalid region file magic",
                    regionFile.toString(),
                    "Expected: " + WorldSaveFormat.REGION_MAGIC + ", Found: " + magic
                ));
            }
            
            int version = dis.readInt();
            if (version != WorldSaveFormat.CURRENT_VERSION) {
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.LOW,
                    "Region file version mismatch",
                    regionFile.toString(),
                    "Version: " + version
                ));
            }
            
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.HIGH,
                "Cannot read region file",
                regionFile.toString(),
                e.getMessage()
            ));
        }
        
        progress.incrementScannedFiles();
    }
    
    private void scanPlayerData(Path worldPath) {
        Path playersDir = worldPath.resolve("players");
        if (!Files.exists(playersDir)) {
            return;
        }
        
        try {
            Files.walk(playersDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".dat"))
                .forEach(this::scanPlayerFile);
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.MEDIUM,
                "Cannot scan players directory",
                playersDir.toString(),
                e.getMessage()
            ));
        }
    }
    
    private void scanPlayerFile(Path playerFile) {
        try {
            // Basic validation - check if file is readable and has minimum size
            long size = Files.size(playerFile);
            if (size < 16) { // Minimum expected size
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.MEDIUM,
                    "Player file too small",
                    playerFile.toString(),
                    "Size: " + size + " bytes"
                ));
            }
            
            // Try to read the file
            try (DataInputStream dis = new DataInputStream(
                    new GZIPInputStream(Files.newInputStream(playerFile)))) {
                dis.readInt(); // Try to read first int
            }
            
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.MEDIUM,
                "Cannot read player file",
                playerFile.toString(),
                e.getMessage()
            ));
        }
        
        progress.incrementScannedFiles();
    }
    
    private void scanSessionData(Path worldPath) {
        Path sessionLock = worldPath.resolve("session.lock");
        if (Files.exists(sessionLock)) {
            try {
                String content = Files.readString(sessionLock);
                if (content.trim().isEmpty()) {
                    issues.add(new CorruptionIssue(
                        CorruptionSeverity.LOW,
                        "Empty session lock file",
                        sessionLock.toString(),
                        "Session lock file exists but is empty"
                    ));
                }
            } catch (IOException e) {
                issues.add(new CorruptionIssue(
                    CorruptionSeverity.LOW,
                    "Cannot read session lock",
                    sessionLock.toString(),
                    e.getMessage()
                ));
            }
        }
        
        progress.incrementScannedFiles();
    }
    
    private void scanChecksums(Path worldPath) {
        Path checksumFile = worldPath.resolve("checksums.dat");
        if (!Files.exists(checksumFile)) {
            return; // Checksums are optional
        }
        
        try {
            Map<String, String> storedChecksums = loadChecksums(checksumFile);
            
            for (Map.Entry<String, String> entry : storedChecksums.entrySet()) {
                Path file = worldPath.resolve(entry.getKey());
                if (Files.exists(file)) {
                    String actualChecksum = calculateFileChecksum(file);
                    if (!actualChecksum.equals(entry.getValue())) {
                        issues.add(new CorruptionIssue(
                            CorruptionSeverity.HIGH,
                            "Checksum mismatch",
                            file.toString(),
                            "Expected: " + entry.getValue() + ", Actual: " + actualChecksum
                        ));
                    }
                }
            }
            
        } catch (IOException e) {
            issues.add(new CorruptionIssue(
                CorruptionSeverity.MEDIUM,
                "Cannot verify checksums",
                checksumFile.toString(),
                e.getMessage()
            ));
        }
    }
    
    // Private repair methods
    
    private boolean repairIssue(Path worldPath, CorruptionIssue issue, RepairOptions options) {
        switch (issue.type()) {
            case "Missing level.dat file" -> {
                return repairMissingLevelDat(worldPath);
            }
            case "Invalid level.dat magic number" -> {
                return repairLevelDatMagic(worldPath);
            }
            case "Invalid region file magic" -> {
                return repairRegionFileMagic(Paths.get(issue.filePath()));
            }
            case "Player file too small" -> {
                if (options.deleteCorruptedFiles()) {
                    return deleteCorruptedFile(Paths.get(issue.filePath()));
                }
            }
            case "Empty session lock file" -> {
                return repairSessionLock(worldPath);
            }
            default -> {
                logger.warn("No repair method for issue type: {}", issue.type());
                return false;
            }
        }
        return false;
    }
    
    private boolean repairMissingLevelDat(Path worldPath) {
        try {
            Path levelDat = worldPath.resolve("level.dat");
            try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(levelDat))) {
                dos.writeInt(WorldSaveFormat.LEVEL_MAGIC);
                dos.writeInt(WorldSaveFormat.CURRENT_VERSION);
                dos.writeLong(System.currentTimeMillis());
                dos.writeLong(new Random().nextLong()); // Random seed
                dos.writeUTF("Repaired World");
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to repair missing level.dat", e);
            return false;
        }
    }
    
    private boolean repairLevelDatMagic(Path worldPath) {
        // This would require more complex logic to preserve existing data
        // while fixing the magic number
        return false;
    }
    
    private boolean repairRegionFileMagic(Path regionFile) {
        // This would require complex logic to fix region file headers
        return false;
    }
    
    private boolean deleteCorruptedFile(Path file) {
        try {
            Files.deleteIfExists(file);
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete corrupted file: " + file, e);
            return false;
        }
    }
    
    private boolean repairSessionLock(Path worldPath) {
        try {
            Path sessionLock = worldPath.resolve("session.lock");
            Files.writeString(sessionLock, String.valueOf(System.currentTimeMillis()));
            return true;
        } catch (IOException e) {
            logger.error("Failed to repair session lock", e);
            return false;
        }
    }
    
    // Helper methods
    
    private void validateRegionStructure(Path regionsDir, List<ValidationIssue> issues, 
                                       ValidationProgress progress) {
        try {
            Files.walk(regionsDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String fileName = file.getFileName().toString();
                    if (!fileName.endsWith(".region")) {
                        issues.add(new ValidationIssue(
                            ValidationSeverity.LOW,
                            "Unexpected file in regions directory",
                            file.toString()
                        ));
                    }
                    progress.incrementValidatedFiles();
                });
        } catch (IOException e) {
            issues.add(new ValidationIssue(
                ValidationSeverity.MEDIUM,
                "Cannot validate region structure",
                regionsDir.toString()
            ));
        }
    }
    
    private void validatePlayerData(Path playersDir, List<ValidationIssue> issues, 
                                  ValidationProgress progress) {
        try {
            Files.walk(playersDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String fileName = file.getFileName().toString();
                    if (!fileName.endsWith(".dat")) {
                        issues.add(new ValidationIssue(
                            ValidationSeverity.LOW,
                            "Unexpected file in players directory",
                            file.toString()
                        ));
                    }
                    progress.incrementValidatedFiles();
                });
        } catch (IOException e) {
            issues.add(new ValidationIssue(
                ValidationSeverity.MEDIUM,
                "Cannot validate player data",
                playersDir.toString()
            ));
        }
    }
    
    private double calculateValidationScore(List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return 100.0;
        }
        
        int totalPenalty = issues.stream()
            .mapToInt(issue -> switch (issue.severity()) {
                case CRITICAL -> 50;
                case HIGH -> 20;
                case MEDIUM -> 10;
                case LOW -> 5;
            })
            .sum();
        
        return Math.max(0.0, 100.0 - totalPenalty);
    }
    
    private Map<String, String> loadChecksums(Path checksumFile) throws IOException {
        Map<String, String> checksums = new HashMap<>();
        List<String> lines = Files.readAllLines(checksumFile);
        
        for (String line : lines) {
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                checksums.put(parts[1], parts[0]);
            }
        }
        
        return checksums;
    }
    
    private String calculateFileChecksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file);
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }
    
    private long calculateDirectorySize(Path directory) throws IOException {
        return Files.walk(directory)
            .filter(Files::isRegularFile)
            .mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }
    
    private void copyDirectoryWithProgress(Path source, Path target, BackupProgress progress) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    progress.addProcessedBytes(Files.size(sourcePath));
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
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // CLI helper methods
    
    private static void printUsage() {
        System.out.println("Odyssey Corruption Scanner & Repair Tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -cp odyssey.jar com.odyssey.world.tools.CorruptionScanner <command> <world_path> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  scan <world_path>     - Scan world for corruption");
        System.out.println("  repair <world_path>   - Repair corruption issues");
        System.out.println("  validate <world_path> - Validate world integrity");
        System.out.println("  backup <world_path> [backup_path] - Create backup");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --level-data     - Scan level.dat file");
        System.out.println("  --region-files   - Scan region files");
        System.out.println("  --player-data    - Scan player data");
        System.out.println("  --session-data   - Scan session data");
        System.out.println("  --checksums      - Verify checksums");
        System.out.println("  --create-backup  - Create backup before repair");
        System.out.println("  --delete-corrupt - Delete corrupted files during repair");
    }
    
    private static ScanOptions parseScanOptions(String[] args) {
        Set<String> options = new HashSet<>(Arrays.asList(args));
        return new ScanOptions(
            options.contains("--level-data") || args.length == 2,
            options.contains("--region-files") || args.length == 2,
            options.contains("--player-data") || args.length == 2,
            options.contains("--session-data") || args.length == 2,
            options.contains("--checksums") || args.length == 2
        );
    }
    
    private static RepairOptions parseRepairOptions(String[] args) {
        Set<String> options = new HashSet<>(Arrays.asList(args));
        return new RepairOptions(
            options.contains("--create-backup"),
            options.contains("--delete-corrupt"),
            CorruptionSeverity.LOW
        );
    }
    
    private static void printScanResult(ScanResult result) {
        System.out.println("=== Scan Result ===");
        System.out.println("Success: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.println("Issues found: " + result.issues().size());
        System.out.println();
        
        if (!result.issues().isEmpty()) {
            System.out.println("Issues:");
            for (CorruptionIssue issue : result.issues()) {
                System.out.printf("[%s] %s: %s%n", 
                    issue.severity(), issue.type(), issue.description());
                System.out.printf("  File: %s%n", issue.filePath());
            }
        }
    }
    
    private static void printRepairResult(RepairResult result) {
        System.out.println("=== Repair Result ===");
        System.out.println("Success: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.println("Repaired: " + result.repairedCount());
        System.out.println("Failed: " + result.failedCount());
    }
    
    private static void printValidationResult(ValidationResult result) {
        System.out.println("=== Validation Result ===");
        System.out.println("Success: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.printf("Score: %.1f%%%n", result.score());
        System.out.println("Issues: " + result.issues().size());
    }
    
    private static void printBackupResult(BackupResult result) {
        System.out.println("=== Backup Result ===");
        System.out.println("Success: " + result.success());
        System.out.println("Message: " + result.message());
        System.out.println("Size: " + result.backupSize() + " bytes");
    }
    
    // Enums and record classes
    
    public enum CorruptionSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum ValidationSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public record ScanOptions(boolean scanLevelData, boolean scanRegionFiles, 
                             boolean scanPlayerData, boolean scanSessionData, boolean scanChecksums) {
        public static ScanOptions defaults() {
            return new ScanOptions(true, true, true, true, true);
        }
    }
    
    public record RepairOptions(boolean createBackup, boolean deleteCorruptedFiles, 
                               CorruptionSeverity minimumSeverity) {
        public static RepairOptions defaults() {
            return new RepairOptions(true, false, CorruptionSeverity.LOW);
        }
    }
    
    public record CorruptionIssue(CorruptionSeverity severity, String type, 
                                 String filePath, String description) {}
    
    public record ValidationIssue(ValidationSeverity severity, String description, String filePath) {}
    
    public record ScanResult(boolean success, String message, List<CorruptionIssue> issues) {}
    
    public record RepairResult(boolean success, String message, int repairedCount, int failedCount) {}
    
    public record ValidationResult(boolean success, String message, 
                                  List<ValidationIssue> issues, double score) {}
    
    public record BackupResult(boolean success, String message, long backupSize) {}
    
    // Progress tracking classes
    
    public static class ScanProgress {
        private long totalFiles = 0;
        private long scannedFiles = 0;
        
        public void setTotalFiles(long total) { this.totalFiles = total; }
        public void incrementScannedFiles() { this.scannedFiles++; }
        public void reset() { this.totalFiles = 0; this.scannedFiles = 0; }
        
        public double getProgressPercentage() {
            return totalFiles > 0 ? (double) scannedFiles / totalFiles * 100.0 : 0.0;
        }
        
        public long getTotalFiles() { return totalFiles; }
        public long getScannedFiles() { return scannedFiles; }
    }
    
    public static class ValidationProgress {
        private int validatedFiles = 0;
        
        public void incrementValidatedFiles() { this.validatedFiles++; }
        public int getValidatedFiles() { return validatedFiles; }
    }
    
    public static class BackupProgress {
        private long totalBytes = 0;
        private long processedBytes = 0;
        
        public void setTotalBytes(long total) { this.totalBytes = total; }
        public void addProcessedBytes(long bytes) { this.processedBytes += bytes; }
        
        public double getProgressPercentage() {
            return totalBytes > 0 ? (double) processedBytes / totalBytes * 100.0 : 0.0;
        }
        
        public long getTotalBytes() { return totalBytes; }
        public long getProcessedBytes() { return processedBytes; }
    }
}