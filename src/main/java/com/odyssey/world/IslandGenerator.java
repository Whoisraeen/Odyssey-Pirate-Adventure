package com.odyssey.world;

import com.odyssey.world.feature.*;
import com.odyssey.world.structure.*;
import com.odyssey.world.WorldConfig.IslandConfig;
import com.odyssey.world.noise.NoiseGenerator;
import com.odyssey.util.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Island generator - creates procedural islands with varied terrain, biomes, and structures
 */
public class IslandGenerator {
    
    // Logger instance
    private final Logger logger;
    
    // Generation parameters
    private final NoiseGenerator heightNoise;
    private final NoiseGenerator biomeNoise;
    private final NoiseGenerator featureNoise;
    private final NoiseGenerator caveNoise;
    
    // Island configuration
    private final IslandConfig config;
    private final Random random;
    
    // Generation cache
    private final Map<Vector2f, Island> islandCache;
    private final Map<Vector2f, IslandData> islandDataCache;
    
    public IslandGenerator(long seed, IslandConfig config) {
        this.logger = Logger.getLogger(IslandGenerator.class);
        this.config = config;
        this.random = new Random(seed);
        this.islandCache = new HashMap<>();
        this.islandDataCache = new HashMap<>();
        
        // Initialize noise generators with different seeds
        this.heightNoise = new NoiseGenerator(seed);
        this.biomeNoise = new NoiseGenerator(seed + 1000);
        this.featureNoise = new NoiseGenerator(seed + 2000);
        this.caveNoise = new NoiseGenerator(seed + 3000);
        
        logger.debug(Logger.WORLD, "Initialized island generator with seed {}", seed);
    }
    
    /**
     * Generates an island at the specified world coordinates
     */
    public Island generateIsland(float worldX, float worldZ, IslandType type) {
        Vector2f key = new Vector2f(worldX, worldZ);
        
        // Check cache first
        if (islandCache.containsKey(key)) {
            return islandCache.get(key);
        }
        
        // Generate new island
        Island island = createIsland(worldX, worldZ, type);
        islandCache.put(key, island);
        
        logger.debug(Logger.WORLD, "Generated {} island at ({}, {})", type.getDisplayName(), worldX, worldZ);
        
        return island;
    }
    
    /**
     * Gets island data for a region (used for world generation planning)
     */
    public IslandData getIslandData(float worldX, float worldZ) {
        Vector2f key = new Vector2f(worldX, worldZ);
        
        if (islandDataCache.containsKey(key)) {
            return islandDataCache.get(key);
        }
        
        IslandData data = generateIslandData(worldX, worldZ);
        islandDataCache.put(key, data);
        
        return data;
    }
    
    /**
     * Checks if there should be an island at the given coordinates
     */
    public boolean shouldGenerateIsland(float worldX, float worldZ) {
        // Use noise to determine island placement
        float islandNoise = heightNoise.sample(worldX * 0.001f, worldZ * 0.001f);
        
        // Islands are more likely in certain noise ranges
        return islandNoise > config.getThreshold();
    }
    
    /**
     * Determines the type of island to generate at given coordinates
     */
    public IslandType determineIslandType(float worldX, float worldZ) {
        float typeNoise = biomeNoise.sample(worldX * 0.0005f, worldZ * 0.0005f);
        float sizeNoise = featureNoise.sample(worldX * 0.0008f, worldZ * 0.0008f);
        
        // Determine size category first
        IslandSize size;
        if (sizeNoise > 0.7f) {
            size = IslandSize.LARGE;
        } else if (sizeNoise > 0.3f) {
            size = IslandSize.MEDIUM;
        } else {
            size = IslandSize.SMALL;
        }
        
        // Determine type based on noise and size
        if (typeNoise > 0.8f) {
            return size == IslandSize.LARGE ? IslandType.VOLCANIC_LARGE : IslandType.VOLCANIC_SMALL;
        } else if (typeNoise > 0.6f) {
            return size == IslandSize.LARGE ? IslandType.MOUNTAINOUS_LARGE : IslandType.MOUNTAINOUS_MEDIUM;
        } else if (typeNoise > 0.4f) {
            return size == IslandSize.MEDIUM ? IslandType.TROPICAL_MEDIUM : IslandType.TROPICAL_SMALL;
        } else if (typeNoise > 0.2f) {
            return IslandType.DESERT_MEDIUM;
        } else if (typeNoise > -0.2f) {
            return size == IslandSize.SMALL ? IslandType.ROCKY_SMALL : IslandType.SWAMP_MEDIUM;
        } else {
            return IslandType.CORAL_ATOLL;
        }
    }
    
