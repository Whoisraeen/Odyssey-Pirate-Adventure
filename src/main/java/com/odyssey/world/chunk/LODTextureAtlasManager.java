package com.odyssey.world.chunk;

import com.odyssey.graphics.TextureAtlasManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Manages distance-based texture atlases for the LOD system.
 * Provides efficient texture atlas management with multiple resolution levels.
 */
public class LODTextureAtlasManager {
    private static final Logger logger = LoggerFactory.getLogger(LODTextureAtlasManager.class);
    
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
     * Represents a texture atlas with multiple resolution levels.
     */
    public static class TextureAtlas {
        private final String name;
        private final int baseResolution;
        private final Map<Integer, AtlasLevel> levels = new ConcurrentHashMap<>();
        private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger currentUsers = new AtomicInteger(0);
        
        /**
         * Represents a specific resolution level of a texture atlas.
         */
        public static class AtlasLevel {
            private final int resolution;
            private final int textureId;
            private final float[] uvCoordinates;
            private final int textureCount;
            private final long memoryUsage;
            private final AtomicLong lastUsed = new AtomicLong(System.currentTimeMillis());
            
            /**
             * Creates a new atlas level.
             * 
             * @param resolution texture resolution
             * @param textureId OpenGL texture ID
             * @param uvCoordinates UV coordinates for each texture in the atlas
             * @param textureCount number of textures in the atlas
             */
            public AtlasLevel(int resolution, int textureId, float[] uvCoordinates, int textureCount) {
                this.resolution = resolution;
                this.textureId = textureId;
                this.uvCoordinates = uvCoordinates.clone();
                this.textureCount = textureCount;
                this.memoryUsage = calculateMemoryUsage(resolution, textureCount);
            }
            
            /**
             * Calculates memory usage for this atlas level.
             * 
             * @param resolution texture resolution
             * @param textureCount number of textures
             * @return memory usage in bytes
             */
            private long calculateMemoryUsage(int resolution, int textureCount) {
                // Assume RGBA format (4 bytes per pixel) with mipmaps (adds ~33% more)
                long pixelCount = (long) resolution * resolution * textureCount;
                return (long) (pixelCount * 4 * 1.33f);
            }
            
            /**
             * Updates the last used timestamp.
             */
            public void markUsed() {
                lastUsed.set(System.currentTimeMillis());
            }
            
            /**
             * Gets UV coordinates for a specific texture index.
             * 
             * @param textureIndex the texture index
             * @return UV coordinates [minU, minV, maxU, maxV]
             */
            public float[] getUVCoordinates(int textureIndex) {
                if (textureIndex < 0 || textureIndex >= textureCount) {
                    throw new IllegalArgumentException("Invalid texture index: " + textureIndex);
                }
                
                int baseIndex = textureIndex * 4;
                return new float[] {
                    uvCoordinates[baseIndex],     // minU
                    uvCoordinates[baseIndex + 1], // minV
                    uvCoordinates[baseIndex + 2], // maxU
                    uvCoordinates[baseIndex + 3]  // maxV
                };
            }
            
            public int getResolution() { return resolution; }
            public int getTextureId() { return textureId; }
            public int getTextureCount() { return textureCount; }
            public long getMemoryUsage() { return memoryUsage; }
            public long getLastUsed() { return lastUsed.get(); }
        }
        
        /**
         * Creates a new texture atlas.
         * 
         * @param name atlas name
         * @param baseResolution base resolution for the atlas
         */
        public TextureAtlas(String name, int baseResolution) {
            this.name = name;
            this.baseResolution = baseResolution;
        }
        
        /**
         * Adds a resolution level to this atlas.
         * 
         * @param level the atlas level to add
         */
        public void addLevel(AtlasLevel level) {
            levels.put(level.getResolution(), level);
            logger.debug("Added {}x{} level to atlas '{}' ({}MB)", 
                        level.getResolution(), level.getResolution(), name,
                        level.getMemoryUsage() / (1024 * 1024));
        }
        
