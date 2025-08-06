package com.odyssey.graphics;

import com.odyssey.core.jobs.JobSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Content Pipeline Integration for The Odyssey.
 * Handles asset scanning, texture validation, and automatic atlas rebuilding
 * when new content is added to resource packs.
 */
public class ContentPipeline {
    private static final Logger logger = LoggerFactory.getLogger(ContentPipeline.class);
    
    // Supported texture formats
    private static final Set<String> SUPPORTED_FORMATS = Set.of("png", "jpg", "jpeg", "bmp", "gif");
    
    // Supported texture resolutions
    private static final Set<Integer> SUPPORTED_RESOLUTIONS = Set.of(16, 32, 64, 128, 256, 512, 1024);
    
    private final AssetManager assetManager;
    private final TextureAtlasManager atlasManager;
    private final JobSystem jobSystem;
    
    // File watching
    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    
    // Asset tracking
    private final Map<String, AssetInfo> discoveredAssets = new ConcurrentHashMap<>();
    private final Set<String> resourcePackPaths = ConcurrentHashMap.newKeySet();
    
    // Validation results
    private final Map<String, ValidationResult> validationResults = new ConcurrentHashMap<>();
    
    /**
     * Information about a discovered asset.
     */
    public static class AssetInfo {
        private final String path;
        private final String name;
        private final AssetType type;
        private final long size;
        private final long lastModified;
        private final int width;
        private final int height;
        
        public AssetInfo(String path, String name, AssetType type, long size, 
                        long lastModified, int width, int height) {
            this.path = path;
            this.name = name;
            this.type = type;
            this.size = size;
            this.lastModified = lastModified;
            this.width = width;
            this.height = height;
        }
        
        // Getters
        public String getPath() { return path; }
        public String getName() { return name; }
        public AssetType getType() { return type; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
    
    /**
     * Asset type enumeration.
     */
    public enum AssetType {
        TEXTURE_DIFFUSE,
        TEXTURE_NORMAL,
        TEXTURE_SPECULAR,
        TEXTURE_EMISSION,
        TEXTURE_ANIMATED,
        UNKNOWN
    }
    
    /**
     * Validation result for an asset.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
    }
    
    /**
     * Creates a new ContentPipeline.
     *
     * @param assetManager Asset manager for loading assets
     * @param atlasManager Texture atlas manager for rebuilding atlases
     * @param jobSystem Job system for async operations
     */
    public ContentPipeline(AssetManager assetManager, TextureAtlasManager atlasManager, 
                          JobSystem jobSystem) {
        this.assetManager = assetManager;
        this.atlasManager = atlasManager;
        this.jobSystem = jobSystem;
        
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.error("Failed to create file watch service", e);
        }
        
        logger.info("ContentPipeline initialized");
    }
    
    /**
     * Adds a resource pack directory to be monitored.
     *
     * @param resourcePackPath Path to the resource pack directory
     * @return CompletableFuture that completes when scanning is done
     */
    public CompletableFuture<Void> addResourcePack(String resourcePackPath) {
        resourcePackPaths.add(resourcePackPath);
        
        return CompletableFuture.runAsync(() -> {
            try {
                scanResourcePack(resourcePackPath);
                startWatching(resourcePackPath);
            } catch (IOException e) {
                logger.error("Failed to add resource pack: {}", resourcePackPath, e);
                throw new RuntimeException(e);
            }
        }, jobSystem.getExecutor());
    }
    
    /**
     * Scans a resource pack directory for assets.
     *
     * @param resourcePackPath Path to scan
     * @throws IOException If scanning fails
     */
    private void scanResourcePack(String resourcePackPath) throws IOException {
        Path packPath = Paths.get(resourcePackPath);
        if (!Files.exists(packPath)) {
            logger.warn("Resource pack path does not exist: {}", resourcePackPath);
            return;
        }
        
        logger.info("Scanning resource pack: {}", resourcePackPath);
        
        Files.walk(packPath)
                .filter(Files::isRegularFile)
                .filter(this::isTextureFile)
                .forEach(this::processTextureFile);
        
        logger.info("Completed scanning resource pack: {}", resourcePackPath);
    }
    
