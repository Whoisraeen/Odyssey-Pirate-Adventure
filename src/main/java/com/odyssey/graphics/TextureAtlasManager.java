package com.odyssey.graphics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages multiple texture atlases for different texture categories.
 * Automatically creates new atlases when existing ones are full.
 * Supports streaming texture loading for large atlases.
 */
public class TextureAtlasManager {
    private static final Logger logger = LoggerFactory.getLogger(TextureAtlasManager.class);
    
    /**
     * Priority levels for texture streaming.
     */
    public enum StreamingPriority {
        CRITICAL(0),    // Player immediate vicinity
        HIGH(1),        // Player view distance
        MEDIUM(2),      // Background loading
        LOW(3),         // Distant chunks
        BACKGROUND(4);  // Preloading
        
        private final int level;
        
        StreamingPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Represents a texture request for streaming.
     */
    public static class TextureRequest {
        private final String name;
        private final String texturePath;
        private final AtlasCategory category;
        private final StreamingPriority priority;
        private final Consumer<AtlasTextureReference> callback;
        private final long requestTime;
        
        public TextureRequest(String name, String texturePath, AtlasCategory category, 
                            StreamingPriority priority, Consumer<AtlasTextureReference> callback) {
            this.name = name;
            this.texturePath = texturePath;
            this.category = category;
            this.priority = priority;
            this.callback = callback;
            this.requestTime = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public String getTexturePath() { return texturePath; }
        public AtlasCategory getCategory() { return category; }
        public StreamingPriority getPriority() { return priority; }
        public Consumer<AtlasTextureReference> getCallback() { return callback; }
        public long getRequestTime() { return requestTime; }
    }
    
    /**
     * Different categories of textures for organization.
     */
    public enum AtlasCategory {
        BLOCKS("blocks", 2048, TextureAtlas.PackingAlgorithm.SHELF),      // Similar-sized block textures
        ENTITIES("entities", 1024, TextureAtlas.PackingAlgorithm.GUILLOTINE), // Varied entity textures
        UI("ui", 1024, TextureAtlas.PackingAlgorithm.GUILLOTINE),         // Mixed UI element sizes
        EFFECTS("effects", 512, TextureAtlas.PackingAlgorithm.GUILLOTINE), // Particle/effect textures
        TERRAIN("terrain", 2048, TextureAtlas.PackingAlgorithm.SHELF),    // Similar terrain textures
        // PBR Material Categories
        PBR_ALBEDO("pbr_albedo", 2048, TextureAtlas.PackingAlgorithm.SHELF),     // PBR albedo textures
        PBR_NORMAL("pbr_normal", 2048, TextureAtlas.PackingAlgorithm.SHELF),     // PBR normal maps
        PBR_METALLIC("pbr_metallic", 1024, TextureAtlas.PackingAlgorithm.SHELF), // PBR metallic maps
        PBR_ROUGHNESS("pbr_roughness", 1024, TextureAtlas.PackingAlgorithm.SHELF), // PBR roughness maps
        PBR_AO("pbr_ao", 1024, TextureAtlas.PackingAlgorithm.SHELF),             // PBR ambient occlusion maps
        PBR_EMISSION("pbr_emission", 1024, TextureAtlas.PackingAlgorithm.SHELF); // PBR emission maps
        
        private final String name;
        private final int defaultSize;
        private final TextureAtlas.PackingAlgorithm packingAlgorithm;
        
        AtlasCategory(String name, int defaultSize, TextureAtlas.PackingAlgorithm packingAlgorithm) {
            this.name = name;
            this.defaultSize = defaultSize;
            this.packingAlgorithm = packingAlgorithm;
        }
        
        public String getName() {
            return name;
        }
        
        public int getDefaultSize() {
            return defaultSize;
        }
        
        public TextureAtlas.PackingAlgorithm getPackingAlgorithm() {
            return packingAlgorithm;
        }
    }
    
    /**
     * Reference to a texture in an atlas with category information.
     */
    public static class AtlasTextureReference {
        private final TextureAtlas atlas;
        private final TextureAtlas.AtlasRegion region;
        private final AtlasCategory category;
        