        /**
         * Gets the appropriate atlas level for a given distance.
         * 
         * @param distance viewing distance
         * @param maxDistance maximum viewing distance
         * @return the appropriate atlas level
         */
        public AtlasLevel getLevelForDistance(float distance, float maxDistance) {
            lastAccessTime.set(System.currentTimeMillis());
            
            // Calculate desired resolution based on distance
            float distanceRatio = Math.min(1.0f, distance / maxDistance);
            int desiredResolution = Math.max(16, (int) (baseResolution * (1.0f - distanceRatio * 0.75f)));
            
            // Find the closest available resolution
            AtlasLevel bestLevel = null;
            int bestDifference = Integer.MAX_VALUE;
            
            for (AtlasLevel level : levels.values()) {
                int difference = Math.abs(level.getResolution() - desiredResolution);
                if (difference < bestDifference) {
                    bestDifference = difference;
                    bestLevel = level;
                }
            }
            
            if (bestLevel != null) {
                bestLevel.markUsed();
                currentUsers.incrementAndGet();
            }
            
            return bestLevel;
        }
        
        /**
         * Gets a specific resolution level.
         * 
         * @param resolution the resolution
         * @return the atlas level or null if not found
         */
        public AtlasLevel getLevel(int resolution) {
            AtlasLevel level = levels.get(resolution);
            if (level != null) {
                level.markUsed();
                lastAccessTime.set(System.currentTimeMillis());
                currentUsers.incrementAndGet();
            }
            return level;
        }
        
        /**
         * Releases a reference to this atlas.
         */
        public void releaseReference() {
            currentUsers.decrementAndGet();
        }
        
        /**
         * Gets all available resolutions.
         * 
         * @return list of available resolutions
         */
        public List<Integer> getAvailableResolutions() {
            List<Integer> resolutions = new ArrayList<>(levels.keySet());
            Collections.sort(resolutions, Collections.reverseOrder());
            return resolutions;
        }
        
        /**
         * Gets the total memory usage of this atlas.
         * 
         * @return total memory usage in bytes
         */
        public long getTotalMemoryUsage() {
            return levels.values().stream()
                         .mapToLong(AtlasLevel::getMemoryUsage)
                         .sum();
        }
        
        public String getName() { return name; }
        public int getBaseResolution() { return baseResolution; }
        public long getLastAccessTime() { return lastAccessTime.get(); }
        public int getCurrentUsers() { return currentUsers.get(); }
        public int getLevelCount() { return levels.size(); }
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

    /**
     * Configuration for texture atlas management.
     */
    public static class AtlasConfig {
        private final int maxMemoryUsageMB;
        private final long atlasTimeoutMs;
        private final float nearDistance;
        private final float farDistance;
        private final boolean enableDynamicLoading;
        private final int[] supportedResolutions;
        
        /**
         * Creates atlas configuration.
         * 
         * @param maxMemoryUsageMB maximum memory usage in MB
         * @param atlasTimeoutMs timeout for unused atlases in milliseconds
         * @param nearDistance near distance for high-res textures
         * @param farDistance far distance for low-res textures
         * @param enableDynamicLoading whether to enable dynamic loading
         * @param supportedResolutions array of supported resolutions
         */
        public AtlasConfig(int maxMemoryUsageMB, long atlasTimeoutMs, float nearDistance, 
                          float farDistance, boolean enableDynamicLoading, int[] supportedResolutions) {
            this.maxMemoryUsageMB = maxMemoryUsageMB;
            this.atlasTimeoutMs = atlasTimeoutMs;
            this.nearDistance = nearDistance;
            this.farDistance = farDistance;
            this.enableDynamicLoading = enableDynamicLoading;
            this.supportedResolutions = supportedResolutions.clone();
        }
        
        /**
         * Creates default atlas configuration.
         * 
         * @return default configuration
         */
        public static AtlasConfig createDefault() {
            return new AtlasConfig(
                512, // 512MB max memory
                300000, // 5 minutes timeout
                32.0f, // Near distance
                256.0f, // Far distance
                true, // Enable dynamic loading
                new int[]{1024, 512, 256, 128, 64, 32, 16} // Supported resolutions
            );
        }
        
        public int getMaxMemoryUsageMB() { return maxMemoryUsageMB; }
        public long getAtlasTimeoutMs() { return atlasTimeoutMs; }
        public float getNearDistance() { return nearDistance; }
        public float getFarDistance() { return farDistance; }
        public boolean isEnableDynamicLoading() { return enableDynamicLoading; }
        public int[] getSupportedResolutions() { return supportedResolutions.clone(); }
    }
    