    /**
     * Checks if a file is a supported texture file.
     *
     * @param path File path to check
     * @return true if it's a supported texture file
     */
    private boolean isTextureFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return SUPPORTED_FORMATS.stream().anyMatch(fileName::endsWith);
    }
    
    /**
     * Processes a discovered texture file.
     *
     * @param texturePath Path to the texture file
     */
    private void processTextureFile(Path texturePath) {
        try {
            String relativePath = getRelativePath(texturePath);
            String fileName = texturePath.getFileName().toString();
            
            // Get file info
            long size = Files.size(texturePath);
            long lastModified = Files.getLastModifiedTime(texturePath).toMillis();
            
            // Load and analyze image
            byte[] imageData = Files.readAllBytes(texturePath);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            if (image == null) {
                logger.warn("Failed to read image: {}", texturePath);
                return;
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Determine asset type
            AssetType type = determineAssetType(fileName);
            
            // Create asset info
            AssetInfo assetInfo = new AssetInfo(relativePath, fileName, type, size, 
                                              lastModified, width, height);
            
            discoveredAssets.put(relativePath, assetInfo);
            
            // Validate the texture
            ValidationResult validation = validateTexture(assetInfo, image);
            validationResults.put(relativePath, validation);
            
            if (validation.isValid()) {
                logger.debug("Discovered valid texture: {} ({}x{})", relativePath, width, height);
            } else {
                logger.warn("Invalid texture discovered: {} - Errors: {}", 
                           relativePath, validation.getErrors());
            }
            
        } catch (IOException e) {
            logger.error("Failed to process texture file: {}", texturePath, e);
        }
    }
    
    /**
     * Gets relative path from absolute path.
     *
     * @param absolutePath Absolute file path
     * @return Relative path string
     */
    private String getRelativePath(Path absolutePath) {
        // Find the resource pack root and return relative path
        for (String packPath : resourcePackPaths) {
            Path packRoot = Paths.get(packPath);
            if (absolutePath.startsWith(packRoot)) {
                return packRoot.relativize(absolutePath).toString().replace('\\', '/');
            }
        }
        return absolutePath.toString().replace('\\', '/');
    }
    
    /**
     * Determines the asset type based on filename.
     *
     * @param fileName File name to analyze
     * @return Determined asset type
     */
    private AssetType determineAssetType(String fileName) {
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.contains("normal") || lowerName.contains("_n.")) {
            return AssetType.TEXTURE_NORMAL;
        } else if (lowerName.contains("specular") || lowerName.contains("_s.")) {
            return AssetType.TEXTURE_SPECULAR;
        } else if (lowerName.contains("emission") || lowerName.contains("emissive") || 
                   lowerName.contains("_e.")) {
            return AssetType.TEXTURE_EMISSION;
        } else if (lowerName.contains("anim") || lowerName.contains("animated")) {
            return AssetType.TEXTURE_ANIMATED;
        } else {
            return AssetType.TEXTURE_DIFFUSE;
        }
    }
    
    /**
     * Validates a texture asset.
     *
     * @param assetInfo Asset information
     * @param image Loaded image
     * @return Validation result
     */
    private ValidationResult validateTexture(AssetInfo assetInfo, BufferedImage image) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Check if dimensions are power of 2
        if (!isPowerOfTwo(width) || !isPowerOfTwo(height)) {
            warnings.add("Texture dimensions are not power of 2: " + width + "x" + height);
        }
        
        // Check if dimensions are supported
        if (!SUPPORTED_RESOLUTIONS.contains(width) || !SUPPORTED_RESOLUTIONS.contains(height)) {
            warnings.add("Unusual texture resolution: " + width + "x" + height);
        }
        
        // Check if texture is square (recommended for atlasing)
        if (width != height) {
            warnings.add("Non-square texture may not pack efficiently: " + width + "x" + height);
        }
        
