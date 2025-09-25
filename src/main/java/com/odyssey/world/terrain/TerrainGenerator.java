package com.odyssey.world.terrain;

import com.odyssey.world.WorldChunk;
import com.odyssey.world.WorldConfig;
import com.odyssey.world.BiomeType;
import com.odyssey.world.noise.SimplexNoise;
import org.joml.Vector2f;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced terrain generator using Simplex noise for natural-looking landscapes
 * Generates smooth, continuous terrain with realistic features like hills, valleys, and plains
 */
public class TerrainGenerator {
    
    // Noise generators for different terrain features
    private final SimplexNoise continentalNoise;
    private final SimplexNoise erosionNoise;
    private final SimplexNoise peaksValleysNoise;
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise ridgeNoise;
    private final SimplexNoise caveNoise;
    
    // Terrain configuration
    private final WorldConfig config;
    private final long seed;
    
    // Height parameters
    private static final int SEA_LEVEL = 64;
    private static final int MAX_HEIGHT = 256;
    private static final int MIN_HEIGHT = 0;
    private static final float TERRAIN_SCALE = 0.5f;
    
    // Noise scales for different features
    private static final float CONTINENTAL_SCALE = 0.0008f;
    private static final float EROSION_SCALE = 0.002f;
    private static final float PEAKS_VALLEYS_SCALE = 0.004f;
    private static final float TEMPERATURE_SCALE = 0.001f;
    private static final float HUMIDITY_SCALE = 0.0015f;
    private static final float RIDGE_SCALE = 0.008f;
    
    // Terrain feature weights
    private static final float CONTINENTAL_WEIGHT = 0.6f;
    private static final float EROSION_WEIGHT = 0.3f;
    private static final float PEAKS_VALLEYS_WEIGHT = 0.4f;
    private static final float RIDGE_WEIGHT = 0.2f;
    
    // Biome thresholds
    private static final float TEMPERATURE_COLD = -0.4f;
    private static final float TEMPERATURE_HOT = 0.4f;
    private static final float HUMIDITY_DRY = -0.3f;
    private static final float HUMIDITY_WET = 0.3f;
    
    // Performance optimization - height cache
    private final Map<Vector2f, Float> heightCache;
    private static final int CACHE_SIZE = 10000;
    
