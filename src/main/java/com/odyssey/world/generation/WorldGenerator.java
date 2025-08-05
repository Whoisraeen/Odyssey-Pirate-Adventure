package com.odyssey.world.generation;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced procedural world generation system for The Odyssey.
 * Implements sophisticated island generation, terrain features, and geological systems.
 */
public class WorldGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WorldGenerator.class);
    
    private final long seed;
    private final Random random;
    
    // Noise generators for different terrain features
    private final NoiseGenerator continentNoise;
    private final NoiseGenerator islandNoise;
    private final NoiseGenerator mountainNoise;
    private final NoiseGenerator caveNoise;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    
    // Generation parameters
    private static final int SEA_LEVEL = 64;
    private static final float CONTINENT_SCALE = 0.0005f;
    private static final float ISLAND_SCALE = 0.003f;
    private static final float MOUNTAIN_SCALE = 0.01f;
    
    // Geological features
    private final List<Island> generatedIslands = new ArrayList<>();
    private final List<VolcanicIsland> volcanicIslands = new ArrayList<>();
    private final List<UndergroundCave> caveSystems = new ArrayList<>();
    private final Map<Vector2f, GeologicalFeature> geologicalFeatures = new HashMap<>();
    
    // Ore deposits and resources
    private final Map<Vector3f, OreDeposit> oreDeposits = new HashMap<>();
    private final List<TreasureLocation> treasureLocations = new ArrayList<>();
    
    public enum BlockType {
        AIR(0), WATER(1), SAND(2), STONE(3), DIRT(4), GRASS(5),
        WOOD(6), LEAVES(7), COAL_ORE(8), IRON_ORE(9), GOLD_ORE(10),
        OBSIDIAN(11), LAVA(12), CORAL(13), SEAWEED(14), PALM_WOOD(15),
        VOLCANIC_ROCK(16), LIMESTONE(17), MARBLE(18), CRYSTAL(19);
        
        public final int id;
        
        BlockType(int id) {
            this.id = id;
        }
    }
    
    public static class Island {
        public Vector2f center;
        public float radius;
        public float height;
        public IslandType type;
        public List<Vector2f> beaches;
        public List<Vector2f> harbors;
        public Map<String, Integer> resources;
        
        public Island(Vector2f center, float radius, float height, IslandType type) {
            this.center = new Vector2f(center);
            this.radius = radius;
            this.height = height;
            this.type = type;
            this.beaches = new ArrayList<>();
            this.harbors = new ArrayList<>();
            this.resources = new HashMap<>();
        }
    }
    
    public enum IslandType {
        TROPICAL_ATOLL, VOLCANIC_SPIRE, DENSE_JUNGLE, MANGROVE_SWAMP,
        CURSED_ISLE, ARCTIC_ARCHIPELAGO, DESERT_OASIS, CORAL_REEF,
        FLOATING_GARDEN, BONE_ISLAND, STORM_TOUCHED, MAGNETIC_ANOMALY
    }
    
    public static class VolcanicIsland extends Island {
        public Vector2f craterCenter;
        public float craterRadius;
        public boolean isActive;
        public double lastEruption;
        public float lavaLevel;
        
        public VolcanicIsland(Vector2f center, float radius, float height) {
            super(center, radius, height, IslandType.VOLCANIC_SPIRE);
            this.craterCenter = new Vector2f(center).add(
                (float)(Math.random() - 0.5) * radius * 0.3f,
                (float)(Math.random() - 0.5) * radius * 0.3f
            );
            this.craterRadius = radius * 0.15f;
            this.isActive = Math.random() < 0.3; // 30% chance of active volcano
            this.lastEruption = 0.0;
            this.lavaLevel = isActive ? height * 0.8f : 0;
        }
    }
    
    public static class UndergroundCave {
        public Vector3f entrance;
        public List<Vector3f> chambers;
        public Map<Vector3f, String> treasures;
        public float depth;
        public boolean isFlooded;
        
        public UndergroundCave(Vector3f entrance) {
            this.entrance = new Vector3f(entrance);
            this.chambers = new ArrayList<>();
            this.treasures = new HashMap<>();
            this.depth = 20 + (float)Math.random() * 50; // 20-70 blocks deep
            this.isFlooded = entrance.y < SEA_LEVEL + 10;
            
            generateChambers();
        }
        
        private void generateChambers() {
            int chamberCount = 3 + (int)(Math.random() * 5); // 3-8 chambers
            Vector3f currentPos = new Vector3f(entrance);
            
            for (int i = 0; i < chamberCount; i++) {
                chambers.add(new Vector3f(currentPos));
                
                // Move to next chamber position
                currentPos.add(
                    (float)(Math.random() - 0.5) * 30,
                    -(float)Math.random() * 15 - 5, // Go deeper
                    (float)(Math.random() - 0.5) * 30
                );
                
                // Occasionally place treasure
                if (Math.random() < 0.4) {
                    String[] treasureTypes = {"ancient_artifact", "gold_coins", "rare_gems", "magical_crystal"};
                    String treasure = treasureTypes[(int)(Math.random() * treasureTypes.length)];
                    treasures.put(new Vector3f(currentPos), treasure);
                }
            }
        }
    }
    
    public static class GeologicalFeature {
        public Vector2f position;
        public FeatureType type;
        public float intensity;
        public Map<String, Object> properties;
        
        public GeologicalFeature(Vector2f position, FeatureType type, float intensity) {
            this.position = new Vector2f(position);
            this.type = type;
            this.intensity = intensity;
            this.properties = new HashMap<>();
        }
    }
    
    public enum FeatureType {
        HOT_SPRING, UNDERWATER_VENT, TECTONIC_FAULT, CORAL_FORMATION,
        MINERAL_VEIN, ANCIENT_RUINS, SHIPWRECK, WHIRLPOOL
    }
    
    public static class OreDeposit {
        public Vector3f position;
        public BlockType oreType;
        public int quantity;
        public float richness;
        public boolean isExposed;
        
        public OreDeposit(Vector3f position, BlockType oreType, int quantity, float richness) {
            this.position = new Vector3f(position);
            this.oreType = oreType;
            this.quantity = quantity;
            this.richness = richness;
            this.isExposed = Math.random() < 0.2; // 20% chance of surface exposure
        }
    }
    
    public static class TreasureLocation {
        public Vector3f position;
        public TreasureType type;
        public boolean isHidden;
        public List<String> treasureItems;
        public String mapFragment;
        
        public TreasureLocation(Vector3f position, TreasureType type) {
            this.position = new Vector3f(position);
            this.type = type;
            this.isHidden = type != TreasureType.VISIBLE_WRECK;
            this.treasureItems = new ArrayList<>();
            this.mapFragment = generateMapFragment();
            
            generateTreasureContents();
        }
        
        private void generateTreasureContents() {
            String[] possibleTreasures = {
                "gold_coins", "silver_coins", "precious_gems", "ancient_artifact",
                "magical_scroll", "rare_weapon", "navigation_instrument", "exotic_spices"
            };
            
            int itemCount = 1 + (int)(Math.random() * 4); // 1-5 items
            for (int i = 0; i < itemCount; i++) {
                treasureItems.add(possibleTreasures[(int)(Math.random() * possibleTreasures.length)]);
            }
        }
        
        private String generateMapFragment() {
            return "Fragment_" + (int)(Math.random() * 1000);
        }
    }
    
    public enum TreasureType {
        BURIED_CHEST, SUNKEN_SHIP, CAVE_HOARD, VISIBLE_WRECK, ANCIENT_VAULT
    }
    
    // Simple Perlin-like noise generator
    public static class NoiseGenerator {
        private final long seed;
        private final Random random;
        
        public NoiseGenerator(long seed) {
            this.seed = seed;
            this.random = new Random(seed);
        }
        
        public float noise(float x, float y) {
            // Simplified noise implementation
            int xi = (int)Math.floor(x);
            int yi = (int)Math.floor(y);
            
            float xf = x - xi;
            float yf = y - yi;
            
            float a = dotGridGradient(xi, yi, x, y);
            float b = dotGridGradient(xi + 1, yi, x, y);
            float c = dotGridGradient(xi, yi + 1, x, y);
            float d = dotGridGradient(xi + 1, yi + 1, x, y);
            
            float i1 = interpolate(a, b, xf);
            float i2 = interpolate(c, d, xf);
            
            return interpolate(i1, i2, yf);
        }
        
        private float dotGridGradient(int ix, int iy, float x, float y) {
            Random gridRandom = new Random(seed + ix * 374761393L + iy * 668265263L);
            float gradientX = (gridRandom.nextFloat() - 0.5f) * 2;
            float gradientY = (gridRandom.nextFloat() - 0.5f) * 2;
            
            float dx = x - ix;
            float dy = y - iy;
            
            return dx * gradientX + dy * gradientY;
        }
        
        private float interpolate(float a, float b, float t) {
            return a + smoothstep(t) * (b - a);
        }
        
        private float smoothstep(float t) {
            return t * t * (3.0f - 2.0f * t);
        }
        
        public float octaveNoise(float x, float y, int octaves, float persistence) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                total += noise(x * frequency, y * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= persistence;
                frequency *= 2;
            }
            
            return total / maxValue;
        }
    }
    
    public WorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        
        // Initialize noise generators with different seeds
        this.continentNoise = new NoiseGenerator(seed);
        this.islandNoise = new NoiseGenerator(seed + 1);
        this.mountainNoise = new NoiseGenerator(seed + 2);
        this.caveNoise = new NoiseGenerator(seed + 3);
        this.temperatureNoise = new NoiseGenerator(seed + 4);
        this.humidityNoise = new NoiseGenerator(seed + 5);
    }
    
    public void initialize() {
        logger.info("Initializing advanced world generator with seed: {}", seed);
        
        // Pre-generate some major islands
        generateMajorIslands(20);
        
        // Generate volcanic islands
        generateVolcanicIslands(8);
        
        // Generate geological features
        generateGeologicalFeatures(50);
        
        // Generate ore deposits
        generateOreDeposits(200);
        
        // Generate treasure locations
        generateTreasureLocations(30);
        
        logger.info("World generator initialized with {} islands, {} volcanic islands, {} geological features", 
                   generatedIslands.size(), volcanicIslands.size(), geologicalFeatures.size());
    }
    
    private void generateMajorIslands(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (random.nextFloat() - 0.5f) * 4000, // Spread over 4km radius
                (random.nextFloat() - 0.5f) * 4000
            );
            
            float radius = 50 + random.nextFloat() * 200; // 50-250 block radius
            float height = 20 + random.nextFloat() * 60; // 20-80 blocks above sea level
            
            IslandType type = IslandType.values()[random.nextInt(IslandType.values().length)];
            Island island = new Island(center, radius, height, type);
            
            // Generate island features
            generateIslandFeatures(island);
            
            generatedIslands.add(island);
        }
    }
    
    private void generateIslandFeatures(Island island) {
        // Generate beaches around the perimeter
        int beachCount = 3 + random.nextInt(6); // 3-8 beaches
        for (int i = 0; i < beachCount; i++) {
            float angle = (float)i / beachCount * 2 * (float)Math.PI;
            float beachDistance = island.radius * (0.8f + random.nextFloat() * 0.2f);
            
            Vector2f beach = new Vector2f(
                island.center.x + (float)Math.cos(angle) * beachDistance,
                island.center.y + (float)Math.sin(angle) * beachDistance
            );
            island.beaches.add(beach);
            
            // Some beaches become natural harbors
            if (random.nextFloat() < 0.4) {
                island.harbors.add(beach);
            }
        }
        
        // Generate resources based on island type
        generateIslandResources(island);
    }
    
    private void generateIslandResources(Island island) {
        switch (island.type) {
            case TROPICAL_ATOLL -> {
                island.resources.put("coconuts", 50 + random.nextInt(100));
                island.resources.put("palm_wood", 20 + random.nextInt(50));
                island.resources.put("coral", 30 + random.nextInt(70));
                island.resources.put("fish", 40 + random.nextInt(80));
            }
            case VOLCANIC_SPIRE -> {
                island.resources.put("obsidian", 30 + random.nextInt(60));
                island.resources.put("sulfur", 20 + random.nextInt(40));
                island.resources.put("rare_gems", 5 + random.nextInt(15));
                island.resources.put("volcanic_glass", 10 + random.nextInt(25));
            }
            case DENSE_JUNGLE -> {
                island.resources.put("hardwood", 100 + random.nextInt(200));
                island.resources.put("exotic_fruits", 60 + random.nextInt(120));
                island.resources.put("medicinal_herbs", 30 + random.nextInt(60));
                island.resources.put("rare_animals", 10 + random.nextInt(20));
            }
            case ARCTIC_ARCHIPELAGO -> {
                island.resources.put("pine_wood", 80 + random.nextInt(160));
                island.resources.put("seal_blubber", 20 + random.nextInt(40));
                island.resources.put("ice_crystals", 15 + random.nextInt(30));
                island.resources.put("whale_bone", 5 + random.nextInt(15));
            }
        }
    }
    
    private void generateVolcanicIslands(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (random.nextFloat() - 0.5f) * 3000,
                (random.nextFloat() - 0.5f) * 3000
            );
            
            float radius = 80 + random.nextFloat() * 120; // Larger than normal islands
            float height = 60 + random.nextFloat() * 100; // Taller
            
            VolcanicIsland volcano = new VolcanicIsland(center, radius, height);
            volcanicIslands.add(volcano);
        }
    }
    
    private void generateGeologicalFeatures(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f position = new Vector2f(
                (random.nextFloat() - 0.5f) * 5000,
                (random.nextFloat() - 0.5f) * 5000
            );
            
            FeatureType type = FeatureType.values()[random.nextInt(FeatureType.values().length)];
            float intensity = 0.3f + random.nextFloat() * 0.7f;
            
            GeologicalFeature feature = new GeologicalFeature(position, type, intensity);
            
            // Add type-specific properties
            switch (type) {
                case HOT_SPRING -> {
                    feature.properties.put("temperature", 60 + random.nextInt(40)); // 60-100°C
                    feature.properties.put("healing_properties", random.nextFloat() < 0.3);
                }
                case ANCIENT_RUINS -> {
                    feature.properties.put("age", 500 + random.nextInt(2000)); // 500-2500 years
                    feature.properties.put("civilization", "Ancient_" + random.nextInt(10));
                    feature.properties.put("treasure_chance", 0.6f + random.nextFloat() * 0.4f);
                }
                case MINERAL_VEIN -> {
                    BlockType[] ores = {BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.GOLD_ORE};
                    feature.properties.put("ore_type", ores[random.nextInt(ores.length)]);
                    feature.properties.put("richness", intensity);
                }
            }
            
            geologicalFeatures.put(position, feature);
        }
    }
    
    private void generateOreDeposits(int count) {
        BlockType[] oreTypes = {BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.GOLD_ORE};
        float[] rarities = {0.6f, 0.3f, 0.1f}; // Coal common, iron uncommon, gold rare
        
        for (int i = 0; i < count; i++) {
            Vector3f position = new Vector3f(
                (random.nextFloat() - 0.5f) * 4000,
                random.nextFloat() * -100 - 10, // Underground: -10 to -110
                (random.nextFloat() - 0.5f) * 4000
            );
            
            // Select ore type based on rarity
            BlockType oreType = BlockType.COAL_ORE;
            float roll = random.nextFloat();
            float cumulative = 0;
            
            for (int j = 0; j < oreTypes.length; j++) {
                cumulative += rarities[j];
                if (roll <= cumulative) {
                    oreType = oreTypes[j];
                    break;
                }
            }
            
            int quantity = switch (oreType) {
                case COAL_ORE -> 50 + random.nextInt(150);
                case IRON_ORE -> 20 + random.nextInt(80);
                case GOLD_ORE -> 5 + random.nextInt(20);
                default -> 10;
            };
            
            float richness = 0.3f + random.nextFloat() * 0.7f;
            
            oreDeposits.put(position, new OreDeposit(position, oreType, quantity, richness));
        }
    }
    
    private void generateTreasureLocations(int count) {
        TreasureType[] types = TreasureType.values();
        
        for (int i = 0; i < count; i++) {
            Vector3f position = new Vector3f(
                (random.nextFloat() - 0.5f) * 4000,
                random.nextFloat() * 20 + SEA_LEVEL - 30, // Can be above or below sea level
                (random.nextFloat() - 0.5f) * 4000
            );
            
            TreasureType type = types[random.nextInt(types.length)];
            TreasureLocation treasure = new TreasureLocation(position, type);
            
            treasureLocations.add(treasure);
        }
    }
    
    public void update(double deltaTime) {
        // Update volcanic activity
        updateVolcanicActivity(deltaTime);
        
        // Update geological processes (very slow)
        updateGeologicalProcesses(deltaTime);
    }
    
    private void updateVolcanicActivity(double deltaTime) {
        for (VolcanicIsland volcano : volcanicIslands) {
            if (volcano.isActive) {
                volcano.lastEruption += deltaTime;
                
                // Random chance of eruption (very rare)
                if (volcano.lastEruption > 3600 && random.nextFloat() < deltaTime * 0.0001) { // Once per ~3 hours minimum
                    triggerVolcanicEruption(volcano);
                    volcano.lastEruption = 0.0;
                }
            }
        }
    }
    
    private void triggerVolcanicEruption(VolcanicIsland volcano) {
        logger.info("Volcanic eruption at {} with intensity {}", volcano.center, volcano.lavaLevel);
        
        // Increase lava level
        volcano.lavaLevel = Math.min(volcano.height, volcano.lavaLevel + 5 + random.nextFloat() * 10);
        
        // Create new geological features around the volcano
        for (int i = 0; i < 3; i++) {
            Vector2f newFeaturePos = new Vector2f(volcano.center).add(
                (random.nextFloat() - 0.5f) * volcano.radius * 2,
                (random.nextFloat() - 0.5f) * volcano.radius * 2
            );
            
            GeologicalFeature lavaFeature = new GeologicalFeature(
                newFeaturePos, FeatureType.UNDERWATER_VENT, 0.8f + random.nextFloat() * 0.2f
            );
            
            geologicalFeatures.put(newFeaturePos, lavaFeature);
        }
    }
    
    private void updateGeologicalProcesses(double deltaTime) {
        // Very slow geological changes
        // This could include island erosion, new island formation, etc.
        
        // Occasionally spawn new small islands
        if (random.nextFloat() < deltaTime * 0.00001) { // Extremely rare
            Vector2f newIslandPos = new Vector2f(
                (random.nextFloat() - 0.5f) * 6000,
                (random.nextFloat() - 0.5f) * 6000
            );
            
            Island newIsland = new Island(newIslandPos, 20 + random.nextFloat() * 50, 
                                        10 + random.nextFloat() * 30, IslandType.TROPICAL_ATOLL);
            generatedIslands.add(newIsland);
            
            logger.info("New island formed at {} due to geological activity", newIslandPos);
        }
    }
    
    public float getHeightAt(float x, float z) {
        float height = SEA_LEVEL - 20; // Default ocean floor
        
        // Check if position is on any generated island
        for (Island island : generatedIslands) {
            float distance = new Vector2f(x, z).distance(island.center);
            if (distance <= island.radius) {
                // Use smooth falloff from center to edge
                float falloff = 1.0f - (distance / island.radius);
                falloff = falloff * falloff; // Smooth curve
                height = Math.max(height, SEA_LEVEL + island.height * falloff);
            }
        }
        
        // Check volcanic islands
        for (VolcanicIsland volcano : volcanicIslands) {
            float distance = new Vector2f(x, z).distance(volcano.center);
            if (distance <= volcano.radius) {
                float falloff = 1.0f - (distance / volcano.radius);
                falloff = falloff * falloff;
                
                // Crater depression
                float craterDistance = new Vector2f(x, z).distance(volcano.craterCenter);
                if (craterDistance <= volcano.craterRadius) {
                    float craterDepth = (1.0f - craterDistance / volcano.craterRadius) * volcano.height * 0.3f;
                    height = Math.max(height, SEA_LEVEL + volcano.height * falloff - craterDepth);
                } else {
                    height = Math.max(height, SEA_LEVEL + volcano.height * falloff);
                }
            }
        }
        
        // Add noise for terrain variation
        float terrainNoise = mountainNoise.octaveNoise(x * MOUNTAIN_SCALE, z * MOUNTAIN_SCALE, 4, 0.5f);
        height += terrainNoise * 10; // ±10 blocks variation
        
        return height;
    }
    
    public BlockType getBlockAt(int x, int y, int z) {
        float terrainHeight = getHeightAt(x, z);
        
        // Air above terrain
        if (y > terrainHeight) {
            return y <= SEA_LEVEL ? BlockType.WATER : BlockType.AIR;
        }
        
        // Check for ore deposits
        Vector3f pos = new Vector3f(x, y, z);
        for (Map.Entry<Vector3f, OreDeposit> entry : oreDeposits.entrySet()) {
            if (pos.distance(entry.getKey()) <= 5.0f) { // 5-block radius
                OreDeposit deposit = entry.getValue();
                if (random.nextFloat() < deposit.richness) {
                    return deposit.oreType;
                }
            }
        }
        
        // Underground caves
        if (y < terrainHeight - 10) {
            float caveValue = caveNoise.octaveNoise(x * 0.02f, y * 0.02f, z * 0.02f, 3, 0.5f);
            if (caveValue > 0.6f) {
                return BlockType.AIR; // Cave space
            }
        }
        
        // Determine block type based on height and biome
        if (y > terrainHeight - 3) {
            // Surface layer
            if (terrainHeight > SEA_LEVEL + 5) {
                return BlockType.GRASS; // Land surface
            } else {
                return BlockType.SAND; // Beach/shallow water
            }
        } else if (y > terrainHeight - 10) {
            return BlockType.DIRT; // Subsurface
        } else {
            return BlockType.STONE; // Deep rock
        }
    }
    
    public float getTemperatureAt(float x, float z) {
        // Base temperature varies with latitude (distance from equator)
        float latitude = Math.abs(z) / 1000.0f; // Normalized latitude
        float baseTemp = 30.0f - latitude * 25.0f; // 30°C at equator, 5°C at poles
        
        // Add noise for local variation
        float tempNoise = temperatureNoise.octaveNoise(x * 0.001f, z * 0.001f, 3, 0.5f);
        
        return baseTemp + tempNoise * 10.0f;
    }
    
    public float getHumidityAt(float x, float z) {
        // Base humidity
        float baseHumidity = 60.0f;
        
        // Higher humidity near water
        float distanceToWater = 1000.0f; // Default large distance
        for (Island island : generatedIslands) {
            float distanceToIsland = new Vector2f(x, z).distance(island.center);
            if (distanceToIsland > island.radius) {
                distanceToWater = Math.min(distanceToWater, distanceToIsland - island.radius);
            }
        }
        
        float waterEffect = Math.max(0, 30.0f - distanceToWater * 0.1f);
        
        // Add noise
        float humidityNoise = humidityNoise.octaveNoise(x * 0.002f, z * 0.002f, 3, 0.5f);
        
        return Math.max(10, Math.min(100, baseHumidity + waterEffect + humidityNoise * 20));
    }
    
    public List<Island> getNearbyIslands(Vector2f position, float radius) {
        List<Island> nearby = new ArrayList<>();
        
        for (Island island : generatedIslands) {
            if (position.distance(island.center) <= radius + island.radius) {
                nearby.add(island);
            }
        }
        
        return nearby;
    }
    
    public List<TreasureLocation> getNearbyTreasures(Vector3f position, float radius) {
        List<TreasureLocation> nearby = new ArrayList<>();
        
        for (TreasureLocation treasure : treasureLocations) {
            if (position.distance(treasure.position) <= radius) {
                nearby.add(treasure);
            }
        }
        
        return nearby;
    }
    
    public void cleanup() {
        logger.info("Cleaning up world generator");
        generatedIslands.clear();
        volcanicIslands.clear();
        caveSystems.clear();
        geologicalFeatures.clear();
        oreDeposits.clear();
        treasureLocations.clear();
    }
    
    // Getters
    public long getSeed() { return seed; }
    public List<Island> getGeneratedIslands() { return new ArrayList<>(generatedIslands); }
    public List<VolcanicIsland> getVolcanicIslands() { return new ArrayList<>(volcanicIslands); }
    public Map<Vector2f, GeologicalFeature> getGeologicalFeatures() { return new HashMap<>(geologicalFeatures); }
    public List<TreasureLocation> getTreasureLocations() { return new ArrayList<>(treasureLocations); }
}