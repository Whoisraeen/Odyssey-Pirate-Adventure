package com.odyssey.world.chunk;

import org.joml.Vector3f;
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

/**
 * Manages Level of Detail (LOD) for chunks based on distance from player.
 * Provides different rendering qualities to optimize performance for distant chunks.
 * 
 * @author Odyssey Team
 * @since 1.0.0
 */
public class ChunkLOD {
    private static final Logger logger = LoggerFactory.getLogger(ChunkLOD.class);
    
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
    
    /**
     * Different levels of detail for chunk rendering.
     */
    public enum LODLevel {
        /** Full detail - all blocks rendered with full geometry */
        FULL(0, 1.0f, "Full Detail", 1),
        
        /** High detail - minor optimizations, some small details removed */
        HIGH(1, 0.8f, "High Detail", 1),
        
        /** Medium detail - noticeable simplification, merged geometry */
        MEDIUM(2, 0.5f, "Medium Detail", 2),
        
        /** Low detail - heavily simplified, impostor-like rendering */
        LOW(3, 0.3f, "Low Detail", 4),
        
        /** Minimal detail - basic shape only, used for very distant chunks */
        MINIMAL(4, 0.1f, "Minimal Detail", 8),
        
        /** Far terrain - ultra-simplified mesh for extreme distances */
        FAR_TERRAIN(5, 0.05f, "Far Terrain", 16),
        
        /** Impostor - 2D billboard representation for horizon chunks */
        IMPOSTOR(6, 0.01f, "Impostor", 32);
        
        private final int level;
        private final float qualityFactor;
        private final String description;
        private final int blockReductionFactor;
        
        LODLevel(int level, float qualityFactor, String description, int blockReductionFactor) {
            this.level = level;
            this.qualityFactor = qualityFactor;
            this.description = description;
            this.blockReductionFactor = blockReductionFactor;
        }
        
        /**
         * Gets the numeric level (0 = highest quality).
         * 
         * @return the LOD level
         */
        public int getLevel() {
            return level;
        }
        
        /**
         * Gets the quality factor (1.0 = full quality, 0.0 = no quality).
         * 
         * @return the quality factor
         */
        public float getQualityFactor() {
            return qualityFactor;
        }
        
        /**
         * Gets the human-readable description.
         * 
         * @return the description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the block reduction factor for mesh simplification.
         * Higher values mean more aggressive simplification.
         * 
         * @return the block reduction factor
         */
        public int getBlockReductionFactor() {
            return blockReductionFactor;
        }
    }
    
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
        
        logger.info("Initialized ChunkLOD with distances: Full={}, High={}, Medium={}, Low={}, Minimal={}",
                config.fullDetailDistance, config.highDetailDistance, config.mediumDetailDistance,
                config.lowDetailDistance, config.minimalDetailDistance);
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
            level = LODLevel.FULL;
        } else if (distance <= config.highDetailDistance) {
            level = LODLevel.HIGH;
        } else if (distance <= config.mediumDetailDistance) {
            level = LODLevel.MEDIUM;
        } else if (distance <= config.lowDetailDistance) {
            level = LODLevel.LOW;
        } else if (distance <= config.minimalDetailDistance) {
            level = LODLevel.MINIMAL;
        } else if (distance <= config.farTerrainDistance) {
            level = LODLevel.FAR_TERRAIN;
        } else if (distance <= config.impostorDistance) {
            level = LODLevel.IMPOSTOR;
        } else {
            // Beyond maximum render distance
            return null;
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
            case FULL:
                return 1.0f;
            case HIGH:
                return 0.8f;
            case MEDIUM:
                return 0.5f;
            case LOW:
                return 0.3f;
            case MINIMAL:
                return 0.1f;
            case FAR_TERRAIN:
                return 0.05f;
            case IMPOSTOR:
                return 0.01f;
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
        
        return new FarTerrainMesh(chunkX, chunkZ, vertices, indices, LODLevel.FAR_TERRAIN.getLevel());
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
     * Clears all caches including far terrain and impostor data.
     */
    public void clearAllCaches() {
        lodCache.clear();
        farTerrainCache.clear();
        impostorCache.clear();
        logger.debug("Cleared all LOD caches");
    }
}