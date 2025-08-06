package com.odyssey.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Level-of-Detail texture atlas system that manages multiple quality levels
 * of textures based on distance and importance.
 */
public class LODTextureAtlasSystem {
    private static final Logger logger = Logger.getLogger(LODTextureAtlasSystem.class.getName());
    
    /**
     * Different quality levels for LOD.
     */
    public enum LODLevel {
        ULTRA(0, 1.0f, "ultra"),      // Full resolution
        HIGH(1, 0.75f, "high"),       // 75% resolution
        MEDIUM(2, 0.5f, "medium"),    // 50% resolution
        LOW(3, 0.25f, "low"),         // 25% resolution
        MINIMAL(4, 0.125f, "minimal"); // 12.5% resolution
        
        private final int level;
        private final float scaleFactor;
        private final String suffix;
        
        LODLevel(int level, float scaleFactor, String suffix) {
            this.level = level;
            this.scaleFactor = scaleFactor;
            this.suffix = suffix;
        }
        
        public int getLevel() { return level; }
        public float getScaleFactor() { return scaleFactor; }
        public String getSuffix() { return suffix; }
    }
    
    /**
     * Distance-based LOD configuration.
     */
    public static class LODConfig {
        private final Map<LODLevel, Float> distanceThresholds;
        private final boolean enableDynamicLOD;
        private final float lodTransitionSpeed;
        
        public LODConfig() {
            this.distanceThresholds = new HashMap<>();
            this.enableDynamicLOD = true;
            this.lodTransitionSpeed = 2.0f;
            
            // Default distance thresholds
            distanceThresholds.put(LODLevel.ULTRA, 16.0f);
            distanceThresholds.put(LODLevel.HIGH, 32.0f);
            distanceThresholds.put(LODLevel.MEDIUM, 64.0f);
            distanceThresholds.put(LODLevel.LOW, 128.0f);
            distanceThresholds.put(LODLevel.MINIMAL, Float.MAX_VALUE);
        }
        
        public LODConfig setDistanceThreshold(LODLevel level, float distance) {
            distanceThresholds.put(level, distance);
            return this;
        }
        
        public float getDistanceThreshold(LODLevel level) {
            return distanceThresholds.getOrDefault(level, Float.MAX_VALUE);
        }
        
        public LODLevel getLODForDistance(float distance) {
            for (LODLevel level : LODLevel.values()) {
                if (distance <= getDistanceThreshold(level)) {
                    return level;
                }
            }
            return LODLevel.MINIMAL;
        }
        
        public boolean isDynamicLODEnabled() { return enableDynamicLOD; }
        public float getLODTransitionSpeed() { return lodTransitionSpeed; }
    }
    
    /**
     * Represents a texture with multiple LOD levels.
     */
    public static class LODTexture {
        private final String baseName;
        private final TextureAtlasManager.AtlasCategory category;
        private final Map<LODLevel, TextureAtlasManager.AtlasTextureReference> lodReferences;
        private final Map<LODLevel, String> lodPaths;
        private LODLevel currentLOD;
        private float lastDistance;
        
        public LODTexture(String baseName, TextureAtlasManager.AtlasCategory category) {
            this.baseName = baseName;
            this.category = category;
            this.lodReferences = new ConcurrentHashMap<>();
            this.lodPaths = new HashMap<>();
            this.currentLOD = LODLevel.MEDIUM;
            this.lastDistance = 0.0f;
        }
        
        public void setLODPath(LODLevel level, String path) {
            lodPaths.put(level, path);
        }
        
        public String getLODPath(LODLevel level) {
            return lodPaths.get(level);
        }
        
        public void setLODReference(LODLevel level, TextureAtlasManager.AtlasTextureReference reference) {
            lodReferences.put(level, reference);
        }
        
        public TextureAtlasManager.AtlasTextureReference getLODReference(LODLevel level) {
            return lodReferences.get(level);
        }
        
        public TextureAtlasManager.AtlasTextureReference getCurrentReference() {
            return lodReferences.get(currentLOD);
        }
        
        public LODLevel getCurrentLOD() { return currentLOD; }
        public void setCurrentLOD(LODLevel lod) { this.currentLOD = lod; }
        
        public float getLastDistance() { return lastDistance; }
        public void setLastDistance(float distance) { this.lastDistance = distance; }
        
        public String getBaseName() { return baseName; }
        public TextureAtlasManager.AtlasCategory getCategory() { return category; }
        
        public boolean hasLOD(LODLevel level) {
            return lodReferences.containsKey(level);
        }
        
        public List<LODLevel> getAvailableLODs() {
            return new ArrayList<>(lodReferences.keySet());
        }
    }
    