        // Check file size
        if (assetInfo.getSize() > 10 * 1024 * 1024) { // 10MB
            warnings.add("Large texture file size: " + (assetInfo.getSize() / 1024 / 1024) + "MB");
        }
        
        // Check for transparency in non-diffuse textures
        if (assetInfo.getType() != AssetType.TEXTURE_DIFFUSE && hasTransparency(image)) {
            warnings.add("Non-diffuse texture has transparency channel");
        }
        
        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Checks if a number is a power of 2.
     *
     * @param n Number to check
     * @return true if power of 2
     */
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Checks if an image has transparency.
     *
     * @param image Image to check
     * @return true if image has transparency
     */
    private boolean hasTransparency(BufferedImage image) {
        return image.getColorModel().hasAlpha();
    }
    
    /**
     * Starts watching a directory for file changes.
     *
     * @param directoryPath Directory to watch
     * @throws IOException If watching fails
     */
    private void startWatching(String directoryPath) throws IOException {
        if (watchService == null) {
            return;
        }
        
        Path dir = Paths.get(directoryPath);
        WatchKey key = dir.register(watchService, 
                                   StandardWatchEventKinds.ENTRY_CREATE,
                                   StandardWatchEventKinds.ENTRY_MODIFY,
                                   StandardWatchEventKinds.ENTRY_DELETE);
        
        watchKeys.put(key, dir);
        
        if (!isWatching.getAndSet(true)) {
            startWatchThread();
        }
    }
    
    /**
     * Starts the file watching thread.
     */
    private void startWatchThread() {
        Thread watchThread = new Thread(() -> {
            while (isWatching.get()) {
                try {
                    WatchKey key = watchService.take();
                    Path dir = watchKeys.get(key);
                    
                    if (dir == null) {
                        continue;
                    }
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        Path fullPath = dir.resolve(filename);
                        
                        if (isTextureFile(fullPath)) {
                            handleFileChange(kind, fullPath);
                        }
                    }
                    
                    boolean valid = key.reset();
                    if (!valid) {
                        watchKeys.remove(key);
                        if (watchKeys.isEmpty()) {
                            break;
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in file watching", e);
                }
            }
        }, "ContentPipeline-FileWatcher");
        
        watchThread.setDaemon(true);
        watchThread.start();
    }
    
    /**
     * Handles a file change event.
     *
     * @param kind Type of change
     * @param filePath Path to changed file
     */
    private void handleFileChange(WatchEvent.Kind<?> kind, Path filePath) {
        String relativePath = getRelativePath(filePath);
        
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            discoveredAssets.remove(relativePath);
            validationResults.remove(relativePath);
            logger.info("Texture deleted: {}", relativePath);
            
            // Trigger atlas rebuild
            scheduleAtlasRebuild();
            
        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE || 
                   kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            
            // Process the new/modified file
            processTextureFile(filePath);
            logger.info("Texture {}: {}", 
                       kind == StandardWatchEventKinds.ENTRY_CREATE ? "created" : "modified", 
                       relativePath);
            
            // Trigger atlas rebuild
            scheduleAtlasRebuild();
        }
    }
    
    /**
     * Schedules an atlas rebuild operation.
     */
    private void scheduleAtlasRebuild() {
        // Use job system to rebuild atlases asynchronously
        jobSystem.submit(new com.odyssey.core.jobs.Job() {
            @Override
            public void execute() throws Exception {
                rebuildAtlases();
            }
            
            @Override
            public int getPriority() {
                return 2; // Medium priority
            }
            
            @Override
            public String getDescription() {
                return "Rebuild texture atlases";
            }
            
            @Override
            public boolean isCancellable() {
                return true;
            }
        }, () -> {
            rebuildAtlases();
            return null;
        });
    }
    
