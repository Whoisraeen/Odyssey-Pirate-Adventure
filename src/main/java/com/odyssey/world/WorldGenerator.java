package com.odyssey.world;

import com.odyssey.world.BiomeType;
import com.odyssey.world.noise.NoiseGenerator;
import com.odyssey.world.terrain.TerrainGenerator;
import com.odyssey.world.weather.WeatherCondition;
import com.odyssey.world.weather.WeatherSystem;
import com.odyssey.world.TradeRoute;
import com.odyssey.world.navigation.NavigationHazard;
import com.odyssey.util.Logger;
import com.odyssey.util.MathUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main world generator - creates and manages the entire game world
 * Handles procedural generation of islands, ocean, caves, biomes, weather, and trade routes
 */
public class WorldGenerator {
    
    // World configuration
    private final WorldConfig config;
    private final long seed;
    private final Random random;
    
    // Generators
    private final IslandGenerator islandGenerator;
    private final TerrainGenerator terrainGenerator;
    private final NoiseGenerator oceanNoise;
    private final NoiseGenerator weatherNoise;
    private final NoiseGenerator currentNoise;
    
    // Legacy noise generators (keeping for compatibility)
    private final NoiseGenerator heightNoise;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final NoiseGenerator caveNoise;
    private final NoiseGenerator oreNoise;
    private final NoiseGenerator islandNoise;
    
    // World data
    private final Map<Vector2f, WorldChunk> loadedChunks;
    private final Map<Vector2f, Island> islands;
    private final List<TradeRoute> tradeRoutes;
    private final List<NavigationHazard> hazards;
    
    // Systems
    private final WeatherSystem weatherSystem;
    
    // Generation parameters
    private static final int SEA_LEVEL = 64;
    private static final int MAX_HEIGHT = 128;
    private static final int MIN_HEIGHT = 32;
    
    // Island generation
    private static final float ISLAND_FREQUENCY = 0.003f;
    private static final float ISLAND_THRESHOLD = 0.3f;
    private static final int ISLAND_MIN_SIZE = 32;
    private static final int ISLAND_MAX_SIZE = 128;
    
    // Biome thresholds
    private static final float TEMPERATURE_COLD = -0.3f;
    private static final float TEMPERATURE_HOT = 0.3f;
    private static final float HUMIDITY_DRY = -0.3f;
    private static final float HUMIDITY_WET = 0.3f;
    
    // Generation state
    private boolean isGenerating;
    private final Object generationLock = new Object();
    
    public WorldGenerator(long seed) {
        this(seed, new WorldConfig());
    }
    
    public WorldGenerator(long seed, WorldConfig config) {
        this.seed = seed;
        this.config = config;
        this.random = new Random(seed);
        
        // Initialize generators
        this.islandGenerator = new IslandGenerator(seed, config.getIslandConfig());
        this.terrainGenerator = new TerrainGenerator(seed, config);
        this.oceanNoise = new NoiseGenerator(seed + 5000);
        this.weatherNoise = new NoiseGenerator(seed + 6000);
        this.currentNoise = new NoiseGenerator(seed + 7000);
        
        // Initialize legacy noise generators (keeping for compatibility)
        this.heightNoise = new NoiseGenerator(seed);
        this.temperatureNoise = new NoiseGenerator(seed + 1);
        this.humidityNoise = new NoiseGenerator(seed + 2);
        this.caveNoise = new NoiseGenerator(seed + 3);
        this.oreNoise = new NoiseGenerator(seed + 4);
        this.islandNoise = new NoiseGenerator(seed + 5);
        
        // Initialize data structures
        this.loadedChunks = new ConcurrentHashMap<>();
        this.islands = new ConcurrentHashMap<>();
        this.tradeRoutes = new ArrayList<>();
        this.hazards = new ArrayList<>();
        
        // Initialize systems
        this.weatherSystem = new WeatherSystem(seed + 8000, config.getWeatherConfig());
        
        Logger.world("Initialized world generator with seed: {}", seed);
        
        // Generate initial world features
        generateGlobalFeatures();
    }
    
