package com.odyssey.world.chunk;

import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import com.odyssey.world.chunk.LODTextureAtlasManager.LODLevel;

/**
 * Manages Level of Detail (LOD) for chunks based on distance from player.
 * Provides different rendering qualities to optimize performance for distant chunks.
 * Implements hierarchical LOD with multiple resolution tiers similar to Distant Horizons.
 * 
 * @author Odyssey Team
 * @since 1.0.0
 */
public class ChunkLOD {
    private static final Logger logger = LoggerFactory.getLogger(ChunkLOD.class);
    
    /**
     * Represents a hierarchical LOD region that aggregates multiple chunks.
     * Supports 2x2, 4x4, 8x8, and larger chunk blocks for extreme distances.
     */
    public static class HierarchicalLODRegion {
        private final int regionX;
        private final int regionZ;
        private final int regionSize; // Size in chunks (2, 4, 8, 16, etc.)
        private final LODLevel lodLevel;
        private final float[] aggregatedVertices;
        private final int[] aggregatedIndices;
        private final byte[] compressedTerrainData;
        private final long lastUpdateTime;
        private final AtomicBoolean isGenerating = new AtomicBoolean(false);
        
        /**
         * Creates a hierarchical LOD region.
         * 
         * @param regionX region X coordinate (in region units)
         * @param regionZ region Z coordinate (in region units)
         * @param regionSize size of region in chunks (power of 2)
         * @param lodLevel LOD level for this region
         * @param vertices aggregated vertex data
         * @param indices aggregated index data
         * @param compressedData compressed terrain representation
         */
        public HierarchicalLODRegion(int regionX, int regionZ, int regionSize, 
                                   LODLevel lodLevel, float[] vertices, int[] indices,
                                   byte[] compressedData) {
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.regionSize = regionSize;
            this.lodLevel = lodLevel;
            this.aggregatedVertices = vertices != null ? vertices.clone() : new float[0];
            this.aggregatedIndices = indices != null ? indices.clone() : new int[0];
            this.compressedTerrainData = compressedData != null ? compressedData.clone() : new byte[0];
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public int getRegionX() { return regionX; }
        public int getRegionZ() { return regionZ; }
        public int getRegionSize() { return regionSize; }
        public LODLevel getLodLevel() { return lodLevel; }
        public float[] getAggregatedVertices() { return aggregatedVertices.clone(); }
        public int[] getAggregatedIndices() { return aggregatedIndices.clone(); }
        public byte[] getCompressedTerrainData() { return compressedTerrainData.clone(); }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public boolean isGenerating() { return isGenerating.get(); }
        public void setGenerating(boolean generating) { isGenerating.set(generating); }
        
        /**
         * Gets the world coordinates covered by this region.
         * 
         * @return array of [minX, minZ, maxX, maxZ] in world coordinates
         */
        public int[] getWorldBounds() {
            int chunkSize = ChunkPosition.CHUNK_SIZE;
            int minX = regionX * regionSize * chunkSize;
            int minZ = regionZ * regionSize * chunkSize;
            int maxX = minX + (regionSize * chunkSize);
            int maxZ = minZ + (regionSize * chunkSize);
            return new int[]{minX, minZ, maxX, maxZ};
        }
    }
    
    /**
     * Manages pre-computed LOD mesh caching for extreme distances.
     */
    public static class LODMeshCache {
        private final Map<String, HierarchicalLODRegion> regionCache = new ConcurrentHashMap<>();
        private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong cacheMisses = new AtomicLong(0);
        private final int maxCacheSize;
        
        public LODMeshCache(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }
        
        /**
         * Gets a cached LOD region.
         * 
         * @param regionKey unique key for the region
         * @return cached region or null if not found
         */
        public HierarchicalLODRegion getCachedRegion(String regionKey) {
            cacheLock.readLock().lock();
            try {
                HierarchicalLODRegion region = regionCache.get(regionKey);
                if (region != null) {
                    cacheHits.incrementAndGet();
                } else {
                    cacheMisses.incrementAndGet();
                }
                return region;
            } finally {
                cacheLock.readLock().unlock();
            }
        }
        