    // Logger for terrain generation
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TerrainGenerator.class);
    
    public TerrainGenerator(long seed, WorldConfig config) {
        this.seed = seed;
        this.config = config;
        this.heightCache = new HashMap<>();
        
        // Initialize noise generators with different seeds for variety
        this.continentalNoise = new SimplexNoise(seed);
        this.erosionNoise = new SimplexNoise(seed + 1000);
        this.peaksValleysNoise = new SimplexNoise(seed + 2000);
        this.temperatureNoise = new SimplexNoise(seed + 3000);
        this.humidityNoise = new SimplexNoise(seed + 4000);
        this.ridgeNoise = new SimplexNoise(seed + 5000);
        this.caveNoise = new SimplexNoise(seed + 6000);
        
        logger.info("Initialized TerrainGenerator with Simplex noise (seed: {})", seed);
    }
    
    /**
     * Generates terrain for a world chunk
     */
    public void generateChunkTerrain(WorldChunk chunk) {
        int chunkSize = chunk.getChunkSize();
        Vector2f worldPos = chunk.getWorldPosition();
        
        // Generate height map
        float[][] heightMap = generateHeightMap(chunk.getChunkX(), chunk.getChunkZ(), chunkSize);
        
        // Generate climate maps
        float[][] temperatureMap = generateTemperatureMap(chunk.getChunkX(), chunk.getChunkZ(), chunkSize);
        float[][] humidityMap = generateHumidityMap(chunk.getChunkX(), chunk.getChunkZ(), chunkSize);
        
        // Apply terrain data to chunk
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                float height = heightMap[x][z];
                float temperature = temperatureMap[x][z];
                float humidity = humidityMap[x][z];
                
                // Set height
                chunk.setHeight(x, z, height);
                
                // Determine and set biome
                BiomeType biome = determineBiome(height, temperature, humidity);
                chunk.setBiome(x, z, biome);
                
                // Set climate data
                chunk.setTemperature(x, z, temperature);
                chunk.setHumidity(x, z, humidity);
            }
        }
        
        chunk.setTerrainGenerated(true);
        logger.debug("Generated terrain for chunk ({}, {})", chunk.getChunkX(), chunk.getChunkZ());
        logger.debug("Generated terrain chunk at ({}, {}) with {} height points", 
                    chunk.getChunkX(), chunk.getChunkZ(), chunkSize * chunkSize);
    }
    
    /**
     * Generates a height map using multiple octaves of Simplex noise
     */
    private float[][] generateHeightMap(int chunkX, int chunkZ, int chunkSize) {
        float[][] heightMap = new float[chunkSize][chunkSize];
        
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                float worldX = chunkX * chunkSize + x;
                float worldZ = chunkZ * chunkSize + z;
                
                float height = generateHeightAt(worldX, worldZ);
                heightMap[x][z] = height;
            }
        }
        
        return heightMap;
    }
    
    /**
     * Generates height at a specific world coordinate using layered noise
     */
    public float generateHeightAt(float worldX, float worldZ) {
        // Check cache first for performance
        Vector2f cacheKey = new Vector2f(worldX, worldZ);
        if (heightCache.containsKey(cacheKey)) {
            return heightCache.get(cacheKey);
        }
        
        // Get climate data for biome-specific terrain
        float temperature = getTemperatureAt(worldX, worldZ);
        float humidity = getHumidityAt(worldX, worldZ);
        
        // Continental shape - large scale landmass formation
        float continental = continentalNoise.fractalNoise(
            worldX * CONTINENTAL_SCALE, worldZ * CONTINENTAL_SCALE, 4, 0.5f, 2.0f
        ) * CONTINENTAL_WEIGHT;
        
        // Erosion - creates valleys and worn areas
        float erosion = erosionNoise.fractalNoise(
            worldX * EROSION_SCALE, worldZ * EROSION_SCALE, 3, 0.6f, 2.0f
        ) * EROSION_WEIGHT;
        
        // Peaks and valleys - medium scale terrain features
        float peaksValleys = peaksValleysNoise.ridgedNoise(
            worldX * PEAKS_VALLEYS_SCALE, worldZ * PEAKS_VALLEYS_SCALE, 3, 0.5f, 2.0f
        ) * PEAKS_VALLEYS_WEIGHT;
        
        // Ridge noise for mountain ridges and cliff formations
        float ridges = ridgeNoise.ridgedNoise(
            worldX * RIDGE_SCALE, worldZ * RIDGE_SCALE, 2, 0.7f, 2.0f
        ) * RIDGE_WEIGHT;
        
        // Apply biome-specific terrain modifications
        float biomeModifier = getBiomeTerrainModifier(worldX, worldZ, temperature, humidity);
        
        // Combine all noise layers
        float combinedNoise = continental + erosion + peaksValleys + ridges + biomeModifier;
        
        // Apply terrain shaping curve for more realistic distribution
        combinedNoise = applyTerrainCurve(combinedNoise);
        
        // Convert to world height
        float height = SEA_LEVEL + (combinedNoise * (MAX_HEIGHT - SEA_LEVEL) * TERRAIN_SCALE);
        
        // Clamp to valid range
        height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
        
        // Cache the result if cache isn't full
        if (heightCache.size() < CACHE_SIZE) {
            heightCache.put(cacheKey, height);
        }
        
        return height;
    }
    
    /**
     * Applies a curve to terrain noise for more realistic height distribution
     */
    private float applyTerrainCurve(float noise) {
        // Normalize noise to [-1, 1] range
        noise = Math.max(-1.0f, Math.min(1.0f, noise));
        
        // Apply S-curve for more realistic terrain distribution
        // This creates more flat areas (plains) and steeper transitions (cliffs)
        if (noise > 0) {
            // Above sea level - exponential curve for mountains
            return (float) Math.pow(noise, 1.5);
        } else {
            // Below sea level - gentler curve for ocean floor
            return -(float) Math.pow(-noise, 0.8);
        }
    }
    
    /**
     * Generates temperature map for biome determination
     */
    private float[][] generateTemperatureMap(int chunkX, int chunkZ, int chunkSize) {
        float[][] temperatureMap = new float[chunkSize][chunkSize];
        
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                float worldX = chunkX * chunkSize + x;
                float worldZ = chunkZ * chunkSize + z;
                
                // Base temperature varies with latitude (Z coordinate)
                float latitudeTemp = (float) Math.cos(worldZ * 0.0001f) * 0.8f;
                
                // Add noise variation
                float noiseTemp = temperatureNoise.fractalNoise(
                    worldX * TEMPERATURE_SCALE, worldZ * TEMPERATURE_SCALE, 3, 0.5f, 2.0f
                ) * 0.4f;
                
                temperatureMap[x][z] = latitudeTemp + noiseTemp;
            }
        }
        
        return temperatureMap;
    }
    
    /**
     * Generates humidity map for biome determination
     */
    private float[][] generateHumidityMap(int chunkX, int chunkZ, int chunkSize) {
        float[][] humidityMap = new float[chunkSize][chunkSize];
        
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                float worldX = chunkX * chunkSize + x;
                float worldZ = chunkZ * chunkSize + z;
                
                // Humidity varies with noise and distance from water
                float humidity = humidityNoise.fractalNoise(
                    worldX * HUMIDITY_SCALE, worldZ * HUMIDITY_SCALE, 3, 0.6f, 2.0f
                );
                
                humidityMap[x][z] = humidity;
            }
        }
        
        return humidityMap;
    }
    
    /**
     * Determines biome based on height, temperature, and humidity
     */
    private BiomeType determineBiome(float height, float temperature, float humidity) {
        // Ocean biomes
        if (height < SEA_LEVEL - 5) {
            if (temperature < TEMPERATURE_COLD) {
                return BiomeType.DEEP_OCEAN;  // Use DEEP_OCEAN for cold areas
            } else if (temperature > TEMPERATURE_HOT) {
                return BiomeType.OCEAN;  // Use OCEAN for hot areas
            } else {
                return BiomeType.OCEAN;
            }
        }
        
        // Underwater but shallow
        if (height < SEA_LEVEL) {
            return BiomeType.SHALLOW_WATER;
        }
        
        // Land biomes based on temperature and humidity
        if (temperature < TEMPERATURE_COLD) {
            // Cold biomes
            if (humidity < HUMIDITY_DRY) {
                return BiomeType.TUNDRA;
            } else {
                return BiomeType.FOREST;  // Use FOREST instead of TAIGA
            }
        } else if (temperature > TEMPERATURE_HOT) {
            // Hot biomes
            if (humidity < HUMIDITY_DRY) {
                return BiomeType.DESERT;
            } else if (humidity > HUMIDITY_WET) {
                return BiomeType.TROPICAL_FOREST;  // Use TROPICAL_FOREST instead of TROPICAL_RAINFOREST
            } else {
                return BiomeType.SAVANNA;
            }
        } else {
            // Temperate biomes
            if (humidity < HUMIDITY_DRY) {
                return BiomeType.GRASSLAND;
            } else if (humidity > HUMIDITY_WET) {
                return BiomeType.FOREST;  // Use FOREST instead of TEMPERATE_RAINFOREST
            } else {
                if (height > SEA_LEVEL + 50) {
                    return BiomeType.MOUNTAIN;
                } else {
                    return BiomeType.FOREST;  // Use FOREST instead of TEMPERATE_FOREST
                }
            }
        }
    }
    
    /**
     * Gets temperature at a specific world coordinate
     */
    private float getTemperatureAt(float worldX, float worldZ) {
        // Base temperature varies with latitude (Z coordinate)
        float latitudeTemp = (float) Math.cos(worldZ * 0.0001f) * 0.8f;
        
        // Add noise variation
        float noiseTemp = temperatureNoise.fractalNoise(
            worldX * TEMPERATURE_SCALE, worldZ * TEMPERATURE_SCALE, 3, 0.5f, 2.0f
        ) * 0.4f;
        
        return latitudeTemp + noiseTemp;
    }
    
    /**
     * Gets humidity at a specific world coordinate
     */
    private float getHumidityAt(float worldX, float worldZ) {
        return humidityNoise.fractalNoise(
            worldX * HUMIDITY_SCALE, worldZ * HUMIDITY_SCALE, 3, 0.6f, 2.0f
        );
    }
    
    /**
     * Applies biome-specific terrain modifications
     */
    private float getBiomeTerrainModifier(float worldX, float worldZ, float temperature, float humidity) {
        float modifier = 0.0f;
        
        // Mountain biomes - add extra height and ruggedness
        if (temperature < TEMPERATURE_COLD) {
            // Cold mountains - sharp peaks and deep valleys
            float mountainNoise = peaksValleysNoise.ridgedNoise(
                worldX * 0.008f, worldZ * 0.008f, 4, 0.7f, 2.2f
            );
            modifier += mountainNoise * 0.3f;
        }
        
        // Desert biomes - rolling dunes and mesas
        if (temperature > TEMPERATURE_HOT && humidity < HUMIDITY_DRY) {
            float duneNoise = erosionNoise.fractalNoise(
                worldX * 0.015f, worldZ * 0.015f, 2, 0.4f, 1.8f
            );
            // Create mesa-like formations
            float mesaNoise = ridgeNoise.turbulenceNoise(
                worldX * 0.005f, worldZ * 0.005f, 3, 0.6f, 2.0f
            );
            modifier += (duneNoise * 0.15f) + (mesaNoise * 0.25f);
        }
        
        // Tropical rainforest - rolling hills with river valleys
        if (temperature > TEMPERATURE_HOT && humidity > HUMIDITY_WET) {
            float hillNoise = continentalNoise.fractalNoise(
                worldX * 0.012f, worldZ * 0.012f, 3, 0.5f, 2.0f
            );
            // River valleys
            float riverNoise = erosionNoise.ridgedNoise(
                worldX * 0.003f, worldZ * 0.003f, 2, 0.8f, 2.0f
            );
            modifier += (hillNoise * 0.2f) - (Math.abs(riverNoise) * 0.15f);
        }
        
        // Plains and grasslands - gentle rolling terrain
        if (Math.abs(temperature) < 0.3f && Math.abs(humidity) < 0.4f) {
            float plainsNoise = continentalNoise.fractalNoise(
                worldX * 0.02f, worldZ * 0.02f, 2, 0.3f, 1.5f
            );
            modifier += plainsNoise * 0.1f;
        }
        
        // Coastal areas - flatten near sea level
        float baseHeight = SEA_LEVEL + (modifier * (MAX_HEIGHT - SEA_LEVEL) * TERRAIN_SCALE);
        if (Math.abs(baseHeight - SEA_LEVEL) < 10) {
            float coastalFlattening = (10 - Math.abs(baseHeight - SEA_LEVEL)) / 10.0f;
            modifier *= (1.0f - coastalFlattening * 0.7f);
        }
        
        return modifier;
    }
    
    /**
     * Generates cave systems using 3D Simplex noise
     */
    public boolean isCaveAt(float worldX, float worldY, float worldZ) {
        // Only generate caves below sea level and above bedrock
        if (worldY >= SEA_LEVEL || worldY < 5) {
            return false;
        }
        
        // Use 3D noise for cave generation
        float caveNoise1 = caveNoise.noise3D(worldX * 0.02f, worldY * 0.03f, worldZ * 0.02f);
        float caveNoise2 = caveNoise.noise3D(worldX * 0.03f + 1000, worldY * 0.02f, worldZ * 0.03f + 1000);
        
        // Combine noise for more complex cave systems
        float combinedNoise = (caveNoise1 + caveNoise2) * 0.5f;
        
        // Cave threshold - higher values = fewer caves
        return combinedNoise > 0.6f;
    }
    
    /**
     * Gets the height at a world coordinate (with caching)
     */
    public float getHeightAt(float worldX, float worldZ) {
        return generateHeightAt(worldX, worldZ);
    }
    
    /**
     * Clears the height cache to free memory
     */
    public void clearCache() {
        heightCache.clear();
        logger.info("Cleared terrain height cache");
    }
    
    /**
     * Gets cache statistics for debugging
     */
    public int getCacheSize() {
        return heightCache.size();
    }
    
    // Getters
    public long getSeed() { return seed; }
    public WorldConfig getConfig() { return config; }
}