    /**
     * Rebuilds texture atlases with new content.
     */
    private void rebuildAtlases() {
        logger.info("Rebuilding texture atlases with new content");
        
        // Get all valid textures
        List<AssetInfo> validTextures = discoveredAssets.values().stream()
                .filter(asset -> {
                    ValidationResult validation = validationResults.get(asset.getPath());
                    return validation != null && validation.isValid();
                })
                .toList();
        
        // Group textures by type
        Map<AssetType, List<AssetInfo>> texturesByType = new HashMap<>();
        for (AssetInfo asset : validTextures) {
            texturesByType.computeIfAbsent(asset.getType(), k -> new ArrayList<>()).add(asset);
        }
        
        // Rebuild atlases for each type
        for (Map.Entry<AssetType, List<AssetInfo>> entry : texturesByType.entrySet()) {
            AssetType type = entry.getKey();
            List<AssetInfo> textures = entry.getValue();
            
            try {
                rebuildAtlasForType(type, textures);
            } catch (Exception e) {
                logger.error("Failed to rebuild atlas for type: {}", type, e);
            }
        }
        
        logger.info("Atlas rebuild completed");
    }
    
    /**
     * Rebuilds atlas for a specific texture type.
     *
     * @param type Texture type
     * @param textures List of textures of this type
     */
    private void rebuildAtlasForType(AssetType type, List<AssetInfo> textures) {
        // This would integrate with the existing TextureAtlasManager
        // For now, we'll log the operation
        logger.info("Rebuilding atlas for type {} with {} textures", type, textures.size());
        
        // TODO: Integrate with TextureAtlasManager to actually rebuild atlases
        // This would involve:
        // 1. Creating new atlas for the type
        // 2. Loading texture data for each asset
        // 3. Adding textures to the atlas
        // 4. Updating texture references
    }
    
    /**
     * Gets all discovered assets.
     *
     * @return Map of asset path to asset info
     */
    public Map<String, AssetInfo> getDiscoveredAssets() {
        return new HashMap<>(discoveredAssets);
    }
    
    /**
     * Gets validation results for all assets.
     *
     * @return Map of asset path to validation result
     */
    public Map<String, ValidationResult> getValidationResults() {
        return new HashMap<>(validationResults);
    }
    
    /**
     * Gets assets that failed validation.
     *
     * @return List of invalid asset paths
     */
    public List<String> getInvalidAssets() {
        return validationResults.entrySet().stream()
                .filter(entry -> !entry.getValue().isValid())
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * Generates a content pipeline report.
     *
     * @return Report string
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Content Pipeline Report\n");
        report.append("======================\n\n");
        
        report.append("Resource Packs: ").append(resourcePackPaths.size()).append("\n");
        report.append("Total Assets: ").append(discoveredAssets.size()).append("\n");
        
        // Count by type
        Map<AssetType, Long> countsByType = discoveredAssets.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AssetInfo::getType, 
                        java.util.stream.Collectors.counting()));
        
        report.append("\nAssets by Type:\n");
        for (Map.Entry<AssetType, Long> entry : countsByType.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        // Validation summary
        long validCount = validationResults.values().stream()
                .mapToLong(result -> result.isValid() ? 1 : 0)
                .sum();
        long invalidCount = validationResults.size() - validCount;
        
        report.append("\nValidation Summary:\n");
        report.append("  Valid: ").append(validCount).append("\n");
        report.append("  Invalid: ").append(invalidCount).append("\n");
        
        if (invalidCount > 0) {
            report.append("\nInvalid Assets:\n");
            getInvalidAssets().forEach(path -> {
                ValidationResult result = validationResults.get(path);
                report.append("  ").append(path).append(": ").append(result.getErrors()).append("\n");
            });
        }
        
        return report.toString();
    }
    
    /**
     * Shuts down the content pipeline.
     */
    public void shutdown() {
        isWatching.set(false);
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Failed to close watch service", e);
            }
        }
        
        discoveredAssets.clear();
        validationResults.clear();
        watchKeys.clear();
        
        logger.info("ContentPipeline shutdown completed");
    }
}