        /**
         * Caches a LOD region.
         * 
         * @param regionKey unique key for the region
         * @param region the region to cache
         */
        public void cacheRegion(String regionKey, HierarchicalLODRegion region) {
            cacheLock.writeLock().lock();
            try {
                if (regionCache.size() >= maxCacheSize) {
                    evictOldestRegion();
                }
                regionCache.put(regionKey, region);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        
        /**
         * Evicts the oldest cached region.
         */
        private void evictOldestRegion() {
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<String, HierarchicalLODRegion> entry : regionCache.entrySet()) {
                if (entry.getValue().getLastUpdateTime() < oldestTime) {
                    oldestTime = entry.getValue().getLastUpdateTime();
                    oldestKey = entry.getKey();
                }
            }
            
            if (oldestKey != null) {
                regionCache.remove(oldestKey);
            }
        }
        
        /**
         * Gets cache statistics.
         * 
         * @return array of [hits, misses, size, hitRatio]
         */
        public double[] getCacheStats() {
            long hits = cacheHits.get();
            long misses = cacheMisses.get();
            double hitRatio = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
            return new double[]{hits, misses, regionCache.size(), hitRatio};
        }
        
        /**
         * Clears the cache.
         */
        public void clear() {
            cacheLock.writeLock().lock();
            try {
                regionCache.clear();
                cacheHits.set(0);
                cacheMisses.set(0);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Represents far terrain mesh data for distant chunks.
     * Similar to Distant Horizons' simplified terrain representation.
     */
    public static class FarTerrainMesh {
        private final int chunkX;
        private final int chunkZ;
        private final float[] vertices;
        private final int[] indices;
        private final long timestamp;
        private final int lodLevel;
        
        /**
         * Creates a far terrain mesh.
         * 
         * @param chunkX chunk X coordinate
         * @param chunkZ chunk Z coordinate
         * @param vertices simplified vertex data
         * @param indices triangle indices
         * @param lodLevel LOD level used for generation
         */
        public FarTerrainMesh(int chunkX, int chunkZ, float[] vertices, 
                             int[] indices, int lodLevel) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.vertices = vertices.clone();
            this.indices = indices.clone();
            this.timestamp = System.currentTimeMillis();
            this.lodLevel = lodLevel;
        }
        
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public float[] getVertices() { return vertices.clone(); }
        public int[] getIndices() { return indices.clone(); }
        public long getTimestamp() { return timestamp; }
        public int getLodLevel() { return lodLevel; }
    }
    
    /**
     * Represents impostor data for extremely distant chunks.
     * Uses simplified representation like billboards or low-poly meshes.
     */
    public static class ImpostorData {
        private final int chunkX;
        private final int chunkZ;
        private final byte[] colorData;
        private final float averageHeight;
        private final long timestamp;
        
        /**
         * Creates impostor data.
         * 
         * @param chunkX chunk X coordinate
         * @param chunkZ chunk Z coordinate
         * @param colorData compressed color information
         * @param averageHeight average terrain height
         */
        public ImpostorData(int chunkX, int chunkZ, byte[] colorData, float averageHeight) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.colorData = colorData.clone();
            this.averageHeight = averageHeight;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public byte[] getColorData() { return colorData.clone(); }
        public float getAverageHeight() { return averageHeight; }
        public long getTimestamp() { return timestamp; }
    }
    
    // LODLevel enum is now imported from LODTextureAtlasManager for consistency
    
    /**
     * Configuration for LOD distance thresholds.
     */
    public static class LODConfig {
        /** Distance for full detail rendering */
        public final float fullDetailDistance;
        
        /** Distance for high detail rendering */
        public final float highDetailDistance;
        
        /** Distance for medium detail rendering */
        public final float mediumDetailDistance;
        
        /** Distance for low detail rendering */
        public final float lowDetailDistance;
        
        /** Distance beyond which chunks use minimal detail */
        public final float minimalDetailDistance;
        
        /** Distance for far terrain rendering (like Distant Horizons) */
        public final float farTerrainDistance;
        
        /** Distance for impostor rendering (extreme distances) */
        public final float impostorDistance;
        
        /** Maximum render distance for any LOD level */
        public final float maxRenderDistance;
        
        /**
         * Creates LOD configuration with specified distances.
         * 
         * @param fullDetailDistance distance for full detail
         * @param highDetailDistance distance for high detail
         * @param mediumDetailDistance distance for medium detail
         * @param lowDetailDistance distance for low detail
         * @param minimalDetailDistance distance for minimal detail
         * @param farTerrainDistance distance for far terrain
         * @param impostorDistance distance for impostor rendering
         * @param maxRenderDistance maximum render distance
         */
        public LODConfig(float fullDetailDistance, float highDetailDistance, 
                        float mediumDetailDistance, float lowDetailDistance, 
                        float minimalDetailDistance, float farTerrainDistance,
                        float impostorDistance, float maxRenderDistance) {
            this.fullDetailDistance = fullDetailDistance;
            this.highDetailDistance = highDetailDistance;
            this.mediumDetailDistance = mediumDetailDistance;
            this.lowDetailDistance = lowDetailDistance;
            this.minimalDetailDistance = minimalDetailDistance;
            this.farTerrainDistance = farTerrainDistance;
            this.impostorDistance = impostorDistance;
            this.maxRenderDistance = maxRenderDistance;
        }
        
        /**
         * Creates default LOD configuration with Distant Horizons-like distances.
         * 
         * @return default configuration
         */
        public static LODConfig createDefault() {
            return new LODConfig(
                4.0f,    // Full detail
                8.0f,    // High detail  
                16.0f,   // Medium detail
                32.0f,   // Low detail
                64.0f,   // Minimal detail
                128.0f,  // Far terrain (like Distant Horizons)
                256.0f,  // Impostor rendering
                512.0f   // Maximum render distance
            );
        }
    }
    
    /** Current LOD configuration */
    private LODConfig config;
    
    /** Cache of chunk LOD levels to avoid recalculation */
    private final Map<ChunkPosition, LODLevel> lodCache;
    
    /** Last player position for cache invalidation */
    private Vector3f lastPlayerPosition;
    
    /** Distance threshold for cache invalidation */
    private static final float CACHE_INVALIDATION_DISTANCE = 2.0f;
    
    // Far terrain and impostor caches (like Distant Horizons)
    private final Map<String, FarTerrainMesh> farTerrainCache = new ConcurrentHashMap<>();
    private final Map<String, ImpostorData> impostorCache = new ConcurrentHashMap<>();
    
    // Background processing for far terrain generation
    private final ExecutorService backgroundProcessor = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
        r -> {
            Thread t = new Thread(r, "LOD-Background-Processor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    );
    
    // Disk cache directory for persistent LOD data
    private final Path cacheDirectory;
    
    // Advanced LOD features
    private final LODTransitionManager transitionManager;
    private final LODTextureAtlasManager textureAtlasManager;
    private final ProceduralDetailInjector detailInjector;
    private final boolean diskCacheEnabled;
    
    /**
     * Creates a new ChunkLOD manager with default configuration.
     */
    public ChunkLOD() {
        this(LODConfig.createDefault(), null, true);
    }
    
    /**
     * Creates a new ChunkLOD manager with specified configuration.
     * 
     * @param config the LOD configuration
     */
    public ChunkLOD(LODConfig config) {
        this(config, null, true);
    }
    
    /**
     * Creates a new ChunkLOD manager with full configuration.
     * 
     * @param config the LOD configuration to use
     * @param cacheDirectory directory for disk cache (null to disable)
     * @param enableDiskCache whether to enable disk caching
     */
    public ChunkLOD(LODConfig config, Path cacheDirectory, boolean enableDiskCache) {
        this.config = config;
        this.lodCache = new ConcurrentHashMap<>();
        this.lastPlayerPosition = new Vector3f();
        this.diskCacheEnabled = enableDiskCache && cacheDirectory != null;
        
        if (this.diskCacheEnabled) {
            this.cacheDirectory = cacheDirectory;
            try {
                Files.createDirectories(cacheDirectory);
                Files.createDirectories(cacheDirectory.resolve("far_terrain"));
                Files.createDirectories(cacheDirectory.resolve("impostors"));
            } catch (IOException e) {
                logger.error("Failed to create LOD cache directories: {}", e.getMessage());
            }
        } else {
            this.cacheDirectory = null;
        }
        
        // Initialize advanced LOD features
        this.transitionManager = new LODTransitionManager(1920, 1080, 500); // Default screen size and 500ms transitions
        this.textureAtlasManager = new LODTextureAtlasManager(null, LODTextureAtlasManager.AtlasConfig.createDefault());
        this.detailInjector = new ProceduralDetailInjector(ProceduralDetailInjector.DetailConfig.createDefault());
        
        logger.info("Initialized ChunkLOD with distances: Full={}, High={}, Medium={}, Low={}, Minimal={}, Far={}, Impostor={}",
                config.fullDetailDistance, config.highDetailDistance, config.mediumDetailDistance,
                config.lowDetailDistance, config.minimalDetailDistance, config.farTerrainDistance, config.impostorDistance);
        logger.info("Advanced LOD features enabled: Seamless transitions, Occlusion culling, Texture atlases, Procedural details");
    }
    
    /**
     * Determines the appropriate LOD level for a chunk based on player distance.
     * 
     * @param chunkPosition the chunk position
     * @param playerPosition the player position
     * @return the appropriate LOD level
     */
    public LODLevel determineLODLevel(ChunkPosition chunkPosition, Vector3f playerPosition) {
        if (chunkPosition == null || playerPosition == null) {
            return LODLevel.MINIMAL;
        }
        
        // Check cache first
        if (shouldUseCachedLOD(playerPosition)) {
            LODLevel cached = lodCache.get(chunkPosition);
            if (cached != null) {
                return cached;
            }
        }
        
        // Calculate distance from player to chunk center
        float chunkCenterX = chunkPosition.x * ChunkPosition.CHUNK_SIZE + ChunkPosition.CHUNK_SIZE / 2.0f;
        float chunkCenterZ = chunkPosition.z * ChunkPosition.CHUNK_SIZE + ChunkPosition.CHUNK_SIZE / 2.0f;
        
        float dx = playerPosition.x - chunkCenterX;
        float dz = playerPosition.z - chunkCenterZ;
        float distance = (float) Math.sqrt(dx * dx + dz * dz) / ChunkPosition.CHUNK_SIZE;
        
        // Determine LOD level based on distance
        LODLevel level;
        if (distance <= config.fullDetailDistance) {
            level = LODLevel.ULTRA;  // Map FULL to ULTRA
        } else if (distance <= config.highDetailDistance) {
            level = LODLevel.HIGH;
        } else if (distance <= config.mediumDetailDistance) {
            level = LODLevel.MEDIUM;
        } else if (distance <= config.lowDetailDistance) {
            level = LODLevel.LOW;
        } else if (distance <= config.minimalDetailDistance) {
            level = LODLevel.MINIMAL;
        } else {
            // Beyond maximum render distance - use minimal for far terrain and impostor
            level = LODLevel.MINIMAL;
        }
        
        // Cache the result
        lodCache.put(chunkPosition, level);
        
        return level;
    }
    
    /**
     * Updates the LOD system with new player position.
     * Invalidates cache if player has moved significantly.
     * 
     * @param playerPosition the new player position
     */
    public void updatePlayerPosition(Vector3f playerPosition) {
        if (playerPosition == null) {
            return;
        }
        
        // Check if player has moved significantly
        float distance = lastPlayerPosition.distance(playerPosition);
        if (distance > CACHE_INVALIDATION_DISTANCE) {
            // Clear cache for chunks that might have changed LOD level
            invalidateDistantCache(playerPosition);
            lastPlayerPosition.set(playerPosition);
        }
    }
    
    /**
     * Checks if cached LOD values should be used.
     * 
     * @param playerPosition current player position
     * @return true if cache is valid
     */
    private boolean shouldUseCachedLOD(Vector3f playerPosition) {
        return lastPlayerPosition.distance(playerPosition) <= CACHE_INVALIDATION_DISTANCE;
    }
    
    /**
     * Invalidates cache entries for chunks that might have changed LOD level.
     * 
     * @param playerPosition current player position
     */
    private void invalidateDistantCache(Vector3f playerPosition) {
        int playerChunkX = (int) Math.floor(playerPosition.x / ChunkPosition.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPosition.z / ChunkPosition.CHUNK_SIZE);
        
        // Remove cache entries for chunks that are far from the player
        lodCache.entrySet().removeIf(entry -> {
            ChunkPosition pos = entry.getKey();
            int dx = Math.abs(pos.x - playerChunkX);
            int dz = Math.abs(pos.z - playerChunkZ);
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            // Remove if beyond minimal detail distance + buffer
            return distance > config.minimalDetailDistance + 10;
        });
    }
    
    /**
     * Gets the current LOD configuration.
     * 
     * @return the LOD configuration
     */
    public LODConfig getConfig() {
        return config;
    }
    
    /**
     * Updates the LOD configuration.
     * 
     * @param config the new configuration
     */
    public void setConfig(LODConfig config) {
        if (config != null) {
            this.config = config;
            lodCache.clear(); // Clear cache as distances have changed
            logger.info("Updated ChunkLOD configuration");
        }
    }
    
    /**
     * Clears the LOD cache.
     */
    public void clearCache() {
        lodCache.clear();
        logger.debug("Cleared ChunkLOD cache");
    }
    
    /**
     * Gets the number of cached LOD entries.
     * 
     * @return cache size
     */
    public int getCacheSize() {
        return lodCache.size();
    }
    
    /**
     * Calculates the mesh complexity factor for a given LOD level.
     * This can be used to reduce vertex count or simplify geometry.
     * 
     * @param level the LOD level
     * @return complexity factor (1.0 = full complexity, 0.0 = minimal)
     */
    public static float getMeshComplexityFactor(LODLevel level) {
        switch (level) {
            case ULTRA:
                return 1.0f;
            case HIGH:
                return 0.8f;
            case MEDIUM:
                return 0.5f;
            case LOW:
                return 0.3f;
            case MINIMAL:
                return 0.1f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Determines if a chunk should use simplified meshing based on LOD level.
     * 
     * @param level the LOD level
     * @return true if simplified meshing should be used
     */
    public static boolean shouldUseSimplifiedMeshing(LODLevel level) {
        return level.getLevel() >= LODLevel.MEDIUM.getLevel();
    }
    
    /**
     * Determines if a chunk should skip small details based on LOD level.
     * 
     * @param level the LOD level
     * @return true if small details should be skipped
     */
    public static boolean shouldSkipSmallDetails(LODLevel level) {
        return level.getLevel() >= LODLevel.HIGH.getLevel();
    }
    
    /**
     * Generates far terrain mesh for a chunk in the background.
     * Similar to Distant Horizons' terrain generation.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param terrainData simplified terrain data
     */
    public void generateFarTerrainAsync(int chunkX, int chunkZ, byte[] terrainData) {
        String key = chunkX + "," + chunkZ;
        
        // Check if already cached
        if (farTerrainCache.containsKey(key)) {
            return;
        }
        
        // Submit to background thread
        backgroundProcessor.submit(() -> {
            try {
                FarTerrainMesh mesh = generateFarTerrainMesh(chunkX, chunkZ, terrainData);
                farTerrainCache.put(key, mesh);
                
                // Save to disk if enabled
                if (diskCacheEnabled) {
                    saveFarTerrainToDisk(mesh);
                }
                
                logger.debug("Generated far terrain mesh for chunk ({}, {})", chunkX, chunkZ);
            } catch (Exception e) {
                logger.error("Failed to generate far terrain for chunk ({}, {}): {}", 
                           chunkX, chunkZ, e.getMessage());
            }
        });
    }
    
    /**
     * Generates impostor data for extremely distant chunks.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param colorData simplified color information
     * @param averageHeight average terrain height
     */
    public void generateImpostorAsync(int chunkX, int chunkZ, byte[] colorData, float averageHeight) {
        String key = chunkX + "," + chunkZ;
        
        // Check if already cached
        if (impostorCache.containsKey(key)) {
            return;
        }
        
        // Submit to background thread
        backgroundProcessor.submit(() -> {
            try {
                ImpostorData impostor = new ImpostorData(chunkX, chunkZ, colorData, averageHeight);
                impostorCache.put(key, impostor);
                
                // Save to disk if enabled
                if (diskCacheEnabled) {
                    saveImpostorToDisk(impostor);
                }
                
                logger.debug("Generated impostor data for chunk ({}, {})", chunkX, chunkZ);
            } catch (Exception e) {
                logger.error("Failed to generate impostor for chunk ({}, {}): {}", 
                           chunkX, chunkZ, e.getMessage());
            }
        });
    }
    
    /**
     * Gets cached far terrain mesh for a chunk.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return far terrain mesh or null if not cached
     */
    public FarTerrainMesh getFarTerrainMesh(int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        FarTerrainMesh mesh = farTerrainCache.get(key);
        
        // Try loading from disk if not in memory
        if (mesh == null && diskCacheEnabled) {
            mesh = loadFarTerrainFromDisk(chunkX, chunkZ);
            if (mesh != null) {
                farTerrainCache.put(key, mesh);
            }
        }
        
        return mesh;
    }
    
    /**
     * Gets cached impostor data for a chunk.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return impostor data or null if not cached
     */
    public ImpostorData getImpostorData(int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        ImpostorData impostor = impostorCache.get(key);
        
        // Try loading from disk if not in memory
        if (impostor == null && diskCacheEnabled) {
            impostor = loadImpostorFromDisk(chunkX, chunkZ);
            if (impostor != null) {
                impostorCache.put(key, impostor);
            }
        }
        
        return impostor;
    }
    
    /**
     * Generates a simplified far terrain mesh from terrain data.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param terrainData simplified terrain height data
     * @return generated far terrain mesh
     */
    private FarTerrainMesh generateFarTerrainMesh(int chunkX, int chunkZ, byte[] terrainData) {
        // Simplified mesh generation - create a low-poly representation
        int resolution = 4; // 4x4 grid instead of full chunk resolution
        float[] vertices = new float[resolution * resolution * 3];
        int[] indices = new int[(resolution - 1) * (resolution - 1) * 6];
        
        // Generate vertices
        int vertexIndex = 0;
        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                float worldX = chunkX * 16 + (x * 16.0f / (resolution - 1));
                float worldZ = chunkZ * 16 + (z * 16.0f / (resolution - 1));
                float height = getHeightFromTerrainData(terrainData, x, z, resolution);
                
                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = height;
                vertices[vertexIndex++] = worldZ;
            }
        }
        
        // Generate indices for triangles
        int indexIndex = 0;
        for (int z = 0; z < resolution - 1; z++) {
            for (int x = 0; x < resolution - 1; x++) {
                int topLeft = z * resolution + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * resolution + x;
                int bottomRight = bottomLeft + 1;
                
                // First triangle
                indices[indexIndex++] = topLeft;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = topRight;
                
                // Second triangle
                indices[indexIndex++] = topRight;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = bottomRight;
            }
        }
        
        return new FarTerrainMesh(chunkX, chunkZ, vertices, indices, LODLevel.MINIMAL.getLevel());
    }
    
    /**
     * Extracts height from terrain data at given coordinates.
     * 
     * @param terrainData terrain height data
     * @param x local X coordinate
     * @param z local Z coordinate
     * @param resolution grid resolution
     * @return height value
     */
    private float getHeightFromTerrainData(byte[] terrainData, int x, int z, int resolution) {
        if (terrainData == null || terrainData.length == 0) {
            return 64.0f; // Default sea level
        }
        
        int index = z * resolution + x;
        if (index >= terrainData.length) {
            return 64.0f;
        }
        
        return (terrainData[index] & 0xFF) / 4.0f; // Convert byte to height
    }
    
    /**
     * Saves far terrain mesh to disk cache.
     * 
     * @param mesh the mesh to save
     */
    private void saveFarTerrainToDisk(FarTerrainMesh mesh) {
        if (!diskCacheEnabled || cacheDirectory == null) {
            return;
        }
        
        try {
            Path filePath = cacheDirectory.resolve("far_terrain")
                    .resolve(mesh.getChunkX() + "_" + mesh.getChunkZ() + ".dat");
            
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
                
                oos.writeInt(mesh.getChunkX());
                oos.writeInt(mesh.getChunkZ());
                oos.writeObject(mesh.getVertices());
                oos.writeObject(mesh.getIndices());
                oos.writeLong(mesh.getTimestamp());
                oos.writeInt(mesh.getLodLevel());
            }
        } catch (IOException e) {
            logger.warn("Failed to save far terrain to disk: {}", e.getMessage());
        }
    }
    
    /**
     * Saves impostor data to disk cache.
     * 
     * @param impostor the impostor data to save
     */
    private void saveImpostorToDisk(ImpostorData impostor) {
        if (!diskCacheEnabled || cacheDirectory == null) {
            return;
        }
        
        try {
            Path filePath = cacheDirectory.resolve("impostors")
                    .resolve(impostor.getChunkX() + "_" + impostor.getChunkZ() + ".dat");
            
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
                
                oos.writeInt(impostor.getChunkX());
                oos.writeInt(impostor.getChunkZ());
                oos.writeObject(impostor.getColorData());
                oos.writeFloat(impostor.getAverageHeight());
                oos.writeLong(impostor.getTimestamp());
            }
        } catch (IOException e) {
            logger.warn("Failed to save impostor to disk: {}", e.getMessage());
        }
    }
    
    /**
     * Loads far terrain mesh from disk cache.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return loaded mesh or null if not found
     */
    private FarTerrainMesh loadFarTerrainFromDisk(int chunkX, int chunkZ) {
        if (!diskCacheEnabled || cacheDirectory == null) {
            return null;
        }
        
        try {
            Path filePath = cacheDirectory.resolve("far_terrain")
                    .resolve(chunkX + "_" + chunkZ + ".dat");
            
            if (!Files.exists(filePath)) {
                return null;
            }
            
            try (FileInputStream fis = new FileInputStream(filePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                int x = ois.readInt();
                int z = ois.readInt();
                float[] vertices = (float[]) ois.readObject();
                int[] indices = (int[]) ois.readObject();
                long timestamp = ois.readLong();
                int lodLevel = ois.readInt();
                
                return new FarTerrainMesh(x, z, vertices, indices, lodLevel);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Failed to load far terrain from disk: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Loads impostor data from disk cache.
     * 
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return loaded impostor data or null if not found
     */
    private ImpostorData loadImpostorFromDisk(int chunkX, int chunkZ) {
        if (!diskCacheEnabled || cacheDirectory == null) {
            return null;
        }
        
        try {
            Path filePath = cacheDirectory.resolve("impostors")
                    .resolve(chunkX + "_" + chunkZ + ".dat");
            
            if (!Files.exists(filePath)) {
                return null;
            }
            
            try (FileInputStream fis = new FileInputStream(filePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                int x = ois.readInt();
                int z = ois.readInt();
                byte[] colorData = (byte[]) ois.readObject();
                float averageHeight = ois.readFloat();
                long timestamp = ois.readLong();
                
                return new ImpostorData(x, z, colorData, averageHeight);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Failed to load impostor from disk: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Cleans up old cache entries and shuts down background processing.
     */
    public void shutdown() {
        backgroundProcessor.shutdown();
        try {
            if (!backgroundProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("ChunkLOD system shut down");
    }
    
    /**
     * Gets the number of cached far terrain meshes.
     * 
     * @return far terrain cache size
     */
    public int getFarTerrainCacheSize() {
        return farTerrainCache.size();
    }
    
    /**
     * Gets the number of cached impostor data entries.
     * 
     * @return impostor cache size
     */
    public int getImpostorCacheSize() {
        return impostorCache.size();
    }
    
    /**
     * Clears all caches (LOD, far terrain, and impostor).
     */
    public void clearAllCaches() {
        clearCache();
        farTerrainCache.clear();
        impostorCache.clear();
        hierarchicalLODCache.clear();
        logger.info("Cleared all LOD caches");
    }
    
    // ========== TEMPORAL LOD UPDATE SYSTEM ==========
    
    /**
     * Manages temporal LOD updates to spread work across multiple frames.
     */
    public static class TemporalLODUpdater {
        private final List<ChunkPosition> pendingUpdates = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger currentUpdateIndex = new AtomicInteger(0);
        private final int updatesPerFrame;
        private final long updateIntervalMs;
        private long lastUpdateTime = 0;
        
        /**
         * Creates a temporal LOD updater.
         * 
         * @param updatesPerFrame maximum updates to process per frame
         * @param updateIntervalMs minimum interval between update cycles
         */
        public TemporalLODUpdater(int updatesPerFrame, long updateIntervalMs) {
            this.updatesPerFrame = updatesPerFrame;
            this.updateIntervalMs = updateIntervalMs;
        }
        
        /**
         * Adds a chunk position for LOD update.
         * 
         * @param position chunk position to update
         */
        public void scheduleUpdate(ChunkPosition position) {
            if (!pendingUpdates.contains(position)) {
                pendingUpdates.add(position);
            }
        }
        
        /**
         * Processes pending LOD updates for this frame.
         * 
         * @param chunkLOD the LOD system to update
         * @param playerPosition current player position
         * @return number of updates processed
         */
        public int processUpdates(ChunkLOD chunkLOD, Vector3f playerPosition) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < updateIntervalMs) {
                return 0;
            }
            
            int processed = 0;
            int startIndex = currentUpdateIndex.get();
            
            for (int i = 0; i < updatesPerFrame && !pendingUpdates.isEmpty(); i++) {
                int index = (startIndex + i) % pendingUpdates.size();
                if (index >= pendingUpdates.size()) break;
                
                ChunkPosition position = pendingUpdates.get(index);
                if (position != null) {
                    // Update LOD level for this chunk
                    chunkLOD.determineLODLevel(position, playerPosition);
                    processed++;
                }
            }
            
            // Remove processed updates
            if (processed > 0) {
                int endIndex = Math.min(startIndex + processed, pendingUpdates.size());
                pendingUpdates.subList(startIndex, endIndex).clear();
                currentUpdateIndex.set(0);
            }
            
            lastUpdateTime = currentTime;
            return processed;
        }
        
        /**
         * Gets the number of pending updates.
         * 
         * @return pending update count
         */
        public int getPendingUpdateCount() {
            return pendingUpdates.size();
        }
        
        /**
         * Clears all pending updates.
         */
        public void clearPendingUpdates() {
            pendingUpdates.clear();
            currentUpdateIndex.set(0);
        }
    }
    
    // ========== PERFORMANCE MONITORING SYSTEM ==========
    
    /**
     * Monitors LOD system performance and provides auto-adjustment capabilities.
     */
    public static class LODPerformanceMonitor {
        private final AtomicLong totalLODCalculations = new AtomicLong(0);
        private final AtomicLong totalLODTime = new AtomicLong(0);
        private final AtomicLong frameCount = new AtomicLong(0);
        private final AtomicLong lastFrameTime = new AtomicLong(System.nanoTime());
        private final List<Long> frameTimes = Collections.synchronizedList(new ArrayList<>());
        private final int maxFrameHistory = 60; // Keep 60 frames of history
        
        private volatile boolean autoAdjustmentEnabled = true;
        private volatile float targetFrameTime = 16.67f; // 60 FPS target
        private volatile float lodDistanceMultiplier = 1.0f;
        
        /**
         * Records the start of a LOD calculation.
         * 
         * @return start time in nanoseconds
         */
        public long startLODCalculation() {
            return System.nanoTime();
        }
        
        /**
         * Records the end of a LOD calculation.
         * 
         * @param startTime start time from startLODCalculation()
         */
        public void endLODCalculation(long startTime) {
            long duration = System.nanoTime() - startTime;
            totalLODCalculations.incrementAndGet();
            totalLODTime.addAndGet(duration);
        }
        
        /**
         * Records frame timing for performance monitoring.
         */
        public void recordFrame() {
            long currentTime = System.nanoTime();
            long lastTime = lastFrameTime.getAndSet(currentTime);
            long frameTime = currentTime - lastTime;
            
            synchronized (frameTimes) {
                frameTimes.add(frameTime);
                if (frameTimes.size() > maxFrameHistory) {
                    frameTimes.remove(0);
                }
            }
            
            frameCount.incrementAndGet();
            
            // Auto-adjust LOD distances if enabled
            if (autoAdjustmentEnabled && frameCount.get() % 60 == 0) {
                adjustLODDistances();
            }
        }
        
        /**
         * Automatically adjusts LOD distances based on performance.
         */
        private void adjustLODDistances() {
            float avgFrameTime = getAverageFrameTime();
            
            if (avgFrameTime > targetFrameTime * 1.2f) {
                // Performance is poor, reduce LOD distances
                lodDistanceMultiplier = Math.max(0.5f, lodDistanceMultiplier * 0.95f);
                logger.info("LOD performance adjustment: reducing distances (multiplier: {})", 
                           lodDistanceMultiplier);
            } else if (avgFrameTime < targetFrameTime * 0.8f) {
                // Performance is good, increase LOD distances
                lodDistanceMultiplier = Math.min(2.0f, lodDistanceMultiplier * 1.05f);
                logger.info("LOD performance adjustment: increasing distances (multiplier: {})", 
                           lodDistanceMultiplier);
            }
        }
        
        /**
         * Gets the average frame time in milliseconds.
         * 
         * @return average frame time
         */
        public float getAverageFrameTime() {
            synchronized (frameTimes) {
                if (frameTimes.isEmpty()) {
                    return 0.0f;
                }
                
                long total = 0;
                for (Long frameTime : frameTimes) {
                    total += frameTime;
                }
                
                return (total / frameTimes.size()) / 1_000_000.0f; // Convert to milliseconds
            }
        }
        
        /**
         * Gets the average LOD calculation time in microseconds.
         * 
         * @return average LOD calculation time
         */
        public double getAverageLODTime() {
            long calculations = totalLODCalculations.get();
            if (calculations == 0) {
                return 0.0;
            }
            
            return (totalLODTime.get() / calculations) / 1000.0; // Convert to microseconds
        }
        
        /**
         * Gets comprehensive performance statistics.
         * 
         * @return performance stats as formatted string
         */
        public String getPerformanceStats() {
            return String.format(
                "LOD Performance: Avg Frame: %.2fms, Avg LOD Calc: %.2fμs, " +
                "Total Calculations: %d, Distance Multiplier: %.2f",
                getAverageFrameTime(),
                getAverageLODTime(),
                totalLODCalculations.get(),
                lodDistanceMultiplier
            );
        }
        
        /**
         * Gets the current LOD distance multiplier.
         * 
         * @return distance multiplier
         */
        public float getLODDistanceMultiplier() {
            return lodDistanceMultiplier;
        }
        
        /**
         * Sets whether auto-adjustment is enabled.
         * 
         * @param enabled true to enable auto-adjustment
         */
        public void setAutoAdjustmentEnabled(boolean enabled) {
            this.autoAdjustmentEnabled = enabled;
        }
        
        /**
         * Sets the target frame time for auto-adjustment.
         * 
         * @param targetFrameTime target frame time in milliseconds
         */
        public void setTargetFrameTime(float targetFrameTime) {
            this.targetFrameTime = targetFrameTime;
        }
        
        /**
         * Resets all performance statistics.
         */
        public void reset() {
            totalLODCalculations.set(0);
            totalLODTime.set(0);
            frameCount.set(0);
            synchronized (frameTimes) {
                frameTimes.clear();
            }
            lodDistanceMultiplier = 1.0f;
        }
    }
    
    // ========== INSTANCE FIELDS FOR NEW SYSTEMS ==========
    
    /** Hierarchical LOD cache for chunk aggregation */
    private final LODMeshCache hierarchicalLODCache = new LODMeshCache(1000);
    
    /** Temporal LOD updater for spreading work across frames */
    private final TemporalLODUpdater temporalUpdater = new TemporalLODUpdater(10, 16);
    
    /** Performance monitor for auto-adjustment */
    private final LODPerformanceMonitor performanceMonitor = new LODPerformanceMonitor();
    
    // ========== NEW PUBLIC METHODS ==========
    
    /**
     * Gets a hierarchical LOD region for the specified area.
     * 
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @param regionSize size in chunks (power of 2)
     * @param playerPosition current player position
     * @return hierarchical LOD region or null if not available
     */
    public HierarchicalLODRegion getHierarchicalLODRegion(int regionX, int regionZ, 
                                                         int regionSize, Vector3f playerPosition) {
        String regionKey = regionX + "_" + regionZ + "_" + regionSize;
        
        // Check cache first
        HierarchicalLODRegion cached = hierarchicalLODCache.getCachedRegion(regionKey);
        if (cached != null) {
            return cached;
        }
        
        // Generate new region asynchronously
        generateHierarchicalLODRegionAsync(regionX, regionZ, regionSize, playerPosition);
        
        return null; // Will be available in cache after generation
    }
    
    /**
     * Generates a hierarchical LOD region asynchronously.
     * 
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @param regionSize size in chunks
     * @param playerPosition current player position
     */
    private void generateHierarchicalLODRegionAsync(int regionX, int regionZ, 
                                                   int regionSize, Vector3f playerPosition) {
        backgroundProcessor.submit(() -> {
            try {
                // Calculate distance to determine LOD level
                float centerX = (regionX + regionSize / 2.0f) * ChunkPosition.CHUNK_SIZE;
                float centerZ = (regionZ + regionSize / 2.0f) * ChunkPosition.CHUNK_SIZE;
                float distance = (float) Math.sqrt(
                    Math.pow(playerPosition.x - centerX, 2) + 
                    Math.pow(playerPosition.z - centerZ, 2)
                ) / ChunkPosition.CHUNK_SIZE;
                
                LODLevel lodLevel = determineLODLevelForDistance(distance);
                
                // Generate simplified mesh for the region
                float[] vertices = generateRegionVertices(regionX, regionZ, regionSize, lodLevel);
                int[] indices = generateRegionIndices(regionSize, lodLevel);
                byte[] compressedData = compressRegionTerrain(regionX, regionZ, regionSize);
                
                HierarchicalLODRegion region = new HierarchicalLODRegion(
                    regionX, regionZ, regionSize, lodLevel, vertices, indices, compressedData
                );
                
                String regionKey = regionX + "_" + regionZ + "_" + regionSize;
                hierarchicalLODCache.cacheRegion(regionKey, region);
                
            } catch (Exception e) {
                logger.error("Failed to generate hierarchical LOD region: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Determines LOD level based on distance.
     * 
     * @param distance distance from player
     * @return appropriate LOD level
     */
    private LODLevel determineLODLevelForDistance(float distance) {
        float multiplier = performanceMonitor.getLODDistanceMultiplier();
        
        if (distance <= config.fullDetailDistance * multiplier) {
            return LODLevel.ULTRA;
        } else if (distance <= config.highDetailDistance * multiplier) {
            return LODLevel.HIGH;
        } else if (distance <= config.mediumDetailDistance * multiplier) {
            return LODLevel.MEDIUM;
        } else if (distance <= config.lowDetailDistance * multiplier) {
            return LODLevel.LOW;
        } else if (distance <= config.minimalDetailDistance * multiplier) {
            return LODLevel.MINIMAL;
        } else if (distance <= config.farTerrainDistance * multiplier) {
            return LODLevel.MINIMAL; // Use MINIMAL for far terrain
        } else {
            return LODLevel.MINIMAL; // Use MINIMAL for impostor distances
        }
    }
    
    /**
     * Generates vertices for a hierarchical LOD region.
     * 
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @param regionSize size in chunks
     * @param lodLevel LOD level to use
     * @return vertex array
     */
    private float[] generateRegionVertices(int regionX, int regionZ, int regionSize, LODLevel lodLevel) {
        int resolution = Math.max(2, 16 / Math.max(1, lodLevel.getLevel() + 1));
        List<Float> vertexList = new ArrayList<>();
        
        for (int z = 0; z <= resolution; z++) {
            for (int x = 0; x <= resolution; x++) {
                float worldX = regionX * regionSize * ChunkPosition.CHUNK_SIZE + 
                              (x * regionSize * ChunkPosition.CHUNK_SIZE) / resolution;
                float worldZ = regionZ * regionSize * ChunkPosition.CHUNK_SIZE + 
                              (z * regionSize * ChunkPosition.CHUNK_SIZE) / resolution;
                float height = 64.0f; // Simplified height calculation
                
                vertexList.add(worldX);
                vertexList.add(height);
                vertexList.add(worldZ);
            }
        }
        
        float[] vertices = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            vertices[i] = vertexList.get(i);
        }
        return vertices;
    }
    
    /**
     * Generates indices for a hierarchical LOD region.
     * 
     * @param regionSize size in chunks
     * @param lodLevel LOD level to use
     * @return index array
     */
    private int[] generateRegionIndices(int regionSize, LODLevel lodLevel) {
        int resolution = Math.max(2, 16 / Math.max(1, lodLevel.getLevel() + 1));
        List<Integer> indexList = new ArrayList<>();
        
        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                int topLeft = z * (resolution + 1) + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * (resolution + 1) + x;
                int bottomRight = bottomLeft + 1;
                
                // First triangle
                indexList.add(topLeft);
                indexList.add(bottomLeft);
                indexList.add(topRight);
                
                // Second triangle
                indexList.add(topRight);
                indexList.add(bottomLeft);
                indexList.add(bottomRight);
            }
        }
        
        return indexList.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Compresses terrain data for a region.
     * 
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @param regionSize size in chunks
     * @return compressed terrain data
     */
    private byte[] compressRegionTerrain(int regionX, int regionZ, int regionSize) {
        // Simplified compression - in a real implementation, this would
        // aggregate actual chunk data and compress it
        byte[] data = new byte[regionSize * regionSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (64 + (i % 32)); // Simplified height data
        }
        return data;
    }
    
    /**
     * Processes temporal LOD updates for this frame.
     * 
     * @param playerPosition current player position
     * @return number of updates processed
     */
    public int processTemporalUpdates(Vector3f playerPosition) {
        performanceMonitor.recordFrame();
        return temporalUpdater.processUpdates(this, playerPosition);
    }
    
    /**
     * Schedules a chunk for temporal LOD update.
     * 
     * @param position chunk position to update
     */
    public void scheduleTemporalUpdate(ChunkPosition position) {
        temporalUpdater.scheduleUpdate(position);
    }
    
    /**
     * Gets the performance monitor for this LOD system.
     * 
     * @return performance monitor
     */
    public LODPerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Gets hierarchical LOD cache statistics.
     * 
     * @return cache statistics
     */
    public double[] getHierarchicalCacheStats() {
        return hierarchicalLODCache.getCacheStats();
    }
    
    // Advanced LOD feature access methods
    
    /**
     * Gets the LOD transition manager for seamless transitions.
     * 
     * @return transition manager
     */
    public LODTransitionManager getTransitionManager() {
        return transitionManager;
    }
    
    /**
     * Gets the texture atlas manager for distance-based texture optimization.
     * 
     * @return texture atlas manager
     */
    public LODTextureAtlasManager getTextureAtlasManager() {
        return textureAtlasManager;
    }
    
    /**
     * Gets the procedural detail injector for adding surface details.
     * 
     * @return procedural detail injector
     */
    public ProceduralDetailInjector getProceduralDetailInjector() {
        return detailInjector;
    }
    
    /**
     * Updates the view-projection matrix for occlusion culling.
     * 
     * @param viewMatrix the view matrix
     * @param projectionMatrix the projection matrix
     */
    public void updateViewProjectionMatrix(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        transitionManager.updateViewProjectionMatrix(viewMatrix, projectionMatrix);
    }
    
    /**
     * Tests if a chunk is occluded and can be culled.
     * 
     * @param chunkPosition the chunk position
     * @param chunkSize the chunk size in world units
     * @param chunkHeight the chunk height
     * @return true if the chunk is occluded
     */
    public boolean isChunkOccluded(ChunkPosition chunkPosition, float chunkSize, float chunkHeight) {
        return transitionManager.isChunkOccluded(chunkPosition, chunkSize, chunkHeight);
    }
    
    /**
     * Updates the occlusion buffer with rendered depth data.
     * 
     * @param depthBuffer the depth buffer from rendering
     */
    public void updateOcclusionBuffer(float[] depthBuffer) {
        transitionManager.updateOcclusionBuffer(depthBuffer);
    }
    
    /**
     * Starts a LOD transition for a chunk.
     * 
     * @param chunkPosition the chunk position
     * @param fromLevel the current LOD level
     * @param toLevel the target LOD level
     * @return the created transition
     */
    public LODTransitionManager.LODTransition startLODTransition(ChunkPosition chunkPosition, 
                                                               LODLevel fromLevel, 
                                                               LODLevel toLevel) {
        return transitionManager.startTransition(chunkPosition, fromLevel, toLevel);
    }
    
    /**
     * Gets the active transition for a chunk.
     * 
     * @param chunkPosition the chunk position
     * @return the active transition or null if none
     */
    public LODTransitionManager.LODTransition getActiveTransition(ChunkPosition chunkPosition) {
        return transitionManager.getActiveTransition(chunkPosition);
    }
    
    /**
     * Updates all active LOD transitions.
     * 
     * @return number of active transitions
     */
    public int updateLODTransitions() {
        return transitionManager.updateTransitions();
    }
    
    /**
     * Gets the appropriate texture atlas level for a chunk at a given distance.
     * 
     * @param atlasName atlas name
     * @param distance viewing distance
     * @return the appropriate atlas level or null if atlas not found
     */
    public LODTextureAtlasManager.TextureAtlas.AtlasLevel getTextureAtlasForDistance(String atlasName, float distance) {
        return textureAtlasManager.getAtlasLevelForDistance(atlasName, distance);
    }
    
    /**
     * Gets the appropriate texture atlas level for a specific LOD level.
     * 
     * @param atlasName atlas name
     * @param lodLevel LOD level
     * @return the appropriate atlas level or null if atlas not found
     */
    public LODTextureAtlasManager.TextureAtlas.AtlasLevel getTextureAtlasForLOD(String atlasName, LODLevel lodLevel) {
        return textureAtlasManager.getAtlasLevelForLOD(atlasName, lodLevel);
    }
    
    /**
     * Generates procedural details for a chunk.
     * 
     * @param chunkPosition chunk position
     * @param terrainData terrain height data
     * @param chunkSize chunk size in world units
     * @param lodLevel current LOD level
     * @return list of procedural details
     */
    public List<ProceduralDetailInjector.ProceduralDetail> generateProceduralDetails(ChunkPosition chunkPosition, 
                                                                                    float[][] terrainData, 
                                                                                    float chunkSize, 
                                                                                    LODLevel lodLevel) {
        return detailInjector.generateDetails(chunkPosition, terrainData, chunkSize, lodLevel);
    }
    
    /**
     * Gets comprehensive LOD system statistics.
     * 
     * @return formatted statistics string
     */
    public String getLODSystemStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== LOD System Statistics ===\n");
        stats.append(String.format("Cache Size: %d entries\n", getCacheSize()));
        stats.append(String.format("Far Terrain Cache: %d entries\n", getFarTerrainCacheSize()));
        stats.append(String.format("Impostor Cache: %d entries\n", getImpostorCacheSize()));
        stats.append(String.format("Active Transitions: %d\n", transitionManager.getActiveTransitionCount()));
        stats.append(String.format("Texture Atlas Memory: %s\n", textureAtlasManager.getMemoryUsageStats()));
        stats.append(String.format("Procedural Details Cached: %d chunks, %d total details\n", 
                                 detailInjector.getCachedChunkCount(), detailInjector.getTotalCachedDetails()));
        
        double[] hierarchicalStats = getHierarchicalCacheStats();
        stats.append(String.format("Hierarchical Cache: %.0f hits, %.0f misses, %.0f entries, %.1f%% hit rate\n",
                                 hierarchicalStats[0], hierarchicalStats[1], hierarchicalStats[2], hierarchicalStats[3] * 100));
        
        return stats.toString();
    }
}