    /**
     * Generates or loads a world chunk at the specified coordinates
     */
    public WorldChunk getChunk(int chunkX, int chunkZ) {
        Vector2f chunkKey = new Vector2f(chunkX, chunkZ);
        
        // Check if chunk is already loaded
        if (loadedChunks.containsKey(chunkKey)) {
            return loadedChunks.get(chunkKey);
        }
        
        // Generate new chunk
        synchronized (generationLock) {
            // Double-check after acquiring lock
            if (loadedChunks.containsKey(chunkKey)) {
                return loadedChunks.get(chunkKey);
            }
            
            WorldChunk chunk = generateWorldChunk(chunkX, chunkZ);
            loadedChunks.put(chunkKey, chunk);
            
            Logger.world("Generated chunk ({}, {})", chunkX, chunkZ);
            return chunk;
        }
    }
    
    /**
     * Gets the height at a specific world coordinate
     */
    public float getHeightAt(float worldX, float worldZ) {
        // Find the chunk containing this coordinate
        int chunkX = (int) Math.floor(worldX / config.getChunkSize());
        int chunkZ = (int) Math.floor(worldZ / config.getChunkSize());
        
        WorldChunk chunk = getChunk(chunkX, chunkZ);
        
        // Get local coordinates within the chunk
        float localX = worldX - (chunkX * config.getChunkSize());
        float localZ = worldZ - (chunkZ * config.getChunkSize());
        
        return chunk.getHeightAt(localX, localZ);
    }
    
    /**
     * Gets the biome at a specific world coordinate
     */
    public BiomeType getBiomeAt(float worldX, float worldZ) {
        int chunkX = (int) Math.floor(worldX / config.getChunkSize());
        int chunkZ = (int) Math.floor(worldZ / config.getChunkSize());
        
        WorldChunk chunk = getChunk(chunkX, chunkZ);
        
        float localX = worldX - (chunkX * config.getChunkSize());
        float localZ = worldZ - (chunkZ * config.getChunkSize());
        
        return chunk.getBiomeAt(localX, localZ);
    }
    
    /**
     * Gets all islands within a specified radius of a point
     */
    public List<Island> getIslandsInRadius(float centerX, float centerZ, float radius) {
        List<Island> nearbyIslands = new ArrayList<>();
        
        for (Island island : islands.values()) {
            float distance = (float) Math.sqrt(
                (island.getCenterX() - centerX) * (island.getCenterX() - centerX) +
                (island.getCenterZ() - centerZ) * (island.getCenterZ() - centerZ)
            );
            
            if (distance <= radius + island.getType().getRadius()) {
                nearbyIslands.add(island);
            }
        }
        
        return nearbyIslands;
    }
    
    /**
     * Gets the nearest island to a point
     */
    public Island getNearestIsland(float worldX, float worldZ) {
        Island nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (Island island : islands.values()) {
            float distance = (float) Math.sqrt(
                (island.getCenterX() - worldX) * (island.getCenterX() - worldX) +
                (island.getCenterZ() - worldZ) * (island.getCenterZ() - worldZ)
            );
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = island;
            }
        }
        