    private Island createIsland(float worldX, float worldZ, IslandType type) {
        Island island = new Island(worldX, worldZ, type);
        
        // Generate island shape and heightmap
        generateIslandTerrain(island);
        
        // Generate biomes
        generateIslandBiomes(island);
        
        // Generate features (caves, ores, etc.)
        generateIslandFeatures(island);
        
        // Generate structures
        generateIslandStructures(island);
        
        // Generate vegetation
        generateIslandVegetation(island);
        
        return island;
    }
    
    private void generateIslandTerrain(Island island) {
        IslandType type = island.getType();
        float centerX = island.getCenterX();
        float centerZ = island.getCenterZ();
        float radius = type.getRadius();
        
        // Generate heightmap
        int size = (int) (radius * 2) + 20; // Add padding
        float[][] heightmap = new float[size][size];
        
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float worldPosX = centerX - radius + x;
                float worldPosZ = centerZ - radius + z;
                
                // Distance from island center
                float distance = (float) Math.sqrt(
                    (worldPosX - centerX) * (worldPosX - centerX) +
                    (worldPosZ - centerZ) * (worldPosZ - centerZ)
                );
                
                // Base height from island shape
                float height = calculateIslandHeight(distance, radius, type);
                
                // Add noise for terrain variation
                height += generateTerrainNoise(worldPosX, worldPosZ, type);
                
                // Ensure minimum ocean depth around island
                if (distance > radius + 10) {
                    height = Math.min(height, -5.0f);
                }
                
                heightmap[x][z] = height;
            }
        }
        
        island.setHeightmap(heightmap);
        island.setSize(size);
    }
    
    private float calculateIslandHeight(float distance, float radius, IslandType type) {
        if (distance > radius) {
            // Underwater slope
            float slopeDistance = distance - radius;
            return -slopeDistance * 0.5f - 2.0f;
        }
        
        // Island height profile
        float normalizedDistance = distance / radius;
        float baseHeight = type.getMaxHeight();
        
        // Different height profiles for different island types
        switch (type.getShape()) {
            case VOLCANIC:
                // Steep cone shape
                return baseHeight * (1.0f - normalizedDistance * normalizedDistance);
                
            case MOUNTAINOUS:
                // Multiple peaks
                float mountainNoise = heightNoise.sample(distance * 0.1f, 0) * 0.3f;
                return baseHeight * (1.0f - normalizedDistance) + mountainNoise * baseHeight;
                
            case FLAT:
                // Gentle slopes
                return baseHeight * (1.0f - normalizedDistance * 0.5f);
                
            case ATOLL:
                // Ring shape with lagoon
                if (normalizedDistance < 0.3f) {
                    return -2.0f; // Lagoon
                } else if (normalizedDistance < 0.7f) {
                    return baseHeight * 0.5f; // Reef
                } else {
                    return baseHeight * (1.0f - normalizedDistance);
                }
                
            default:
                return baseHeight * (1.0f - normalizedDistance);
        }
    }
    
    private float generateTerrainNoise(float worldX, float worldZ, IslandType type) {
        float noise = 0;
        
        // Multiple octaves of noise for detail
        noise += heightNoise.sample(worldX * 0.01f, worldZ * 0.01f) * 10.0f;
        noise += heightNoise.sample(worldX * 0.02f, worldZ * 0.02f) * 5.0f;
        noise += heightNoise.sample(worldX * 0.05f, worldZ * 0.05f) * 2.0f;
        noise += heightNoise.sample(worldX * 0.1f, worldZ * 0.1f) * 1.0f;
        
        // Scale based on island type
        return noise * type.getTerrainRoughness();
    }
    
    private void generateIslandBiomes(Island island) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        BiomeType[][] biomeMap = new BiomeType[size][size];
        
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float height = heightmap[x][z];
                float worldX = island.getCenterX() - island.getType().getRadius() + x;
                float worldZ = island.getCenterZ() - island.getType().getRadius() + z;
                
                BiomeType biome = determineBiome(worldX, worldZ, height, island.getType());
                biomeMap[x][z] = biome;
            }
        }
        
        island.setBiomeMap(biomeMap);
    }
    
    private BiomeType determineBiome(float worldX, float worldZ, float height, IslandType islandType) {
        // Ocean biomes
        if (height < 0) {
            if (height < -20) {
                return BiomeType.DEEP_OCEAN;
            } else if (height < -5) {
                return BiomeType.OCEAN;
            } else {
                return BiomeType.SHALLOW_WATER;
            }
        }
        
        // Land biomes based on height and island type
        float biomeNoise = this.biomeNoise.sample(worldX * 0.01f, worldZ * 0.01f);
        
        // Beach areas (low elevation near water)
        if (height < 2.0f) {
            return BiomeType.BEACH;
        }
        
        // Island type influences biome selection
        switch (islandType.getClimate()) {
            case TROPICAL:
                if (height > 30.0f) {
                    return BiomeType.MOUNTAIN;
                } else if (height > 15.0f) {
                    return biomeNoise > 0 ? BiomeType.TROPICAL_FOREST : BiomeType.JUNGLE;
                } else {
                    return BiomeType.TROPICAL_GRASSLAND;
                }
                
            case TEMPERATE:
                if (height > 25.0f) {
                    return BiomeType.MOUNTAIN;
                } else if (height > 10.0f) {
                    return biomeNoise > 0 ? BiomeType.FOREST : BiomeType.HILLS;
                } else {
                    return BiomeType.GRASSLAND;
                }
                
            case ARID:
                if (height > 20.0f) {
                    return BiomeType.MOUNTAIN;
                } else {
                    return biomeNoise > 0.5f ? BiomeType.DESERT : BiomeType.SAVANNA;
                }
                
            case VOLCANIC:
                if (height > 40.0f) {
                    return BiomeType.VOLCANIC_PEAK;
                } else if (height > 20.0f) {
                    return BiomeType.VOLCANIC_SLOPES;
                } else {
                    return BiomeType.VOLCANIC_PLAINS;
                }
                
            case SWAMP:
                return height > 10.0f ? BiomeType.SWAMP_HILLS : BiomeType.SWAMP;
                
            default:
                return BiomeType.GRASSLAND;
        }
    }
    
    private void generateIslandFeatures(Island island) {
        List<Feature> features = new ArrayList<>();
        
        // Generate caves
        generateCaves(island, features);
        
        // Generate ore deposits
        generateOreDeposits(island, features);
        
        // Generate springs and water features
        generateWaterFeatures(island, features);
        
        // Generate special geological features
        generateGeologicalFeatures(island, features);
        
        island.setFeatures(features);
    }
    
    private void generateCaves(Island island, List<Feature> features) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        
        // Cave generation based on noise
        for (int x = 10; x < size - 10; x += 5) {
            for (int z = 10; z < size - 10; z += 5) {
                float height = heightmap[x][z];
                
                if (height > 5.0f) { // Only generate caves above sea level
                    float worldX = island.getCenterX() - island.getType().getRadius() + x;
                    float worldZ = island.getCenterZ() - island.getType().getRadius() + z;
                    
                    float caveChance = caveNoise.sample(worldX * 0.02f, worldZ * 0.02f);
                    
                    if (caveChance > 0.7f) {
                        int depth = (int)(random.nextFloat() * 10 + 5);
                        int roomCount = (int)(random.nextFloat() * 5 + 3);
                        CaveFeature cave = new CaveFeature(worldX, height - 5, worldZ, depth, roomCount);
                        features.add(cave);
                    }
                }
            }
        }
    }
    
    private void generateOreDeposits(Island island, List<Feature> features) {
        IslandType type = island.getType();
        
        // Different islands have different ore types
        List<OreType> availableOres = getAvailableOres(type);
        
        for (OreType oreType : availableOres) {
            int depositCount = random.nextInt(oreType.getMaxDeposits()) + 1;
            
            for (int i = 0; i < depositCount; i++) {
                float x = island.getCenterX() + (random.nextFloat() - 0.5f) * type.getRadius();
                float z = island.getCenterZ() + (random.nextFloat() - 0.5f) * type.getRadius();
                float y = getHeightAt(island, x, z) - random.nextFloat() * 20;
                
                if (y > -10) { // Don't place ores too deep underwater
                    OreDeposit.DepositType depositType = OreDeposit.DepositType.SHALLOW_VEIN;
                    if (random.nextFloat() < 0.3f) {
                        depositType = OreDeposit.DepositType.DEEP_VEIN;
                    }
                    OreDeposit deposit = new OreDeposit(x, y, z, depositType, oreType);
                    features.add(deposit);
                }
            }
        }
    }
    
    private void generateWaterFeatures(Island island, List<Feature> features) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        
        // Generate springs on higher elevations
        for (int x = 20; x < size - 20; x += 15) {
            for (int z = 20; z < size - 20; z += 15) {
                float height = heightmap[x][z];
                
                if (height > 20.0f) {
                    float springChance = featureNoise.sample(x * 0.1f, z * 0.1f);
                    
                    if (springChance > 0.8f) {
                        float worldX = island.getCenterX() - island.getType().getRadius() + x;
                        float worldZ = island.getCenterZ() - island.getType().getRadius() + z;
                        
                        SpringFeature.SpringType springType = SpringFeature.SpringType.NATURAL;
                        if (random.nextFloat() < 0.2f) {
                            springType = SpringFeature.SpringType.HOT_SPRING;
                        }
                        SpringFeature spring = new SpringFeature(worldX, height, worldZ, springType);
                        features.add(spring);
                    }
                }
            }
        }
    }
    
    private void generateGeologicalFeatures(Island island, List<Feature> features) {
        IslandType type = island.getType();
        
        // Volcanic features for volcanic islands
        if (type.getClimate() == IslandClimate.VOLCANIC) {
            generateVolcanicFeatures(island, features);
        }
        
        // Coral features for atolls
        if (type.getShape() == IslandShape.ATOLL) {
            generateCoralFeatures(island, features);
        }
    }
    
    private void generateVolcanicFeatures(Island island, List<Feature> features) {
        // Central crater
        VolcanicCrater crater = new VolcanicCrater(
            island.getCenterX(), 
            island.getType().getMaxHeight(), 
            island.getCenterZ(),
            island.getType().getRadius() * 0.1f,
            10.0f
        );
        crater.setActivityLevel(random.nextFloat() < 0.3f ? 
            VolcanicCrater.ActivityLevel.ACTIVE : VolcanicCrater.ActivityLevel.DORMANT);
        features.add(crater);
        
        // Lava tubes
        int tubeCount = random.nextInt(3) + 1;
        for (int i = 0; i < tubeCount; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = random.nextFloat() * island.getType().getRadius() * 0.5f;
            
            float x = island.getCenterX() + (float) Math.cos(angle) * distance;
            float z = island.getCenterZ() + (float) Math.sin(angle) * distance;
            float y = getHeightAt(island, x, z);
            
            float length = random.nextFloat() * 50 + 20;
            float width = random.nextFloat() * 3 + 2;
            float height = random.nextFloat() * 2 + 1.5f;
            LavaTube tube = new LavaTube(x, y, z, length, width, height);
            features.add(tube);
        }
    }
    
    private void generateCoralFeatures(Island island, List<Feature> features) {
        float radius = island.getType().getRadius();
        
        // Generate coral reef around the atoll
        int reefPoints = 20;
        for (int i = 0; i < reefPoints; i++) {
            float angle = (float) i / reefPoints * (float) Math.PI * 2;
            float reefRadius = radius * (0.7f + random.nextFloat() * 0.2f);
            
            float x = island.getCenterX() + (float) Math.cos(angle) * reefRadius;
            float z = island.getCenterZ() + (float) Math.sin(angle) * reefRadius;
            
            CoralReef.ReefType reefType = CoralReef.ReefType.BARRIER_REEF;
            if (random.nextFloat() < 0.3f) {
                reefType = CoralReef.ReefType.ATOLL_REEF;
            }
            float depth = random.nextFloat() * 5 + 2;
            CoralReef reef = new CoralReef(x, -1, z, reefType, depth);
            features.add(reef);
        }
    }
    
    private void generateIslandStructures(Island island) {
        List<Structure> structures = new ArrayList<>();
        
        // Generate ruins and abandoned structures
        generateRuins(island, structures);
        
        // Generate pirate hideouts
        generatePirateHideouts(island, structures);
        
        // Generate treasure locations
        generateTreasureLocations(island, structures);
        
        // Generate natural landmarks
        generateLandmarks(island, structures);
        
        island.setStructures(structures);
    }
    
    private void generateRuins(Island island, List<Structure> structures) {
        IslandType type = island.getType();
        
        // Larger islands more likely to have ruins
        float ruinChance = type.getRadius() / 100.0f;
        
        if (random.nextFloat() < ruinChance) {
            // Find suitable location (flat area above sea level)
            Vector3f location = findSuitableStructureLocation(island, 10.0f, 0.5f);
            
            if (location != null) {
                StructureType ruinType = random.nextBoolean() ? 
                    StructureType.TEMPLE : StructureType.ANCIENT_RUINS;
                
                Structure ruin;
                if (ruinType == StructureType.TEMPLE) {
                    ruin = new Temple(location);
                } else {
                    ruin = new AncientRuins(location);
                }
                structures.add(ruin);
            }
        }
    }
    
    private void generatePirateHideouts(Island island, List<Structure> structures) {
        // Small chance for pirate hideout
        if (random.nextFloat() < 0.15f) {
            Vector3f location = findSuitableStructureLocation(island, 5.0f, 0.3f);
            
            if (location != null) {
                Structure hideout = new PirateCamp(location);
                structures.add(hideout);
            }
        }
    }
    
    private void generateTreasureLocations(Island island, List<Structure> structures) {
        // Multiple possible treasure locations per island
        int treasureCount = random.nextInt(3) + 1;
        
        for (int i = 0; i < treasureCount; i++) {
            if (random.nextFloat() < 0.3f) { // 30% chance per location
                Vector3f location = findRandomLocation(island);
                
                if (location != null) {
                    Structure treasure = new TreasureCave(location);
                    structures.add(treasure);
                }
            }
        }
    }
    
    private void generateLandmarks(Island island, List<Structure> structures) {
        IslandType type = island.getType();
        
        // Generate lighthouse on coastal areas
        if (type.getRadius() > 50 && random.nextFloat() < 0.4f) {
            Vector3f coastalLocation = findCoastalLocation(island);
            
            if (coastalLocation != null) {
                Structure lighthouse = new Lighthouse(coastalLocation);
                structures.add(lighthouse);
            }
        }
        
        // Generate watchtower on high points
        if (type.getMaxHeight() > 30 && random.nextFloat() < 0.3f) {
            Vector3f highLocation = findHighestLocation(island);
            
            if (highLocation != null) {
                Structure watchtower = new Watchtower(highLocation);
                structures.add(watchtower);
            }
        }
    }
    
    private void generateIslandVegetation(Island island) {
        BiomeType[][] biomeMap = island.getBiomeMap();
        int size = island.getSize();
        VegetationType[][] vegetationMap = new VegetationType[size][size];
        
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BiomeType biome = biomeMap[x][z];
                float height = island.getHeightmap()[x][z];
                
                VegetationType vegetation = determineVegetation(biome, height);
                vegetationMap[x][z] = vegetation;
            }
        }
        
        island.setVegetationMap(vegetationMap);
    }
    
    private VegetationType determineVegetation(BiomeType biome, float height) {
        if (height < 0) {
            return VegetationType.NONE; // Underwater
        }
        
        float vegetationNoise = featureNoise.sample(height * 0.1f, 0);
        
        switch (biome) {
            case TROPICAL_FOREST:
            case JUNGLE:
                return vegetationNoise > 0.3f ? VegetationType.DENSE_FOREST : VegetationType.TROPICAL_TREES;
                
            case FOREST:
                return vegetationNoise > 0.4f ? VegetationType.TEMPERATE_FOREST : VegetationType.SCATTERED_TREES;
                
            case GRASSLAND:
            case TROPICAL_GRASSLAND:
                return vegetationNoise > 0.6f ? VegetationType.TALL_GRASS : VegetationType.SHORT_GRASS;
                
            case DESERT:
                return vegetationNoise > 0.8f ? VegetationType.CACTI : VegetationType.SPARSE_VEGETATION;
                
            case SWAMP:
                return VegetationType.SWAMP_TREES;
                
            case BEACH:
                return vegetationNoise > 0.7f ? VegetationType.PALM_TREES : VegetationType.BEACH_GRASS;
                
            case MOUNTAIN:
                return height > 50 ? VegetationType.NONE : VegetationType.MOUNTAIN_SHRUBS;
                
            default:
                return VegetationType.SPARSE_VEGETATION;
        }
    }
    
    // Helper methods
    private IslandData generateIslandData(float worldX, float worldZ) {
        boolean hasIsland = shouldGenerateIsland(worldX, worldZ);
        
        if (!hasIsland) {
            return new IslandData(worldX, worldZ, null, false);
        }
        
        IslandType type = determineIslandType(worldX, worldZ);
        return new IslandData(worldX, worldZ, type, true);
    }
    
    private float getHeightAt(Island island, float worldX, float worldZ) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        float radius = island.getType().getRadius();
        
        // Convert world coordinates to heightmap coordinates
        int x = (int) (worldX - island.getCenterX() + radius);
        int z = (int) (worldZ - island.getCenterZ() + radius);
        
        if (x >= 0 && x < size && z >= 0 && z < size) {
            return heightmap[x][z];
        }
        
        return -10.0f; // Default ocean depth
    }
    
    private List<OreType> getAvailableOres(IslandType islandType) {
        List<OreType> ores = new ArrayList<>();
        
        // Common ores on all islands
        ores.add(OreType.IRON);
        ores.add(OreType.COPPER);
        
        // Type-specific ores
        switch (islandType.getClimate()) {
            case VOLCANIC:
                ores.add(OreType.SULFUR);
                ores.add(OreType.OBSIDIAN);
                ores.add(OreType.GOLD);
                break;
                
            case TROPICAL:
                ores.add(OreType.TIN);
                ores.add(OreType.SILVER);
                break;
                
            case ARID:
                ores.add(OreType.SALT);
                ores.add(OreType.GEMS);
                break;
                
            case TEMPERATE:
                ores.add(OreType.COAL);
                ores.add(OreType.LIMESTONE);
                break;
                
            case SWAMP:
                ores.add(OreType.CURSED_GOLD);
                ores.add(OreType.STORM_CRYSTAL);
                ores.add(OreType.IRON);
                ores.add(OreType.TIN);
                break;
        }
        
        return ores;
    }
    
    private Vector3f findSuitableStructureLocation(Island island, float minHeight, float maxSlope) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = random.nextInt(size - 20) + 10;
            int z = random.nextInt(size - 20) + 10;
            
            float height = heightmap[x][z];
            
            if (height > minHeight) {
                // Check slope
                float slope = calculateSlope(heightmap, x, z, size);
                
                if (slope < maxSlope) {
                    float worldX = island.getCenterX() - island.getType().getRadius() + x;
                    float worldZ = island.getCenterZ() - island.getType().getRadius() + z;
                    return new Vector3f(worldX, height, worldZ);
                }
            }
        }
        
        return null;
    }
    
    private Vector3f findRandomLocation(Island island) {
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = random.nextFloat() * island.getType().getRadius() * 0.8f;
        
        float x = island.getCenterX() + (float) Math.cos(angle) * distance;
        float z = island.getCenterZ() + (float) Math.sin(angle) * distance;
        float y = getHeightAt(island, x, z);
        
        if (y > 0) {
            return new Vector3f(x, y, z);
        }
        
        return null;
    }
    
    private Vector3f findCoastalLocation(Island island) {
        float radius = island.getType().getRadius();
        
        for (int attempt = 0; attempt < 20; attempt++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = radius * (0.8f + random.nextFloat() * 0.15f);
            
            float x = island.getCenterX() + (float) Math.cos(angle) * distance;
            float z = island.getCenterZ() + (float) Math.sin(angle) * distance;
            float y = getHeightAt(island, x, z);
            
            if (y > 2 && y < 15) {
                return new Vector3f(x, y, z);
            }
        }
        
        return null;
    }
    
    private Vector3f findHighestLocation(Island island) {
        float[][] heightmap = island.getHeightmap();
        int size = island.getSize();
        
        float maxHeight = -Float.MAX_VALUE;
        int bestX = 0, bestZ = 0;
        
        for (int x = 10; x < size - 10; x++) {
            for (int z = 10; z < size - 10; z++) {
                if (heightmap[x][z] > maxHeight) {
                    maxHeight = heightmap[x][z];
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        
        if (maxHeight > 20) {
            float worldX = island.getCenterX() - island.getType().getRadius() + bestX;
            float worldZ = island.getCenterZ() - island.getType().getRadius() + bestZ;
            return new Vector3f(worldX, maxHeight, worldZ);
        }
        
        return null;
    }
    
    private float calculateSlope(float[][] heightmap, int x, int z, int size) {
        if (x <= 0 || x >= size - 1 || z <= 0 || z >= size - 1) {
            return Float.MAX_VALUE;
        }
        
        float center = heightmap[x][z];
        float maxDiff = 0;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                float neighbor = heightmap[x + dx][z + dz];
                float diff = Math.abs(neighbor - center);
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        return maxDiff;
    }
    
    // Getters
    public IslandConfig getConfig() { return config; }
    public void clearCache() { 
        islandCache.clear(); 
        islandDataCache.clear();
    }
}