    // Instance fields
    private final Map<String, TextureAtlas> atlases = new ConcurrentHashMap<>();
    private final Map<String, LODTexture> lodTextures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<LODTexture>> pendingLODRequests = new ConcurrentHashMap<>();
    private final AtlasConfig config;
    private final LODConfig lodConfig;
    private final TextureAtlasManager textureAtlasManager;
    private final AtomicLong totalMemoryUsage = new AtomicLong(0);
    private final AtomicInteger activeAtlases = new AtomicInteger(0);
    
    /**
     * Creates a new LOD texture atlas manager.
     * 
     * @param textureAtlasManager the texture atlas manager
     * @param config atlas configuration
     * @param lodConfig LOD configuration
     */
    public LODTextureAtlasManager(TextureAtlasManager textureAtlasManager, AtlasConfig config, LODConfig lodConfig) {
        this.textureAtlasManager = textureAtlasManager;
        this.config = config;
        this.lodConfig = lodConfig;
        logger.info("Initialized LOD texture atlas manager with {}MB memory limit", 
                   config.getMaxMemoryUsageMB());
    }
    
    /**
     * Creates a new LOD texture atlas manager with default LOD configuration.
     * 
     * @param textureAtlasManager the texture atlas manager
     * @param config atlas configuration
     */
    public LODTextureAtlasManager(TextureAtlasManager textureAtlasManager, AtlasConfig config) {
        this(textureAtlasManager, config, new LODConfig());
    }
    
    /**
     * Registers a new texture atlas.
     * 
     * @param atlas the texture atlas to register
     */
    public void registerAtlas(TextureAtlas atlas) {
        atlases.put(atlas.getName(), atlas);
        totalMemoryUsage.addAndGet(atlas.getTotalMemoryUsage());
        activeAtlases.incrementAndGet();
        
        logger.info("Registered atlas '{}' with {} levels ({}MB)", 
                   atlas.getName(), atlas.getLevelCount(), 
                   atlas.getTotalMemoryUsage() / (1024 * 1024));
        
        // Check memory usage and cleanup if necessary
        cleanupUnusedAtlases();
    }
    
    /**
     * Gets a texture atlas by name.
     * 
     * @param name atlas name
     * @return the texture atlas or null if not found
     */
    public TextureAtlas getAtlas(String name) {
        return atlases.get(name);
    }
    
    /**
     * Gets the appropriate texture atlas level for a chunk at a given distance.
     * 
     * @param atlasName atlas name
     * @param distance viewing distance
     * @return the appropriate atlas level or null if atlas not found
     */
    public TextureAtlas.AtlasLevel getAtlasLevelForDistance(String atlasName, float distance) {
        TextureAtlas atlas = atlases.get(atlasName);
        if (atlas == null) {
            return null;
        }
        
        return atlas.getLevelForDistance(distance, config.getFarDistance());
    }
    
