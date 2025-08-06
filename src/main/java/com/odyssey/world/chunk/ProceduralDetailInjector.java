package com.odyssey.world.chunk;

import org.joml.Vector3f;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import com.odyssey.world.chunk.LODTextureAtlasManager.LODLevel;

/**
 * Injects procedural details into LOD meshes to maintain visual richness
 * even at lower detail levels. Uses noise functions and procedural generation
 * to add surface details, vegetation, and other features.
 * 
 * @author Odyssey Team
 * @since 1.0.0
 */
public class ProceduralDetailInjector {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralDetailInjector.class);
    
    /**
     * Represents a procedural detail element.
     */
    public static class ProceduralDetail {
        private final Vector3f position;
        private final Vector3f normal;
        private final float scale;
        private final float rotation;
        private final DetailType type;
        private final int variant;
        private final float density;
        
        /**
         * Types of procedural details.
         */
        public enum DetailType {
            GRASS(0.1f, 0.5f),
            SMALL_ROCK(0.2f, 1.0f),
            FLOWER(0.05f, 0.3f),
            SMALL_BUSH(0.3f, 0.8f),
            SURFACE_CRACK(0.0f, 0.1f),
            MOSS_PATCH(0.0f, 0.2f),
            PEBBLE(0.05f, 0.2f),
            TWIG(0.1f, 0.4f);
            
            private final float minScale;
            private final float maxScale;
            
            DetailType(float minScale, float maxScale) {
                this.minScale = minScale;
                this.maxScale = maxScale;
            }
            
            public float getMinScale() { return minScale; }
            public float getMaxScale() { return maxScale; }
        }
        
        /**
         * Creates a new procedural detail.
         * 
         * @param position world position
         * @param normal surface normal
         * @param scale detail scale
         * @param rotation rotation in radians
         * @param type detail type
         * @param variant variant index
         * @param density local density factor
         */
        public ProceduralDetail(Vector3f position, Vector3f normal, float scale, 
                              float rotation, DetailType type, int variant, float density) {
            this.position = new Vector3f(position);
            this.normal = new Vector3f(normal);
            this.scale = scale;
            this.rotation = rotation;
            this.type = type;
            this.variant = variant;
            this.density = density;
        }
        
        public Vector3f getPosition() { return new Vector3f(position); }
        public Vector3f getNormal() { return new Vector3f(normal); }
        public float getScale() { return scale; }
        public float getRotation() { return rotation; }
        public DetailType getType() { return type; }
        public int getVariant() { return variant; }
        public float getDensity() { return density; }
    }
    
    /**
     * Configuration for procedural detail generation.
     */
    public static class DetailConfig {
        private final Map<ProceduralDetail.DetailType, Float> typeDensities = new ConcurrentHashMap<>();
        private final float globalDensityMultiplier;
        private final float minSurfaceSlope;
        private final float maxSurfaceSlope;
        private final int maxDetailsPerChunk;
        private final float noiseScale;
        private final int noiseSeed;
        private final boolean enableBiomeVariation;
        
        /**
         * Creates detail configuration.
         * 
         * @param globalDensityMultiplier global density multiplier
         * @param minSurfaceSlope minimum surface slope for detail placement
         * @param maxSurfaceSlope maximum surface slope for detail placement
         * @param maxDetailsPerChunk maximum details per chunk
         * @param noiseScale noise scale for detail distribution
         * @param noiseSeed noise seed
         * @param enableBiomeVariation whether to enable biome-based variation
         */
        public DetailConfig(float globalDensityMultiplier, float minSurfaceSlope, 
                          float maxSurfaceSlope, int maxDetailsPerChunk, 
                          float noiseScale, int noiseSeed, boolean enableBiomeVariation) {
            this.globalDensityMultiplier = globalDensityMultiplier;
            this.minSurfaceSlope = minSurfaceSlope;
            this.maxSurfaceSlope = maxSurfaceSlope;
            this.maxDetailsPerChunk = maxDetailsPerChunk;
            this.noiseScale = noiseScale;
            this.noiseSeed = noiseSeed;
            this.enableBiomeVariation = enableBiomeVariation;
            
            // Set default densities
            setTypeDensity(ProceduralDetail.DetailType.GRASS, 0.8f);
            setTypeDensity(ProceduralDetail.DetailType.SMALL_ROCK, 0.3f);
            setTypeDensity(ProceduralDetail.DetailType.FLOWER, 0.2f);
            setTypeDensity(ProceduralDetail.DetailType.SMALL_BUSH, 0.1f);
            setTypeDensity(ProceduralDetail.DetailType.SURFACE_CRACK, 0.15f);
            setTypeDensity(ProceduralDetail.DetailType.MOSS_PATCH, 0.25f);
            setTypeDensity(ProceduralDetail.DetailType.PEBBLE, 0.4f);
            setTypeDensity(ProceduralDetail.DetailType.TWIG, 0.2f);
        }
        
        /**
         * Creates default detail configuration.
         * 
         * @return default configuration
         */
        public static DetailConfig createDefault() {
            return new DetailConfig(
                1.0f, // Global density multiplier
                0.0f, // Min surface slope
                0.7f, // Max surface slope (about 35 degrees)
                500,  // Max details per chunk
                0.1f, // Noise scale
                12345, // Noise seed
                true  // Enable biome variation
            );
        }
        
        /**
         * Sets the density for a specific detail type.
         * 
         * @param type detail type
         * @param density density value (0.0 to 1.0)
         */
        public void setTypeDensity(ProceduralDetail.DetailType type, float density) {
            typeDensities.put(type, Math.max(0.0f, Math.min(1.0f, density)));
        }
        
        /**
         * Gets the density for a specific detail type.
         * 
         * @param type detail type
         * @return density value
         */
        public float getTypeDensity(ProceduralDetail.DetailType type) {
            return typeDensities.getOrDefault(type, 0.0f);
        }
        
        public float getGlobalDensityMultiplier() { return globalDensityMultiplier; }
        public float getMinSurfaceSlope() { return minSurfaceSlope; }
        public float getMaxSurfaceSlope() { return maxSurfaceSlope; }
        public int getMaxDetailsPerChunk() { return maxDetailsPerChunk; }
        public float getNoiseScale() { return noiseScale; }
        public int getNoiseSeed() { return noiseSeed; }
        public boolean isEnableBiomeVariation() { return enableBiomeVariation; }
    }
    
    /**
     * Simple noise generator for procedural detail placement.
     */
    public static class SimpleNoise {
        private final int seed;
        private final float scale;
        
        /**
         * Creates a simple noise generator.
         * 
         * @param seed noise seed
         * @param scale noise scale
         */
        public SimpleNoise(int seed, float scale) {
            this.seed = seed;
            this.scale = scale;
        }
        
        /**
         * Generates 2D noise value.
         * 
         * @param x X coordinate
         * @param y Y coordinate
         * @return noise value between -1.0 and 1.0
         */
        public float noise2D(float x, float y) {
            x *= scale;
            y *= scale;
            
            // Simple hash-based noise
            int xi = (int) Math.floor(x);
            int yi = (int) Math.floor(y);
            
            float xf = x - xi;
            float yf = y - yi;
            
            // Smooth interpolation
            float u = fade(xf);
            float v = fade(yf);
            
            // Hash coordinates
            int aa = hash(xi, yi);
            int ab = hash(xi, yi + 1);
            int ba = hash(xi + 1, yi);
            int bb = hash(xi + 1, yi + 1);
            
            // Interpolate
            float x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
            float x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
            
            return lerp(x1, x2, v);
        }
        
        /**
         * Generates 3D noise value.
         * 
         * @param x X coordinate
         * @param y Y coordinate
         * @param z Z coordinate
         * @return noise value between -1.0 and 1.0
         */
        public float noise3D(float x, float y, float z) {
            // Simplified 3D noise using 2D noise layers
            float noise1 = noise2D(x, y);
            float noise2 = noise2D(x + 100, z);
            float noise3 = noise2D(y + 200, z + 300);
            
            return (noise1 + noise2 + noise3) / 3.0f;
        }
        
        private int hash(int x, int y) {
            int h = seed;
            h = h * 31 + x;
            h = h * 31 + y;
            h = ((h >> 16) ^ h) * 0x45d9f3b;
            h = ((h >> 16) ^ h) * 0x45d9f3b;
            h = (h >> 16) ^ h;
            return h;
        }
        
        private float fade(float t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
        
        private float lerp(float a, float b, float t) {
            return a + t * (b - a);
        }
        
        private float grad(int hash, float x, float y) {
            int h = hash & 15;
            float u = h < 8 ? x : y;
            float v = h < 4 ? y : h == 12 || h == 14 ? x : 0;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
    
    // Instance fields
    private final DetailConfig config;
    private final SimpleNoise densityNoise;
    private final SimpleNoise typeNoise;
    private final SimpleNoise scaleNoise;
    private final Map<ChunkPosition, List<ProceduralDetail>> detailCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new procedural detail injector.
     * 
     * @param config detail configuration
     */
    public ProceduralDetailInjector(DetailConfig config) {
        this.config = config;
        this.densityNoise = new SimpleNoise(config.getNoiseSeed(), config.getNoiseScale());
        this.typeNoise = new SimpleNoise(config.getNoiseSeed() + 1, config.getNoiseScale() * 0.5f);
        this.scaleNoise = new SimpleNoise(config.getNoiseSeed() + 2, config.getNoiseScale() * 2.0f);
        
        logger.info("Initialized procedural detail injector with {} max details per chunk", 
                   config.getMaxDetailsPerChunk());
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
    public List<ProceduralDetail> generateDetails(ChunkPosition chunkPosition, 
                                                float[][] terrainData, 
                                                float chunkSize, 
                                                LODLevel lodLevel) {
        // Check cache first
        List<ProceduralDetail> cachedDetails = detailCache.get(chunkPosition);
        if (cachedDetails != null) {
            return new ArrayList<>(cachedDetails);
        }
        
        List<ProceduralDetail> details = new ArrayList<>();
        
        // Adjust detail density based on LOD level
        float lodDensityMultiplier = getLODDensityMultiplier(lodLevel);
        if (lodDensityMultiplier <= 0.0f) {
            return details; // No details for very low LOD levels
        }
        
        int terrainWidth = terrainData.length;
        int terrainHeight = terrainData[0].length;
        
        float worldX = chunkPosition.x * chunkSize;
        float worldZ = chunkPosition.z * chunkSize;
        
        Random random = new Random(chunkPosition.hashCode() ^ config.getNoiseSeed());
        
        // Calculate number of detail attempts based on LOD and configuration
        int maxAttempts = (int) (config.getMaxDetailsPerChunk() * lodDensityMultiplier);
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Random position within chunk
            float localX = random.nextFloat() * chunkSize;
            float localZ = random.nextFloat() * chunkSize;
            
            float globalX = worldX + localX;
            float globalZ = worldZ + localZ;
            
            // Sample terrain height and normal
            Vector3f position = sampleTerrainPosition(terrainData, localX, localZ, chunkSize);
            if (position == null) {
                continue;
            }
            
            position.x += worldX;
            position.z += worldZ;
            
            Vector3f normal = calculateSurfaceNormal(terrainData, localX, localZ, chunkSize);
            
            // Check surface slope
            float slope = 1.0f - normal.y; // 0 = flat, 1 = vertical
            if (slope < config.getMinSurfaceSlope() || slope > config.getMaxSurfaceSlope()) {
                continue;
            }
            
            // Sample density noise
            float densityValue = densityNoise.noise2D(globalX, globalZ);
            float densityThreshold = (densityValue + 1.0f) * 0.5f; // Convert to 0-1 range
            
            if (densityThreshold < 0.3f) {
                continue; // Skip low-density areas
            }
            
            // Determine detail type based on noise and biome
            ProceduralDetail.DetailType detailType = selectDetailType(globalX, globalZ, slope, densityThreshold);
            if (detailType == null) {
                continue;
            }
            
            // Generate detail properties
            float scaleNoise = this.scaleNoise.noise2D(globalX, globalZ);
            float normalizedScale = (scaleNoise + 1.0f) * 0.5f;
            float scale = detailType.getMinScale() + 
                         (detailType.getMaxScale() - detailType.getMinScale()) * normalizedScale;
            
            float rotation = random.nextFloat() * (float) (Math.PI * 2);
            int variant = random.nextInt(4); // 4 variants per type
            
            ProceduralDetail detail = new ProceduralDetail(
                position, normal, scale, rotation, detailType, variant, densityThreshold
            );
            
            details.add(detail);
        }
        
        // Cache the generated details
        detailCache.put(chunkPosition, new ArrayList<>(details));
        
        logger.debug("Generated {} procedural details for chunk {} at LOD {}", 
                    details.size(), chunkPosition, lodLevel);
        
        return details;
    }
    
    /**
     * Gets the density multiplier for a specific LOD level.
     * 
     * @param lodLevel LOD level
     * @return density multiplier
     */
    private float getLODDensityMultiplier(LODLevel lodLevel) {
        switch (lodLevel) {
            case ULTRA:
                return 1.0f;
            case HIGH:
                return 0.8f;
            case MEDIUM:
                return 0.5f;
            case LOW:
                return 0.2f;
            case MINIMAL:
                return 0.1f;
            default:
                return 0.3f;
        }
    }
    
    /**
     * Samples terrain position at local coordinates.
     * 
     * @param terrainData terrain height data
     * @param localX local X coordinate
     * @param localZ local Z coordinate
     * @param chunkSize chunk size
     * @return world position or null if invalid
     */
    private Vector3f sampleTerrainPosition(float[][] terrainData, float localX, float localZ, float chunkSize) {
        int terrainWidth = terrainData.length;
        int terrainHeight = terrainData[0].length;
        
        // Convert to terrain array coordinates
        float terrainX = (localX / chunkSize) * (terrainWidth - 1);
        float terrainZ = (localZ / chunkSize) * (terrainHeight - 1);
        
        int x0 = (int) Math.floor(terrainX);
        int z0 = (int) Math.floor(terrainZ);
        int x1 = Math.min(x0 + 1, terrainWidth - 1);
        int z1 = Math.min(z0 + 1, terrainHeight - 1);
        
        if (x0 < 0 || z0 < 0 || x0 >= terrainWidth || z0 >= terrainHeight) {
            return null;
        }
        
        // Bilinear interpolation
        float fx = terrainX - x0;
        float fz = terrainZ - z0;
        
        float h00 = terrainData[x0][z0];
        float h10 = terrainData[x1][z0];
        float h01 = terrainData[x0][z1];
        float h11 = terrainData[x1][z1];
        
        float h0 = h00 + (h10 - h00) * fx;
        float h1 = h01 + (h11 - h01) * fx;
        float height = h0 + (h1 - h0) * fz;
        
        return new Vector3f(localX, height, localZ);
    }
    
    /**
     * Calculates surface normal at local coordinates.
     * 
     * @param terrainData terrain height data
     * @param localX local X coordinate
     * @param localZ local Z coordinate
     * @param chunkSize chunk size
     * @return surface normal
     */
    private Vector3f calculateSurfaceNormal(float[][] terrainData, float localX, float localZ, float chunkSize) {
        int terrainWidth = terrainData.length;
        int terrainHeight = terrainData[0].length;
        
        float terrainX = (localX / chunkSize) * (terrainWidth - 1);
        float terrainZ = (localZ / chunkSize) * (terrainHeight - 1);
        
        int x = Math.max(1, Math.min(terrainWidth - 2, (int) terrainX));
        int z = Math.max(1, Math.min(terrainHeight - 2, (int) terrainZ));
        
        // Sample neighboring heights
        float hL = terrainData[x - 1][z];     // Left
        float hR = terrainData[x + 1][z];     // Right
        float hD = terrainData[x][z - 1];     // Down
        float hU = terrainData[x][z + 1];     // Up
        
        // Calculate normal using cross product of tangent vectors
        Vector3f tangentX = new Vector3f(2.0f, hR - hL, 0.0f);
        Vector3f tangentZ = new Vector3f(0.0f, hU - hD, 2.0f);
        
        Vector3f normal = new Vector3f();
        tangentX.cross(tangentZ, normal);
        normal.normalize();
        
        return normal;
    }
    
    /**
     * Selects detail type based on noise and environmental factors.
     * 
     * @param globalX global X coordinate
     * @param globalZ global Z coordinate
     * @param slope surface slope
     * @param density density value
     * @return selected detail type or null
     */
    private ProceduralDetail.DetailType selectDetailType(float globalX, float globalZ, 
                                                        float slope, float density) {
        float typeNoise = this.typeNoise.noise2D(globalX, globalZ);
        float normalizedTypeNoise = (typeNoise + 1.0f) * 0.5f;
        
        // Adjust probabilities based on slope and density
        ProceduralDetail.DetailType[] types = ProceduralDetail.DetailType.values();
        float[] probabilities = new float[types.length];
        float totalProbability = 0.0f;
        
        for (int i = 0; i < types.length; i++) {
            ProceduralDetail.DetailType type = types[i];
            float baseProbability = config.getTypeDensity(type);
            
            // Adjust probability based on slope
            if (slope > 0.4f) {
                // Steep slopes favor rocks and cracks
                if (type == ProceduralDetail.DetailType.SMALL_ROCK || 
                    type == ProceduralDetail.DetailType.SURFACE_CRACK ||
                    type == ProceduralDetail.DetailType.PEBBLE) {
                    baseProbability *= 2.0f;
                } else {
                    baseProbability *= 0.3f;
                }
            } else {
                // Flat areas favor vegetation
                if (type == ProceduralDetail.DetailType.GRASS || 
                    type == ProceduralDetail.DetailType.FLOWER ||
                    type == ProceduralDetail.DetailType.SMALL_BUSH) {
                    baseProbability *= 1.5f;
                }
            }
            
            // Adjust probability based on density
            baseProbability *= density * config.getGlobalDensityMultiplier();
            
            probabilities[i] = baseProbability;
            totalProbability += baseProbability;
        }
        
        if (totalProbability <= 0.0f) {
            return null;
        }
        
        // Select type based on weighted random selection
        float randomValue = normalizedTypeNoise * totalProbability;
        float currentSum = 0.0f;
        
        for (int i = 0; i < types.length; i++) {
            currentSum += probabilities[i];
            if (randomValue <= currentSum) {
                return types[i];
            }
        }
        
        return types[types.length - 1]; // Fallback
    }
    
    /**
     * Clears the detail cache for a specific chunk.
     * 
     * @param chunkPosition chunk position
     */
    public void clearChunkCache(ChunkPosition chunkPosition) {
        detailCache.remove(chunkPosition);
    }
    
    /**
     * Clears all cached details.
     */
    public void clearAllCache() {
        detailCache.clear();
        logger.info("Cleared all procedural detail cache");
    }
    
    /**
     * Gets the number of cached chunks.
     * 
     * @return number of cached chunks
     */
    public int getCachedChunkCount() {
        return detailCache.size();
    }
    
    /**
     * Gets the total number of cached details.
     * 
     * @return total number of cached details
     */
    public int getTotalCachedDetails() {
        return detailCache.values().stream()
                         .mapToInt(List::size)
                         .sum();
    }
    
    /**
     * Gets the detail configuration.
     * 
     * @return detail configuration
     */
    public DetailConfig getConfig() {
        return config;
    }
}