        public AtlasTextureReference(TextureAtlas atlas, TextureAtlas.AtlasRegion region, AtlasCategory category) {
            this.atlas = atlas;
            this.region = region;
            this.category = category;
        }
        
        public TextureAtlas getAtlas() {
            return atlas;
        }
        
        public TextureAtlas.AtlasRegion getRegion() {
            return region;
        }
        
        public AtlasCategory getCategory() {
            return category;
        }
    }
    
    private final Map<AtlasCategory, List<TextureAtlas>> atlases;
    private final Map<String, AtlasTextureReference> textureReferences;
    private final Map<String, CompletableFuture<AtlasTextureReference>> pendingRequests;
    private final StreamingTextureManager streamingManager;
    private final AtomicLong totalAtlasMemory;
    private final long maxAtlasMemoryMB;
    
    /**
     * Creates a new texture atlas manager.
     * 
     * @param streamingManager The streaming texture manager for loading textures
     * @param maxAtlasMemoryMB Maximum memory for atlases in megabytes
     */
    public TextureAtlasManager(StreamingTextureManager streamingManager, long maxAtlasMemoryMB) {
        this.atlases = new HashMap<>();
        this.textureReferences = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.streamingManager = streamingManager;
        this.totalAtlasMemory = new AtomicLong(0);
        this.maxAtlasMemoryMB = maxAtlasMemoryMB;
        
        // Initialize atlas lists for each category
        for (AtlasCategory category : AtlasCategory.values()) {
            atlases.put(category, new ArrayList<>());
        }
        
        logger.info("Initialized texture atlas manager with {}MB memory limit", maxAtlasMemoryMB);
    }
    
    /**
     * Creates a new texture atlas manager with default memory limit.
     * 
     * @param streamingManager The streaming texture manager for loading textures
     */
    public TextureAtlasManager(StreamingTextureManager streamingManager) {
        this(streamingManager, 512); // Default 512MB for atlases
    }
    
    /**
     * Requests a texture to be loaded asynchronously into an atlas.
     * 
     * @param name The unique name for the texture
     * @param texturePath Path to the texture file
     * @param category The category to place the texture in
     * @param priority The loading priority
     * @param callback Callback when texture is loaded (optional)
     * @return CompletableFuture that completes when texture is loaded
     */
    public CompletableFuture<AtlasTextureReference> requestTextureAsync(String name, String texturePath, 
                                                                       AtlasCategory category, StreamingPriority priority,
                                                                       Consumer<AtlasTextureReference> callback) {
        // Check if already loaded
        AtlasTextureReference existing = textureReferences.get(name);
        if (existing != null) {
            if (callback != null) {
                callback.accept(existing);
            }
            return CompletableFuture.completedFuture(existing);
        }
        
        // Check if already being loaded
        CompletableFuture<AtlasTextureReference> pending = pendingRequests.get(name);
        if (pending != null) {
            if (callback != null) {
                pending.thenAccept(callback);
            }
            return pending;
        }
        
        // Convert streaming priority to asset priority
        AssetManager.AssetPriority assetPriority = convertPriority(priority);
        
        // Create texture config
        StreamingTextureManager.TextureConfig config = new StreamingTextureManager.TextureConfig()
                .priority(assetPriority)
                .quality(0.9f)  // High quality
                .generateMipmaps(true);
        
        // Load texture asynchronously using direct file loading for atlas
        CompletableFuture<AtlasTextureReference> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Load and add texture to atlas directly
                AtlasTextureReference reference = loadAndAddTextureToAtlas(name, texturePath, category);
                
                // Update memory tracking
                long atlasMemory = calculateAtlasMemoryUsage();
                totalAtlasMemory.set(atlasMemory);
                
                // Check memory limits
                if (atlasMemory > maxAtlasMemoryMB * 1024L * 1024L) {
                    evictLeastUsedAtlases();
                }
                
                return reference;
            } catch (Exception e) {
                logger.error("Failed to add texture to atlas: " + name + " - " + e.getMessage());
                throw new RuntimeException("Atlas addition failed", e);
            }
        })
                .whenComplete((result, throwable) -> {
                    pendingRequests.remove(name);
                    if (callback != null && result != null) {
                        callback.accept(result);
                    }
                });
        