    /**
     * Gets the appropriate texture atlas level for a specific LOD level.
     * 
     * @param atlasName atlas name
     * @param lodLevel LOD level
     * @return the appropriate atlas level or null if atlas not found
     */
    public TextureAtlas.AtlasLevel getAtlasLevelForLOD(String atlasName, LODLevel lodLevel) {
        TextureAtlas atlas = atlases.get(atlasName);
        if (atlas == null) {
            return null;
        }
        
        // Map LOD levels to texture resolutions using scale factors
        int targetResolution = Math.round(atlas.getBaseResolution() * lodLevel.getScaleFactor());
        targetResolution = Math.max(16, targetResolution); // Minimum resolution
        
        return atlas.getLevel(targetResolution);
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
            logger.warn("LOD texture not found: " + baseName);
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
        
        return textureAtlasManager.requestTextureAsync(lodName, lodPath, lodTexture.getCategory(), priority, null)
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
     * Releases a reference to an atlas level.
     * 
     * @param atlasName atlas name
     */
    public void releaseAtlasReference(String atlasName) {
        TextureAtlas atlas = atlases.get(atlasName);
        if (atlas != null) {
            atlas.releaseReference();
        }
    }
    
    /**
     * Cleans up unused atlases to free memory.
     */
    public void cleanupUnusedAtlases() {
        long currentTime = System.currentTimeMillis();
        long maxMemoryBytes = config.getMaxMemoryUsageMB() * 1024L * 1024L;
        
        // Check if we're over memory limit
        if (totalMemoryUsage.get() <= maxMemoryBytes) {
            return;
        }
        
        List<TextureAtlas> candidatesForRemoval = new ArrayList<>();
        
        // Find atlases that haven't been used recently and have no current users
        for (TextureAtlas atlas : atlases.values()) {
            if (atlas.getCurrentUsers() == 0 && 
                currentTime - atlas.getLastAccessTime() > config.getAtlasTimeoutMs()) {
                candidatesForRemoval.add(atlas);
            }
        }
        
        // Sort by last access time (oldest first)
        candidatesForRemoval.sort((a, b) -> 
            Long.compare(a.getLastAccessTime(), b.getLastAccessTime()));
        
        // Remove atlases until we're under the memory limit
        for (TextureAtlas atlas : candidatesForRemoval) {
            if (totalMemoryUsage.get() <= maxMemoryBytes) {
                break;
            }
            
            atlases.remove(atlas.getName());
            totalMemoryUsage.addAndGet(-atlas.getTotalMemoryUsage());
            activeAtlases.decrementAndGet();
            
            logger.info("Removed unused atlas '{}' to free {}MB", 
                       atlas.getName(), atlas.getTotalMemoryUsage() / (1024 * 1024));
        }
    }
    
    /**
     * Forces cleanup of all unused atlases.
     */
    public void forceCleanup() {
        long currentTime = System.currentTimeMillis();
        List<String> atlasesToRemove = new ArrayList<>();
        
        for (Map.Entry<String, TextureAtlas> entry : atlases.entrySet()) {
            TextureAtlas atlas = entry.getValue();
            if (atlas.getCurrentUsers() == 0) {
                atlasesToRemove.add(entry.getKey());
            }
        }
        
        for (String atlasName : atlasesToRemove) {
            TextureAtlas atlas = atlases.remove(atlasName);
            if (atlas != null) {
                totalMemoryUsage.addAndGet(-atlas.getTotalMemoryUsage());
                activeAtlases.decrementAndGet();
                
                logger.info("Force removed atlas '{}' ({}MB)", 
                           atlasName, atlas.getTotalMemoryUsage() / (1024 * 1024));
            }
        }
    }
    
    /**
     * Gets all registered atlas names.
     * 
     * @return list of atlas names
     */
    public List<String> getAtlasNames() {
        return new ArrayList<>(atlases.keySet());
    }
    
    /**
     * Gets memory usage statistics.
     * 
     * @return memory usage in bytes
     */
    public long getTotalMemoryUsage() {
        return totalMemoryUsage.get();
    }
    
    /**
     * Gets memory usage statistics in MB.
     * 
     * @return memory usage in MB
     */
    public long getTotalMemoryUsageMB() {
        return totalMemoryUsage.get() / (1024 * 1024);
    }
    
    /**
     * Gets the number of active atlases.
     * 
     * @return number of active atlases
     */
    public int getActiveAtlasCount() {
        return activeAtlases.get();
    }
    
    /**
     * Gets the atlas configuration.
     * 
     * @return atlas configuration
     */
    public AtlasConfig getConfig() {
        return config;
    }
    
    /**
     * Checks if memory usage is within limits.
     * 
     * @return true if within limits
     */
    public boolean isMemoryUsageWithinLimits() {
        long maxMemoryBytes = config.getMaxMemoryUsageMB() * 1024L * 1024L;
        return totalMemoryUsage.get() <= maxMemoryBytes;
    }
    
    /**
     * Gets detailed memory usage statistics.
     * 
     * @return formatted memory usage string
     */
    public String getMemoryUsageStats() {
        long usedMB = getTotalMemoryUsageMB();
        long maxMB = config.getMaxMemoryUsageMB();
        float percentage = (float) usedMB / maxMB * 100.0f;
        
        return String.format("Atlas Memory: %dMB / %dMB (%.1f%%) - %d atlases active", 
                           usedMB, maxMB, percentage, activeAtlases.get());
    }
    
    /**
     * Gets LOD system statistics.
     * 
     * @return Formatted statistics string
     */
    public String getLODStatistics() {
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
     * Gets the LOD configuration.
     * 
     * @return LOD configuration
     */
    public LODConfig getLODConfig() {
        return lodConfig;
    }
}