    private final TextureAtlasManager atlasManager;
    private final Map<String, LODTexture> lodTextures;
    private final LODConfig lodConfig;
    private final Map<String, CompletableFuture<LODTexture>> pendingLODRequests;
    
    /**
     * Creates a new LOD texture atlas system.
     * 
     * @param atlasManager The texture atlas manager
     * @param lodConfig LOD configuration
     */
    public LODTextureAtlasSystem(TextureAtlasManager atlasManager, LODConfig lodConfig) {
        this.atlasManager = atlasManager;
        this.lodTextures = new ConcurrentHashMap<>();
        this.lodConfig = lodConfig;
        this.pendingLODRequests = new ConcurrentHashMap<>();
        
        logger.info("Initialized LOD texture atlas system");
    }
    
    /**
     * Creates a new LOD texture atlas system with default configuration.
     * 
     * @param atlasManager The texture atlas manager
     */
    public LODTextureAtlasSystem(TextureAtlasManager atlasManager) {
        this(atlasManager, new LODConfig());
    }
    
    /**
     * Registers a texture with multiple LOD levels.
     * 
     * @param baseName Base name for the texture
     * @param category Atlas category
     * @param lodPaths Map of LOD levels to file paths
     * @return CompletableFuture that completes when texture is registered
     */
    public CompletableFuture<LODTexture> registerLODTexture(String baseName, 
                                                           TextureAtlasManager.AtlasCategory category,
                                                           Map<LODLevel, String> lodPaths) {
        // Check if already registered
        LODTexture existing = lodTextures.get(baseName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        
        // Check if already being registered
        CompletableFuture<LODTexture> pending = pendingLODRequests.get(baseName);
        if (pending != null) {
            return pending;
        }
        
        LODTexture lodTexture = new LODTexture(baseName, category);
        
        // Set LOD paths
        for (Map.Entry<LODLevel, String> entry : lodPaths.entrySet()) {
            lodTexture.setLODPath(entry.getKey(), entry.getValue());
        }
        
        // Load the medium quality first as default
        CompletableFuture<LODTexture> future = loadLODLevel(lodTexture, LODLevel.MEDIUM)
                .thenApply(reference -> {
                    lodTextures.put(baseName, lodTexture);
                    pendingLODRequests.remove(baseName);
                    return lodTexture;
                });
        
        pendingLODRequests.put(baseName, future);
        return future;
    }
    
    /**
     * Requests a texture with appropriate LOD based on distance.
     * 
     * @param baseName Base name of the texture
     * @param distance Distance from viewer
     * @param callback Callback when texture is ready (optional)
     * @return CompletableFuture that completes when texture is ready
     */
    public CompletableFuture<TextureAtlasManager.AtlasTextureReference> requestTexture(String baseName, 
                                                                                      float distance,
                                                                                      Consumer<TextureAtlasManager.AtlasTextureReference> callback) {
        LODTexture lodTexture = lodTextures.get(baseName);
        if (lodTexture == null) {
            logger.warning("LOD texture not found: " + baseName);
            return CompletableFuture.completedFuture(null);
        }
        
        LODLevel requiredLOD = lodConfig.getLODForDistance(distance);
        lodTexture.setLastDistance(distance);
        
        // Check if we already have the required LOD
        if (lodTexture.hasLOD(requiredLOD)) {
            lodTexture.setCurrentLOD(requiredLOD);
            TextureAtlasManager.AtlasTextureReference reference = lodTexture.getCurrentReference();
            if (callback != null) {
                callback.accept(reference);
            }
            return CompletableFuture.completedFuture(reference);
        }
        
        // Load the required LOD level
        return loadLODLevel(lodTexture, requiredLOD)
                .thenApply(reference -> {
                    lodTexture.setCurrentLOD(requiredLOD);
                    if (callback != null) {
                        callback.accept(reference);
                    }
                    return reference;
                });
    }
    
    /**
     * Updates LOD levels for all textures based on current distances.
     * Should be called regularly (e.g., each frame).
     */
    public void updateLODLevels() {
        if (!lodConfig.isDynamicLODEnabled()) {
            return;
        }
        
        for (LODTexture lodTexture : lodTextures.values()) {
            float distance = lodTexture.getLastDistance();
            LODLevel requiredLOD = lodConfig.getLODForDistance(distance);
            
            if (requiredLOD != lodTexture.getCurrentLOD()) {
                // Check if we have the required LOD
                if (lodTexture.hasLOD(requiredLOD)) {
                    lodTexture.setCurrentLOD(requiredLOD);
                } else {
                    // Load the required LOD in background
                    loadLODLevel(lodTexture, requiredLOD)
                            .thenRun(() -> lodTexture.setCurrentLOD(requiredLOD));
                }
            }
        }
    }
    
    /**
     * Preloads LOD levels for textures within a certain distance.
     * 
     * @param maxDistance Maximum distance to preload
     * @param priority Loading priority
     */
    public void preloadLODLevels(float maxDistance, TextureAtlasManager.StreamingPriority priority) {
        List<CompletableFuture<Void>> preloadFutures = new ArrayList<>();
        
        for (LODTexture lodTexture : lodTextures.values()) {
            LODLevel requiredLOD = lodConfig.getLODForDistance(maxDistance);
            
            if (!lodTexture.hasLOD(requiredLOD)) {
                CompletableFuture<Void> preloadFuture = loadLODLevel(lodTexture, requiredLOD)
                        .thenRun(() -> {});
                preloadFutures.add(preloadFuture);
            }
        }
        
        CompletableFuture.allOf(preloadFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("Preloaded LOD levels for " + preloadFutures.size() + " textures"));
    }
    
    /**
     * Loads a specific LOD level for a texture.
     * 
     * @param lodTexture The LOD texture
     * @param level The LOD level to load
     * @return CompletableFuture that completes when LOD is loaded
     */
    private CompletableFuture<TextureAtlasManager.AtlasTextureReference> loadLODLevel(LODTexture lodTexture, LODLevel level) {
        String lodPath = lodTexture.getLODPath(level);
        if (lodPath == null) {
            // Try to find a fallback LOD
            for (LODLevel fallback : LODLevel.values()) {
                if (lodTexture.hasLOD(fallback)) {
                    return CompletableFuture.completedFuture(lodTexture.getLODReference(fallback));
                }
            }
            return CompletableFuture.completedFuture(null);
        }
        
        String lodName = lodTexture.getBaseName() + "_" + level.getSuffix();
        TextureAtlasManager.StreamingPriority priority = getPriorityForLOD(level);
        
        return atlasManager.requestTextureAsync(lodName, lodPath, lodTexture.getCategory(), priority, null)
                .thenApply(reference -> {
                    lodTexture.setLODReference(level, reference);
                    return reference;
                });
    }
    
    /**
     * Gets appropriate priority for LOD level.
     * 
     * @param level The LOD level
     * @return The streaming priority
     */
    private TextureAtlasManager.StreamingPriority getPriorityForLOD(LODLevel level) {
        switch (level) {
            case ULTRA: return TextureAtlasManager.StreamingPriority.CRITICAL;
            case HIGH: return TextureAtlasManager.StreamingPriority.HIGH;
            case MEDIUM: return TextureAtlasManager.StreamingPriority.MEDIUM;
            case LOW: return TextureAtlasManager.StreamingPriority.LOW;
            case MINIMAL: return TextureAtlasManager.StreamingPriority.BACKGROUND;
            default: return TextureAtlasManager.StreamingPriority.MEDIUM;
        }
    }
    
    /**
     * Gets a LOD texture by name.
     * 
     * @param baseName Base name of the texture
     * @return The LOD texture, or null if not found
     */
    public LODTexture getLODTexture(String baseName) {
        return lodTextures.get(baseName);
    }
    
    /**
     * Gets LOD system statistics.
     * 
     * @return Formatted statistics string
     */
    public String getStatistics() {
        int totalTextures = lodTextures.size();
        int totalLODs = lodTextures.values().stream()
                .mapToInt(texture -> texture.getAvailableLODs().size())
                .sum();
        
        Map<LODLevel, Integer> lodCounts = new HashMap<>();
        for (LODLevel level : LODLevel.values()) {
            lodCounts.put(level, 0);
        }
        
        for (LODTexture texture : lodTextures.values()) {
            LODLevel currentLOD = texture.getCurrentLOD();
            lodCounts.put(currentLOD, lodCounts.get(currentLOD) + 1);
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("LOD Texture Atlas System Statistics:\n");
        stats.append("Total LOD textures: ").append(totalTextures).append("\n");
        stats.append("Total LOD levels loaded: ").append(totalLODs).append("\n");
        stats.append("Current LOD distribution:\n");
        
        for (LODLevel level : LODLevel.values()) {
            int count = lodCounts.get(level);
            if (count > 0) {
                stats.append("  ").append(level.name()).append(": ").append(count).append("\n");
            }
        }
        
        return stats.toString();
    }
    
    /**
     * Cleans up LOD system resources.
     */
    public void cleanup() {
        logger.info("Cleaning up LOD texture atlas system");
        
        // Cancel pending requests
        pendingLODRequests.values().forEach(future -> future.cancel(true));
        pendingLODRequests.clear();
        
        lodTextures.clear();
        
        logger.info("LOD texture atlas system cleanup complete");
    }
}