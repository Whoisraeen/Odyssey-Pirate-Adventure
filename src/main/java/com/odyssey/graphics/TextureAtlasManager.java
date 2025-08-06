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
        TERRAIN("terrain", 2048, TextureAtlas.PackingAlgorithm.SHELF);    // Similar terrain textures
        
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
}