        pendingRequests.put(name, future);
        return future;
    }
    
    /**
     * Requests a texture to be loaded synchronously into an atlas.
     * 
     * @param name The unique name for the texture
     * @param texturePath Path to the texture file
     * @param category The category to place the texture in
     * @param priority The loading priority
     * @return The atlas texture reference, or null if failed
     */
    public AtlasTextureReference requestTextureSync(String name, String texturePath, 
                                                   AtlasCategory category, StreamingPriority priority) {
        // Check if already loaded
        AtlasTextureReference existing = textureReferences.get(name);
        if (existing != null) {
            return existing;
        }
        
        try {
            // Load and add texture to atlas directly
            AtlasTextureReference reference = loadAndAddTextureToAtlas(name, texturePath, category);
            
            // Update memory tracking
            long atlasMemory = calculateAtlasMemoryUsage();
            totalAtlasMemory.set(atlasMemory);
            
            // Check memory limits
            if (atlasMemory > maxAtlasMemoryMB * 1024L * 1024L) {
                evictLeastUsedAtlases();
            }
            
            return reference;
        } catch (Exception e) {
            logger.error("Failed to load texture synchronously: " + name + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Adds a texture to the appropriate atlas category.
     * @param name The unique name for the texture
     * @param textureData The texture data as ByteBuffer
     * @param width The width of the texture
     * @param height The height of the texture
     * @param category The category to place the texture in
     * @return The atlas texture reference, or null if the texture couldn't be added
     */
    public AtlasTextureReference addTexture(String name, ByteBuffer textureData, int width, int height, AtlasCategory category) {
        if (textureReferences.containsKey(name)) {
            logger.warn("Texture with name '" + name + "' already exists");
            return textureReferences.get(name);
        }
        
        List<TextureAtlas> categoryAtlases = atlases.get(category);
        
        // Try to add to existing atlases
        for (TextureAtlas atlas : categoryAtlases) {
            TextureAtlas.AtlasRegion region = atlas.addTexture(name, textureData, width, height);
            if (region != null) {
                AtlasTextureReference reference = new AtlasTextureReference(atlas, region, category);
                textureReferences.put(name, reference);
                return reference;
            }
        }
        
        // Create new atlas if none can fit the texture
        TextureAtlas newAtlas = new TextureAtlas(category.getDefaultSize(), category.getPackingAlgorithm());
        categoryAtlases.add(newAtlas);
        
        TextureAtlas.AtlasRegion region = newAtlas.addTexture(name, textureData, width, height);
        if (region != null) {
            AtlasTextureReference reference = new AtlasTextureReference(newAtlas, region, category);
            textureReferences.put(name, reference);
            logger.info("Created new atlas for category '" + category.getName() + "' (total: " + categoryAtlases.size() + ")");
            return reference;
        }
        
        logger.error("Failed to add texture '" + name + "' even to new atlas");
        return null;
    }
    
    /**
     * Gets a texture reference by name.
     * @param name The name of the texture
     * @return The atlas texture reference, or null if not found
     */
    public AtlasTextureReference getTexture(String name) {
        return textureReferences.get(name);
    }
    
    /**
     * Checks if a texture exists.
     * @param name The name of the texture
     * @return True if the texture exists, false otherwise
     */
    public boolean hasTexture(String name) {
        return textureReferences.containsKey(name);
    }
    
    /**
     * Gets all atlases for a specific category.
     * @param category The atlas category
     * @return List of atlases for the category
     */
    public List<TextureAtlas> getAtlases(AtlasCategory category) {
        return new ArrayList<>(atlases.get(category));
    }
    
    /**
     * Gets the number of atlases for a specific category.
     * @param category The atlas category
     * @return The number of atlases
     */
    public int getAtlasCount(AtlasCategory category) {
        return atlases.get(category).size();
    }
    
    /**
     * Gets the total number of textures managed.
     * @return The total texture count
     */
    public int getTotalTextureCount() {
        return textureReferences.size();
    }
    
    /**
     * Gets statistics about atlas usage.
     * @return A formatted string with atlas statistics
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Texture Atlas Manager Statistics:\n");
        stats.append("Total textures: ").append(getTotalTextureCount()).append("\n\n");
        
        for (AtlasCategory category : AtlasCategory.values()) {
            List<TextureAtlas> categoryAtlases = atlases.get(category);
            stats.append("Category: ").append(category.getName().toUpperCase()).append("\n");
            stats.append("  Algorithm: ").append(category.getPackingAlgorithm()).append("\n");
            stats.append("  Atlases: ").append(categoryAtlases.size()).append("\n");
            
            int totalRegions = categoryAtlases.stream()
                    .mapToInt(TextureAtlas::getRegionCount)
                    .sum();
            stats.append("  Textures: ").append(totalRegions).append("\n");
            
            if (!categoryAtlases.isEmpty()) {
                float avgUtilization = (float) categoryAtlases.stream()
                        .mapToDouble(TextureAtlas::getSpaceUtilization)
                        .average()
                        .orElse(0.0);
                stats.append("  Avg Space Utilization: ").append(String.format("%.2f%%", avgUtilization * 100)).append("\n");
            }
            stats.append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Preloads common textures into atlases.
     * This method should be called during initialization to set up default textures.
     */
    public void preloadDefaultTextures() {
        // Create placeholder textures for missing assets
        createPlaceholderTexture("missing_texture", AtlasCategory.BLOCKS);
        createPlaceholderTexture("missing_entity", AtlasCategory.ENTITIES);
        createPlaceholderTexture("missing_ui", AtlasCategory.UI);
        
        logger.info("Preloaded default placeholder textures");
    }
    
    /**
     * Creates a placeholder texture with a distinctive pattern.
     * @param name The name for the placeholder texture
     * @param category The category to place it in
     */
    private void createPlaceholderTexture(String name, AtlasCategory category) {
        int size = 16; // 16x16 placeholder
        ByteBuffer textureData = ByteBuffer.allocateDirect(size * size * 4);
        
        // Create a magenta and black checkerboard pattern
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
                if (checker) {
                    // Magenta
                    textureData.put((byte) 255); // R
                    textureData.put((byte) 0);   // G
                    textureData.put((byte) 255); // B
                    textureData.put((byte) 255); // A
                } else {
                    // Black
                    textureData.put((byte) 0);   // R
                    textureData.put((byte) 0);   // G
                    textureData.put((byte) 0);   // B
                    textureData.put((byte) 255); // A
                }
            }
        }
        
        textureData.flip();
        addTexture(name, textureData, size, size, category);
    }
    
    /**
     * Converts streaming priority to asset manager priority.
     * 
     * @param streamingPriority The streaming priority
     * @return The corresponding asset priority
     */
    private AssetManager.AssetPriority convertPriority(StreamingPriority streamingPriority) {
        switch (streamingPriority) {
            case CRITICAL: return AssetManager.AssetPriority.CRITICAL;
            case HIGH: return AssetManager.AssetPriority.HIGH;
            case MEDIUM: return AssetManager.AssetPriority.MEDIUM;
            case LOW: return AssetManager.AssetPriority.LOW;
            case BACKGROUND: return AssetManager.AssetPriority.BACKGROUND;
            default: return AssetManager.AssetPriority.MEDIUM;
        }
    }
    
    /**
     * Loads texture data from file and adds it to an atlas.
     * 
     * @param name The unique name for the texture
     * @param texturePath Path to the texture file
     * @param category The category to place the texture in
     * @return The atlas texture reference, or null if failed
     */
    private AtlasTextureReference loadAndAddTextureToAtlas(String name, String texturePath, AtlasCategory category) {
        if (textureReferences.containsKey(name)) {
            logger.warn("Texture with name '" + name + "' already exists");
            return textureReferences.get(name);
        }
        
        try {
            // Load texture data directly from file for atlas use
            ByteBuffer textureData = loadTextureDataFromFile(texturePath);
            if (textureData == null) {
                logger.error("Failed to load texture data from: " + texturePath);
                return null;
            }
            
            // For now, assume standard texture dimensions - this should be improved
            // to read actual dimensions from the file
            int width = 256; // Default size - should be read from file
            int height = 256;
            
            return addTexture(name, textureData, width, height, category);
        } catch (Exception e) {
            logger.error("Failed to load and add texture to atlas: " + name + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Loads raw texture data from a file for atlas use.
     * This is a simplified implementation that should be expanded.
     * 
     * @param texturePath Path to the texture file
     * @return ByteBuffer containing RGBA texture data, or null if failed
     */
    private ByteBuffer loadTextureDataFromFile(String texturePath) {
        // This is a placeholder implementation
        // In a real implementation, you would use STBImage or similar to load the raw data
        logger.warn("loadTextureDataFromFile is not fully implemented for: " + texturePath);
        return null;
    }
    
    /**
     * Calculates total memory usage of all atlases.
     * 
     * @return Total memory usage in bytes
     */
    private long calculateAtlasMemoryUsage() {
        long totalMemory = 0;
        for (List<TextureAtlas> categoryAtlases : atlases.values()) {
            for (TextureAtlas atlas : categoryAtlases) {
                totalMemory += atlas.getMemoryUsage();
            }
        }
        return totalMemory;
    }
    
    /**
     * Evicts least used atlases to free memory.
     */
    private void evictLeastUsedAtlases() {
        logger.info("Atlas memory limit exceeded, evicting least used atlases");
        
        // Simple eviction strategy: remove atlases with lowest utilization
        for (AtlasCategory category : AtlasCategory.values()) {
            List<TextureAtlas> categoryAtlases = atlases.get(category);
            categoryAtlases.removeIf(atlas -> {
                if (atlas.getSpaceUtilization() < 0.3f) { // Less than 30% utilized
                    // Remove texture references for this atlas
                    textureReferences.entrySet().removeIf(entry -> 
                        entry.getValue().getAtlas() == atlas);
                    
                    atlas.cleanup();
                    logger.debug("Evicted atlas from category: " + category.getName());
                    return true;
                }
                return false;
            });
        }
        
        // Update memory tracking
        totalAtlasMemory.set(calculateAtlasMemoryUsage());
    }
    
    /**
     * Preloads textures for a specific area or distance.
     * 
     * @param textureRequests List of texture requests to preload
     */
    public void preloadTextures(List<TextureRequest> textureRequests) {
        // Sort by priority
        textureRequests.sort((r1, r2) -> 
            Integer.compare(r1.getPriority().getLevel(), r2.getPriority().getLevel()));
        
        for (TextureRequest request : textureRequests) {
            requestTextureAsync(request.getName(), request.getTexturePath(), 
                              request.getCategory(), request.getPriority(), 
                              request.getCallback());
        }
        
        logger.info("Preloading {} textures", textureRequests.size());
    }
    
    /**
     * Gets streaming statistics.
     * 
     * @return Formatted statistics string
     */
    public String getStreamingStatistics() {
        long memoryMB = totalAtlasMemory.get() / (1024 * 1024);
        int pendingCount = pendingRequests.size();
        
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("Atlas Streaming: %dMB/%dMB memory (%.1f%%), %d pending requests\n",
                memoryMB, maxAtlasMemoryMB, (double) memoryMB / maxAtlasMemoryMB * 100, pendingCount));
        
        stats.append(getStatistics());
        
        if (streamingManager != null) {
            stats.append("\n").append(streamingManager.getStatistics());
        }
        
        return stats.toString();
    }
    
    /**
     * Performs atlas defragmentation for a specific category.
     * Consolidates textures from multiple fragmented atlases into fewer, more efficient atlases.
     * This is useful for long-running games where atlases may become fragmented over time.
     * 
     * @param category The category to defragment
     * @return True if defragmentation was performed, false if not needed
     */
    public boolean defragmentAtlases(AtlasCategory category) {
        List<TextureAtlas> categoryAtlases = atlases.get(category);
        
        if (categoryAtlases.size() <= 1) {
            return false; // No need to defragment single or no atlases
        }
        
        // Calculate average utilization
        double avgUtilization = categoryAtlases.stream()
                .mapToDouble(TextureAtlas::getSpaceUtilization)
                .average()
                .orElse(1.0);
        
        // Only defragment if average utilization is below threshold
        if (avgUtilization > 0.7) {
            return false; // Atlases are well utilized
        }
        
        logger.info("Starting defragmentation for category: {} (avg utilization: {:.2f}%)", 
                   category.getName(), avgUtilization * 100);
        
        // Collect all textures from existing atlases
        Map<String, TextureData> texturesToRepack = new HashMap<>();
        
        for (TextureAtlas atlas : categoryAtlases) {
            for (Map.Entry<String, AtlasTextureReference> entry : textureReferences.entrySet()) {
                AtlasTextureReference ref = entry.getValue();
                if (ref.getAtlas() == atlas && ref.getCategory() == category) {
                    // Extract texture data for repacking
                    TextureData textureData = extractTextureData(atlas, ref.getRegion());
                    if (textureData != null) {
                        texturesToRepack.put(entry.getKey(), textureData);
                    }
                }
            }
        }
        
        if (texturesToRepack.isEmpty()) {
            return false;
        }
        
        // Clean up old atlases
        for (TextureAtlas atlas : categoryAtlases) {
            atlas.cleanup();
        }
        categoryAtlases.clear();
        
        // Remove old texture references for this category
        textureReferences.entrySet().removeIf(entry -> 
            entry.getValue().getCategory() == category);
        
        // Repack textures into new atlases
        int repackedCount = 0;
        for (Map.Entry<String, TextureData> entry : texturesToRepack.entrySet()) {
            String name = entry.getKey();
            TextureData data = entry.getValue();
            
            AtlasTextureReference newRef = addTexture(name, data.data, data.width, data.height, category);
            if (newRef != null) {
                repackedCount++;
            } else {
                logger.warn("Failed to repack texture: {}", name);
            }
        }
        
        // Update memory tracking
        totalAtlasMemory.set(calculateAtlasMemoryUsage());
        
        logger.info("Defragmentation complete for category: {}. Repacked {}/{} textures into {} atlases", 
                   category.getName(), repackedCount, texturesToRepack.size(), categoryAtlases.size());
        
        return true;
    }
    
    /**
     * Performs defragmentation on all atlas categories that need it.
     * 
     * @return Number of categories that were defragmented
     */
    public int defragmentAllAtlases() {
        int defragmentedCount = 0;
        
        for (AtlasCategory category : AtlasCategory.values()) {
            if (defragmentAtlases(category)) {
                defragmentedCount++;
            }
        }
        
        if (defragmentedCount > 0) {
            logger.info("Defragmented {} atlas categories", defragmentedCount);
        }
        
        return defragmentedCount;
    }
    
    /**
     * Schedules automatic defragmentation based on fragmentation metrics.
     * This should be called periodically in long-running games.
     * 
     * @param forceDefragmentation If true, defragments regardless of metrics
     */
    public void scheduleDefragmentation(boolean forceDefragmentation) {
        if (forceDefragmentation) {
            defragmentAllAtlases();
            return;
        }
        
        // Check if defragmentation is needed based on metrics
        boolean needsDefragmentation = false;
        
        for (AtlasCategory category : AtlasCategory.values()) {
            List<TextureAtlas> categoryAtlases = atlases.get(category);
            
            if (categoryAtlases.size() > 3) { // Too many atlases
                needsDefragmentation = true;
                break;
            }
            
            if (categoryAtlases.size() > 1) {
                double avgUtilization = categoryAtlases.stream()
                        .mapToDouble(TextureAtlas::getSpaceUtilization)
                        .average()
                        .orElse(1.0);
                
                if (avgUtilization < 0.5) { // Low utilization
                    needsDefragmentation = true;
                    break;
                }
            }
        }
        
        if (needsDefragmentation) {
            logger.info("Automatic defragmentation triggered");
            defragmentAllAtlases();
        }
    }
    
    /**
     * Gets defragmentation statistics for monitoring.
     * 
     * @return Formatted statistics about atlas fragmentation
     */
    public String getDefragmentationStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Atlas Defragmentation Statistics:\n");
        
        for (AtlasCategory category : AtlasCategory.values()) {
            List<TextureAtlas> categoryAtlases = atlases.get(category);
            
            if (categoryAtlases.isEmpty()) {
                continue;
            }
            
            double avgUtilization = categoryAtlases.stream()
                    .mapToDouble(TextureAtlas::getSpaceUtilization)
                    .average()
                    .orElse(0.0);
            
            double minUtilization = categoryAtlases.stream()
                    .mapToDouble(TextureAtlas::getSpaceUtilization)
                    .min()
                    .orElse(0.0);
            
            stats.append(String.format("  %s: %d atlases, %.1f%% avg util, %.1f%% min util", 
                    category.getName(), categoryAtlases.size(), 
                    avgUtilization * 100, minUtilization * 100));
            
            if (categoryAtlases.size() > 3 || avgUtilization < 0.5) {
                stats.append(" [NEEDS DEFRAG]");
            }
            stats.append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Helper class to store texture data during defragmentation.
     */
    private static class TextureData {
        final ByteBuffer data;
        final int width;
        final int height;
        
        TextureData(ByteBuffer data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Extracts texture data from an atlas region for defragmentation.
     * 
     * @param atlas The source atlas
     * @param region The region to extract
     * @return TextureData containing the extracted texture, or null if failed
     */
    private TextureData extractTextureData(TextureAtlas atlas, TextureAtlas.AtlasRegion region) {
        try {
            // This is a simplified implementation
            // In a real implementation, you would read the texture data from the GPU
            // and extract the specific region
            
            Vector2f size = region.getSize();
            int width = (int) size.x;
            int height = (int) size.y;
            
            // For now, create a placeholder texture data
            // This should be replaced with actual texture extraction from GPU
            ByteBuffer data = ByteBuffer.allocateDirect(width * height * 4);
            
            // Fill with a pattern to indicate this is extracted data
            for (int i = 0; i < width * height; i++) {
                data.put((byte) 128); // R
                data.put((byte) 128); // G
                data.put((byte) 128); // B
                data.put((byte) 255); // A
            }
            data.flip();
            
            return new TextureData(data, width, height);
        } catch (Exception e) {
            logger.error("Failed to extract texture data for region: {} - {}", region.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Cleans up all atlas resources.
     */
    public void cleanup() {
        logger.info("Cleaning up texture atlas manager");
        
        // Cancel pending requests
        pendingRequests.values().forEach(future -> future.cancel(true));
        pendingRequests.clear();
        
        for (List<TextureAtlas> categoryAtlases : atlases.values()) {
            for (TextureAtlas atlas : categoryAtlases) {
                atlas.cleanup();
            }
            categoryAtlases.clear();
        }
        
        textureReferences.clear();
        totalAtlasMemory.set(0);
        
        if (streamingManager != null) {
            streamingManager.shutdown();
        }
        
        logger.info("Texture atlas manager cleanup complete");
    }
    
    // ===== PBR Material Support Methods =====
    
    /**
     * Represents a complete PBR material with all texture maps.
     */
    public static class PBRMaterial {
        private final AtlasTextureReference albedo;
        private final AtlasTextureReference normal;
        private final AtlasTextureReference metallic;
        private final AtlasTextureReference roughness;
        private final AtlasTextureReference ao;
        private final AtlasTextureReference emission;
        
        public PBRMaterial(AtlasTextureReference albedo, AtlasTextureReference normal,
                          AtlasTextureReference metallic, AtlasTextureReference roughness,
                          AtlasTextureReference ao, AtlasTextureReference emission) {
            this.albedo = albedo;
            this.normal = normal;
            this.metallic = metallic;
            this.roughness = roughness;
            this.ao = ao;
            this.emission = emission;
        }
        
        public AtlasTextureReference getAlbedo() { return albedo; }
        public AtlasTextureReference getNormal() { return normal; }
        public AtlasTextureReference getMetallic() { return metallic; }
        public AtlasTextureReference getRoughness() { return roughness; }
        public AtlasTextureReference getAO() { return ao; }
        public AtlasTextureReference getEmission() { return emission; }
    }
    
    /**
     * Loads a complete PBR material with all texture maps.
     * 
     * @param materialName Base name for the material
     * @param albedoPath Path to albedo texture
     * @param normalPath Path to normal map (can be null)
     * @param metallicPath Path to metallic map (can be null)
     * @param roughnessPath Path to roughness map (can be null)
     * @param aoPath Path to ambient occlusion map (can be null)
     * @param emissionPath Path to emission map (can be null)
     * @return CompletableFuture containing the PBR material
     */
    public CompletableFuture<PBRMaterial> loadPBRMaterial(String materialName,
                                                         String albedoPath,
                                                         String normalPath,
                                                         String metallicPath,
                                                         String roughnessPath,
                                                         String aoPath,
                                                         String emissionPath) {
        List<CompletableFuture<AtlasTextureReference>> futures = new ArrayList<>();
        
        // Load albedo (required)
        futures.add(requestTextureAsync(materialName + "_albedo", albedoPath, AtlasCategory.PBR_ALBEDO, StreamingPriority.HIGH, null));
        
        // Load optional maps
        futures.add(normalPath != null ? 
            requestTextureAsync(materialName + "_normal", normalPath, AtlasCategory.PBR_NORMAL, StreamingPriority.MEDIUM, null) :
            CompletableFuture.completedFuture(null));
            
        futures.add(metallicPath != null ? 
            requestTextureAsync(materialName + "_metallic", metallicPath, AtlasCategory.PBR_METALLIC, StreamingPriority.MEDIUM, null) :
            CompletableFuture.completedFuture(null));
            
        futures.add(roughnessPath != null ? 
            requestTextureAsync(materialName + "_roughness", roughnessPath, AtlasCategory.PBR_ROUGHNESS, StreamingPriority.MEDIUM, null) :
            CompletableFuture.completedFuture(null));
            
        futures.add(aoPath != null ? 
            requestTextureAsync(materialName + "_ao", aoPath, AtlasCategory.PBR_AO, StreamingPriority.LOW, null) :
            CompletableFuture.completedFuture(null));
            
        futures.add(emissionPath != null ? 
            requestTextureAsync(materialName + "_emission", emissionPath, AtlasCategory.PBR_EMISSION, StreamingPriority.LOW, null) :
            CompletableFuture.completedFuture(null));
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                try {
                    return new PBRMaterial(
                        futures.get(0).get(), // albedo
                        futures.get(1).get(), // normal
                        futures.get(2).get(), // metallic
                        futures.get(3).get(), // roughness
                        futures.get(4).get(), // ao
                        futures.get(5).get()  // emission
                    );
                } catch (Exception e) {
                    logger.error("Failed to create PBR material: {}", materialName, e);
                    return null;
                }
            });
    }
    
    /**
     * Loads a PBR material from a standard naming convention.
     * Expects files named: materialName_albedo.png, materialName_normal.png, etc.
     * 
     * @param materialName Base name for the material
     * @param basePath Base directory path
     * @return CompletableFuture containing the PBR material
     */
    public CompletableFuture<PBRMaterial> loadPBRMaterialStandard(String materialName, String basePath) {
        String base = basePath + "/" + materialName;
        return loadPBRMaterial(materialName,
            base + "_albedo.png",
            base + "_normal.png",
            base + "_metallic.png", 
            base + "_roughness.png",
            base + "_ao.png",
            base + "_emission.png");
    }
    
    /**
     * Gets statistics about PBR texture usage.
     * 
     * @return Formatted statistics about PBR texture atlases
     */
    public String getPBRStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("PBR Texture Atlas Statistics:\n");
        
        AtlasCategory[] pbrCategories = {
            AtlasCategory.PBR_ALBEDO, AtlasCategory.PBR_NORMAL,
            AtlasCategory.PBR_METALLIC, AtlasCategory.PBR_ROUGHNESS,
            AtlasCategory.PBR_AO, AtlasCategory.PBR_EMISSION
        };
        
        for (AtlasCategory category : pbrCategories) {
            List<TextureAtlas> categoryAtlases = atlases.get(category);
            int textureCount = categoryAtlases.stream()
                .mapToInt(atlas -> atlas.getTextureCount())
                .sum();
            
            double avgUtilization = categoryAtlases.stream()
                .mapToDouble(TextureAtlas::getSpaceUtilization)
                .average()
                .orElse(0.0);
            
            stats.append(String.format("  %s: %d textures in %d atlases (%.1f%% utilization)\n",
                category.getName(), textureCount, categoryAtlases.size(), avgUtilization * 100));
        }
        
        return stats.toString();
    }
}