        return nearest;
    }
    
    /**
     * Gets ocean current velocity at a point
     */
    public Vector2f getOceanCurrentAt(float worldX, float worldZ) {
        // Generate ocean currents using noise
        float currentX = currentNoise.sample(worldX * 0.001f, worldZ * 0.001f) * config.getMaxCurrentSpeed();
        float currentZ = currentNoise.sample(worldX * 0.001f + 1000, worldZ * 0.001f + 1000) * config.getMaxCurrentSpeed();
        
        // Add circular current patterns around islands
        List<Island> nearbyIslands = getIslandsInRadius(worldX, worldZ, 200);
        
        for (Island island : nearbyIslands) {
            float dx = worldX - island.getCenterX();
            float dz = worldZ - island.getCenterZ();
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            
            if (distance < island.getType().getRadius() + 100) {
                // Create circular current around island
                float influence = 1.0f - (distance / (island.getType().getRadius() + 100));
                float circularStrength = influence * 2.0f;
                
                currentX += -dz / distance * circularStrength;
                currentZ += dx / distance * circularStrength;
            }
        }
        
        return new Vector2f(currentX, currentZ);
    }
    
    /**
     * Gets wind velocity at a point
     */
    public Vector2f getWindAt(float worldX, float worldZ) {
        return weatherSystem.getWindAt(worldX, worldZ);
    }
    
    /**
     * Gets weather conditions at a point
     */
    public WeatherCondition getWeatherAt(float worldX, float worldZ) {
        return weatherSystem.getWeatherAt(worldX, worldZ);
    }
    
    /**
     * Gets all trade routes
     */
    public List<TradeRoute> getTradeRoutes() {
        return new ArrayList<>(tradeRoutes);
    }
    
    /**
     * Gets navigation hazards in a region
     */
    public List<NavigationHazard> getHazardsInRegion(float minX, float minZ, float maxX, float maxZ) {
        List<NavigationHazard> regionHazards = new ArrayList<>();
        
        for (NavigationHazard hazard : hazards) {
            Vector3f pos = hazard.getPosition3D();
            
            if (pos.x >= minX && pos.x <= maxX && pos.z >= minZ && pos.z <= maxZ) {
                regionHazards.add(hazard);
            }
        }
        
        return regionHazards;
    }
    
    /**
     * Updates dynamic world systems
     */
    public void update(float deltaTime) {
        weatherSystem.update(deltaTime);
        
        // Update trade routes
        for (TradeRoute route : tradeRoutes) {
            route.update(deltaTime);
        }
        
        // Update navigation hazards
        for (NavigationHazard hazard : hazards) {
            // Get current weather condition at hazard location
            Vector2f pos = hazard.getPosition();
            WeatherCondition currentWeather = weatherSystem.getWeatherAt(pos.x, pos.y);
            hazard.update(deltaTime, currentWeather);
        }
    }
    
    private WorldChunk generateWorldChunk(int chunkX, int chunkZ) {
        isGenerating = true;
        
        try {
            WorldChunk chunk = new WorldChunk(chunkX, chunkZ, config.getChunkSize());
            
            // Generate chunk terrain
            generateChunkTerrain(chunk);
            
            // Generate chunk biomes
            generateChunkBiomes(chunk);
            
            // Generate islands in chunk
            generateChunkIslands(chunk);
            
            // Generate ocean features
            generateChunkOceanFeatures(chunk);
            
            // Generate navigation hazards
            generateChunkHazards(chunk);
            
            return chunk;
            
        } finally {
            isGenerating = false;
        }
    }
    
    /**
     * Generates terrain for a chunk using the TerrainGenerator
     */
    private void generateChunkTerrain(WorldChunk chunk) {
        terrainGenerator.generateChunkTerrain(chunk);
    }
    
    /**
     * Generates biomes for a chunk
     */
    private void generateChunkBiomes(WorldChunk chunk) {
        int chunkSize = config.getChunkSize();
        float[][] temperatureMap = generateTemperatureMap(chunk.getChunkX(), chunk.getChunkZ());
        float[][] humidityMap = generateHumidityMap(chunk.getChunkX(), chunk.getChunkZ());
        
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                float temperature = temperatureMap[x][z];
                float humidity = humidityMap[x][z];
                int height = (int) chunk.getHeightAt(x, z);
                BiomeType biome = getBiome(temperature, humidity, height);
                chunk.setBiome(x, z, biome);
            }
        }
    }
    
    /**
     * Generates islands within a chunk
     */
    private void generateChunkIslands(WorldChunk chunk) {
        int chunkSize = config.getChunkSize();
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Check for islands in this chunk using island noise
        for (int x = 0; x < chunkSize; x += 8) { // Sample every 8 blocks for performance
            for (int z = 0; z < chunkSize; z += 8) {
                int worldX = chunkX * chunkSize + x;
                int worldZ = chunkZ * chunkSize + z;
                
                float islandValue = islandNoise.sample(worldX * ISLAND_FREQUENCY, worldZ * ISLAND_FREQUENCY);
                if (islandValue > ISLAND_THRESHOLD) {
                    // Generate island at this location
                    IslandType islandType = islandGenerator.determineIslandType(worldX, worldZ);
                    Island island = islandGenerator.generateIsland(worldX, worldZ, islandType);
                    if (island != null) {
                        Vector2f key = new Vector2f(worldX, worldZ);
                        islands.put(key, island);
                    }
                }
            }
        }
    }
    
    /**
     * Generates ocean features for a chunk
     */
    private void generateChunkOceanFeatures(WorldChunk chunk) {
        int chunkSize = config.getChunkSize();
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Generate ocean currents
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                int worldX = chunkX * chunkSize + x;
                int worldZ = chunkZ * chunkSize + z;
                
                float currentX = currentNoise.sample(worldX * 0.01f, worldZ * 0.01f);
                float currentZ = currentNoise.sample(worldX * 0.01f + 1000, worldZ * 0.01f + 1000);
                
                Vector2f currentDirection = new Vector2f(currentX, currentZ);
                float currentStrength = currentDirection.length();
                if (currentStrength > 0) {
                    currentDirection.normalize();
                }
                
                chunk.setCurrent(x, z, currentDirection, currentStrength);
            }
        }
    }
    
    /**
     * Generates navigation hazards for a chunk
     */
    private void generateChunkHazards(WorldChunk chunk) {
        int chunkSize = config.getChunkSize();
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Randomly place hazards based on noise
        Random hazardRandom = new Random(seed + chunkX * 1000L + chunkZ);
        
        if (hazardRandom.nextFloat() < 0.1f) { // 10% chance per chunk
            int x = hazardRandom.nextInt(chunkSize);
            int z = hazardRandom.nextInt(chunkSize);
            int worldX = chunkX * chunkSize + x;
            int worldZ = chunkZ * chunkSize + z;
            
            // Create a simple navigation hazard (rocks, reefs, etc.)
            NavigationHazard hazard = new NavigationHazard(
                "hazard_" + worldX + "_" + worldZ,
                "Coral Reef",
                HazardType.REEF,
                HazardSeverity.MODERATE,
                new Vector2f(worldX, worldZ),
                50.0f // radius
            );
            hazards.add(hazard);
        }
    }
    
    private void generateGlobalFeatures() {
        Logger.world("Generating global world features...");
        
        // Generate major trade routes
        generateTradeRoutes();
        
        // Generate major weather patterns
        weatherSystem.generateGlobalPatterns();
        
        Logger.world("Global world features generated");
    }
    
    private void generateTradeRoutes() {
        int routeCount = config.getTradeRouteCount();
        
        for (int i = 0; i < routeCount; i++) {
            // Generate random start and end points
            float startX = (random.nextFloat() - 0.5f) * config.getWorldSize();
            float startZ = (random.nextFloat() - 0.5f) * config.getWorldSize();
            float endX = (random.nextFloat() - 0.5f) * config.getWorldSize();
            float endZ = (random.nextFloat() - 0.5f) * config.getWorldSize();
            
            // Create dummy islands for the trade route (will be replaced with actual islands later)
            Island startIsland = createDummyIsland(startX, startZ, "Start_" + i);
            Island endIsland = createDummyIsland(endX, endZ, "End_" + i);
            
            // Create trade route with proper constructor parameters
            TradeRoute route = new TradeRoute(
                "Route_" + i,
                startIsland,
                endIsland,
                TradeRouteType.MERCHANT
            );
            
            tradeRoutes.add(route);
        }
        
        Logger.world("Generated {} trade routes", routeCount);
    }
    
    /**
     * Creates a dummy island for trade route generation
     */
    private Island createDummyIsland(float x, float z, String name) {
        // Create a basic island with minimal properties for trade route generation
        Island island = new Island(x, z, IslandType.TROPICAL_SMALL);
        return island;
    }
    
    /**
     * Generates height map for a chunk
     */
    private float[][] generateHeightMap(int chunkX, int chunkZ) {
        float[][] heightMap = new float[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                // Check if this position should have an island
                float islandValue = islandNoise.noise(worldX * ISLAND_FREQUENCY, worldZ * ISLAND_FREQUENCY);
                
                if (islandValue > ISLAND_THRESHOLD) {
                    // Generate island terrain
                    float baseHeight = heightNoise.noise(worldX * 0.01f, worldZ * 0.01f) * 32;
                    float detailHeight = heightNoise.noise(worldX * 0.05f, worldZ * 0.05f) * 8;
                    float fineDetail = heightNoise.noise(worldX * 0.1f, worldZ * 0.1f) * 4;
                    
                    // Island falloff from center
                    float distanceFromCenter = getDistanceToNearestIslandCenter(worldX, worldZ);
                    float falloff = Math.max(0, 1.0f - (distanceFromCenter / ISLAND_MAX_SIZE));
                    falloff = falloff * falloff; // Smooth falloff
                    
                    float totalHeight = SEA_LEVEL + (baseHeight + detailHeight + fineDetail) * falloff;
                    heightMap[x][z] = Math.max(SEA_LEVEL - 10, Math.min(MAX_HEIGHT, totalHeight));
                } else {
                    // Ocean floor
                    float oceanFloor = heightNoise.noise(worldX * 0.02f, worldZ * 0.02f) * 16;
                    heightMap[x][z] = Math.max(MIN_HEIGHT, SEA_LEVEL - 20 + oceanFloor);
                }
            }
        }
        
        return heightMap;
    }
    
    /**
     * Generates temperature map for biome determination
     */
    private float[][] generateTemperatureMap(int chunkX, int chunkZ) {
        float[][] temperatureMap = new float[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                temperatureMap[x][z] = temperatureNoise.noise(worldX * 0.005f, worldZ * 0.005f);
            }
        }
        
        return temperatureMap;
    }
    
    /**
     * Generates humidity map for biome determination
     */
    private float[][] generateHumidityMap(int chunkX, int chunkZ) {
        float[][] humidityMap = new float[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                humidityMap[x][z] = humidityNoise.noise(worldX * 0.005f, worldZ * 0.005f);
            }
        }
        
        return humidityMap;
    }
    
    /**
     * Generates a vertical column of blocks
     */
    private void generateColumn(Chunk chunk, int x, int z, int worldX, int worldZ, 
                               float height, float temperature, float humidity) {
        
        int terrainHeight = (int) height;
        BiomeType biome = getBiome(temperature, humidity, terrainHeight);
        
        // Generate from bottom to top
        for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
            Block.BlockType blockType;
            
            if (y > terrainHeight) {
                // Above terrain
                if (y <= SEA_LEVEL) {
                    blockType = Block.BlockType.WATER;
                } else {
                    blockType = Block.BlockType.AIR;
                }
            } else if (y == terrainHeight) {
                // Surface block
                blockType = getSurfaceBlock(biome, y);
            } else if (y > terrainHeight - 4) {
                // Subsurface blocks
                blockType = getSubsurfaceBlock(biome, y, terrainHeight);
            } else {
                // Deep blocks
                blockType = Block.BlockType.STONE;
            }
            
            chunk.setBlock(x, y, z, blockType);
        }
        
        // Add bedrock at bottom
        for (int y = 0; y < 5; y++) {
            if (random.nextFloat() < (5 - y) / 5.0f) {
                chunk.setBlock(x, y, z, Block.BlockType.STONE);
            }
        }
    }
    
    /**
     * Determines biome based on temperature and humidity
     */
    private BiomeType getBiome(float temperature, float humidity, int height) {
        if (height <= SEA_LEVEL) {
            return BiomeType.OCEAN;
        }
        
        if (temperature > TEMPERATURE_HOT) {
            if (humidity < HUMIDITY_DRY) {
                return BiomeType.DESERT;
            } else if (humidity > HUMIDITY_WET) {
                return BiomeType.TROPICAL_FOREST;
            } else {
                return BiomeType.SAVANNA;
            }
        } else if (temperature < TEMPERATURE_COLD) {
            return BiomeType.TUNDRA;
        } else {
            if (humidity > HUMIDITY_WET) {
                return BiomeType.FOREST;
            } else if (humidity < HUMIDITY_DRY) {
                return BiomeType.PLAINS;
            } else {
                return BiomeType.GRASSLAND;
            }
        }
    }
    
    /**
     * Gets the surface block for a biome
     */
    private Block.BlockType getSurfaceBlock(BiomeType biome, int height) {
        switch (biome) {
            case DESERT:
                return Block.BlockType.SAND;
            case TROPICAL_FOREST:
            case JUNGLE:
            case FOREST:
                return Block.BlockType.GRASS;
            case OCEAN:
                return height <= SEA_LEVEL ? Block.BlockType.SAND : Block.BlockType.GRASS;
            case TUNDRA:
                return Block.BlockType.STONE;
            case PLAINS:
            case GRASSLAND:
            case SAVANNA:
            default:
                return Block.BlockType.GRASS;
        }
    }
    
    /**
     * Gets subsurface blocks for a biome
     */
    private Block.BlockType getSubsurfaceBlock(BiomeType biome, int y, int surfaceHeight) {
        switch (biome) {
            case DESERT:
                return Block.BlockType.SAND;
            case OCEAN:
                return y <= SEA_LEVEL ? Block.BlockType.SAND : Block.BlockType.DIRT;
            default:
                return Block.BlockType.DIRT;
        }
    }
    
    /**
     * Generates caves using 3D noise
     */
    private void generateCaves(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                for (int y = 1; y < SEA_LEVEL; y++) {
                    int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x;
                    int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + z;
                    
                    float caveValue = caveNoise.noise3D(worldX * 0.02f, y * 0.02f, worldZ * 0.02f);
                    
                    if (caveValue > 0.6f && chunk.getBlock(x, y, z) == Block.BlockType.STONE) {
                        chunk.setBlock(x, y, z, Block.BlockType.AIR);
                    }
                }
            }
        }
    }
    
    /**
     * Generates ore deposits
     */
    private void generateOres(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                for (int y = 1; y < MAX_HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) != Block.BlockType.STONE) {
                        continue;
                    }
                    
                    int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x;
                    int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + z;
                    
                    float oreValue = oreNoise.noise3D(worldX * 0.05f, y * 0.05f, worldZ * 0.05f);
                    
                    // Coal ore (common, higher levels)
                    if (y > 32 && oreValue > 0.7f && random.nextFloat() < 0.1f) {
                        chunk.setBlock(x, y, z, Block.BlockType.COAL_ORE);
                    }
                    // Iron ore (medium, mid levels)
                    else if (y > 16 && y < 64 && oreValue > 0.75f && random.nextFloat() < 0.05f) {
                        chunk.setBlock(x, y, z, Block.BlockType.IRON_ORE);
                    }
                    // Gold ore (rare, lower levels)
                    else if (y < 32 && oreValue > 0.8f && random.nextFloat() < 0.02f) {
                        chunk.setBlock(x, y, z, Block.BlockType.GOLD_ORE);
                    }
                }
            }
        }
    }
    
    /**
     * Generates vegetation based on biome
     */
    private void generateVegetation(Chunk chunk, float[][] temperatureMap, float[][] humidityMap) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                // Find surface level
                int surfaceY = -1;
                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block.BlockType block = chunk.getBlock(x, y, z);
                    if (block != Block.BlockType.AIR && block != Block.BlockType.WATER) {
                        surfaceY = y;
                        break;
                    }
                }
                
                if (surfaceY < 0 || surfaceY >= SEA_LEVEL - 1) {
                    continue; // No valid surface or underwater
                }
                
                float temperature = temperatureMap[x][z];
                float humidity = humidityMap[x][z];
                BiomeType biome = getBiome(temperature, humidity, surfaceY);
                
                // Generate trees
                if (random.nextFloat() < getTreeDensity(biome)) {
                    generateTree(chunk, x, surfaceY + 1, z, biome);
                }
                
                // Generate coral underwater
                if (surfaceY < SEA_LEVEL - 5 && random.nextFloat() < 0.1f) {
                    chunk.setBlock(x, surfaceY + 1, z, Block.BlockType.CORAL);
                }
                
                // Generate seaweed
                if (surfaceY < SEA_LEVEL - 2 && random.nextFloat() < 0.2f) {
                    for (int y = surfaceY + 1; y <= SEA_LEVEL && y < surfaceY + 4; y++) {
                        chunk.setBlock(x, y, z, Block.BlockType.SEAWEED);
                    }
                }
            }
        }
    }
    
    /**
     * Gets tree density for a biome
     */
    private float getTreeDensity(BiomeType biome) {
        switch (biome) {
            case FOREST: return 0.3f;
            case TROPICAL_FOREST: return 0.4f;
            case JUNGLE: return 0.5f;
            case GRASSLAND: return 0.1f;
            case SAVANNA: return 0.05f;
            default: return 0.0f;
        }
    }
    
    /**
     * Generates a tree at the specified position
     */
    private void generateTree(Chunk chunk, int x, int y, int z, BiomeType biome) {
        Block.BlockType woodType = (biome == BiomeType.TROPICAL_FOREST || biome == BiomeType.JUNGLE) ? 
                                   Block.BlockType.PALM_WOOD : Block.BlockType.WOOD;
        Block.BlockType leafType = (biome == BiomeType.TROPICAL_FOREST || biome == BiomeType.JUNGLE) ? 
                                   Block.BlockType.PALM_LEAVES : Block.BlockType.LEAVES;
        
        int treeHeight = 4 + random.nextInt(3);
        
        // Generate trunk
        for (int i = 0; i < treeHeight; i++) {
            if (y + i < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y + i, z, woodType);
            }
        }
        
        // Generate leaves
        int leafY = y + treeHeight - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    int leafX = x + dx;
                    int leafZ = z + dz;
                    int currentY = leafY + dy;
                    
                    if (leafX >= 0 && leafX < Chunk.CHUNK_SIZE && 
                        leafZ >= 0 && leafZ < Chunk.CHUNK_SIZE && 
                        currentY < Chunk.CHUNK_HEIGHT) {
                        
                        float distance = (float) Math.sqrt(dx * dx + dz * dz + dy * dy);
                        if (distance <= 2.5f && random.nextFloat() < 0.8f) {
                            if (chunk.getBlock(leafX, currentY, leafZ) == Block.BlockType.AIR) {
                                chunk.setBlock(leafX, currentY, leafZ, leafType);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Generates structures like treasure chests
     */
    private void generateStructures(Chunk chunk) {
        // Rare treasure chests on islands
        if (random.nextFloat() < 0.01f) {
            int x = random.nextInt(Chunk.CHUNK_SIZE);
            int z = random.nextInt(Chunk.CHUNK_SIZE);
            
            // Find surface
            for (int y = Chunk.CHUNK_HEIGHT - 1; y >= SEA_LEVEL; y--) {
                if (chunk.getBlock(x, y, z) != Block.BlockType.AIR) {
                    if (y + 1 < Chunk.CHUNK_HEIGHT && chunk.getBlock(x, y + 1, z) == Block.BlockType.AIR) {
                        chunk.setBlock(x, y + 1, z, Block.BlockType.TREASURE_CHEST);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Gets distance to nearest island center (simplified)
     */
    private float getDistanceToNearestIslandCenter(int worldX, int worldZ) {
        // Simplified - in a real implementation, you'd track island centers
        int gridSize = ISLAND_MAX_SIZE;
        int centerX = (worldX / gridSize) * gridSize + gridSize / 2;
        int centerZ = (worldZ / gridSize) * gridSize + gridSize / 2;
        
        return (float) Math.sqrt((worldX - centerX) * (worldX - centerX) + (worldZ - centerZ) * (worldZ - centerZ));
    }
    
    /**
     * Gets the world configuration
     */
    public WorldConfig getConfig() {
        return config;
    }
    
    /**
     * Gets the world seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Gets all loaded chunks
     */
    public Map<Vector2f, WorldChunk> getLoadedChunks() {
        return new HashMap<>(loadedChunks);
    }
    
    /**
     * Gets all islands
     */
    public Map<Vector2f, Island> getIslands() {
        return new HashMap<>(islands);
    }
    
    /**
     * Gets all hazards
     */
    public List<NavigationHazard> getHazards() {
        return new ArrayList<>(hazards);
    }
    
    /**
     * Gets the weather system
     */
    public WeatherSystem getWeatherSystem() {
        return weatherSystem;
    }
    
    /**
     * Adds a new hazard to the world
     */
    public void addHazard(NavigationHazard hazard) {
        hazards.add(hazard);
        Logger.world("Added hazard: {} at ({}, {})", hazard.getName(), 
                    hazard.getPosition().x, hazard.getPosition().y);
    }
    
    /**
     * Removes a hazard from the world
     */
    public void removeHazard(NavigationHazard hazard) {
        hazards.remove(hazard);
        Logger.world("Removed hazard: {}", hazard.getName());
    }
    
    /**
     * Adds a new trade route to the world
     */
    public void addTradeRoute(TradeRoute route) {
        tradeRoutes.add(route);
        Logger.world("Added trade route: {} with {} waypoints", 
                    route.getName(), route.getWaypoints().size());
    }
    
    /**
     * Removes a trade route from the world
     */
    public void removeTradeRoute(TradeRoute route) {
        tradeRoutes.remove(route);
        Logger.world("Removed trade route: {}", route.getName());
    }
    
    /**
     * Checks if world generation is currently in progress
     */
    public boolean isGenerating() {
        return isGenerating;
    }
    
    /**
     * Forces regeneration of a chunk
     */
    public void regenerateChunk(int chunkX, int chunkZ) {
        Vector2f chunkPos = new Vector2f(chunkX, chunkZ);
        loadedChunks.remove(chunkPos);
        Logger.world("Marked chunk ({}, {}) for regeneration", chunkX, chunkZ);
    }
    
    /**
     * Clears all loaded chunks (useful for memory management)
     */
    public void clearLoadedChunks() {
        loadedChunks.clear();
        Logger.world("Cleared all loaded chunks");
    }
    
    /**
     * Gets generation statistics
     */
    public GenerationStats getGenerationStats() {
        return new GenerationStats(
            loadedChunks.size(),
            islands.size(),
            tradeRoutes.size(),
            hazards.size()
        );
    }
    
    /**
     * Statistics about world generation
     */
    public static class GenerationStats {
        public final int loadedChunks;
        public final int islands;
        public final int tradeRoutes;
        public final int hazards;
        
        public GenerationStats(int loadedChunks, int islands, int tradeRoutes, int hazards) {
            this.loadedChunks = loadedChunks;
            this.islands = islands;
            this.tradeRoutes = tradeRoutes;
            this.hazards = hazards;
        }
        
        @Override
        public String toString() {
            return String.format("GenerationStats{chunks=%d, islands=%d, routes=%d, hazards=%d}",
                               loadedChunks, islands, tradeRoutes, hazards);
        }
    }
    

    
    /**
     * Simple noise generator using Java's Random
     */
    private static class NoiseGenerator {
        private final Random random;
        
        public NoiseGenerator(long seed) {
            this.random = new Random(seed);
        }
        
        public float noise(float x, float z) {
            // Simple interpolated noise
            int x0 = (int) Math.floor(x);
            int z0 = (int) Math.floor(z);
            
            float fx = x - x0;
            float fz = z - z0;
            
            float n00 = randomFloat(x0, z0);
            float n10 = randomFloat(x0 + 1, z0);
            float n01 = randomFloat(x0, z0 + 1);
            float n11 = randomFloat(x0 + 1, z0 + 1);
            
            float nx0 = MathUtils.lerp(n00, n10, fx);
            float nx1 = MathUtils.lerp(n01, n11, fx);
            
            return MathUtils.lerp(nx0, nx1, fz);
        }
        
        public float noise3D(float x, float y, float z) {
            // Simplified 3D noise
            return (noise(x, y) + noise(y, z) + noise(x, z)) / 3.0f;
        }
        
        public float sample(float x, float z) {
            // Alias for noise method to match expected interface
            return noise(x, z);
        }
        
        private float randomFloat(int x, int z) {
            random.setSeed(x * 374761393L + z * 668265263L);
            return random.nextFloat() * 2.0f - 1.0f;
        }
    }
}