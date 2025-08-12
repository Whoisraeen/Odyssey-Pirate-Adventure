package com.odyssey.world.treasures;

import com.odyssey.core.Engine;
import com.odyssey.world.World;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Generates treasure maps and manages treasure locations in the world.
 * Integrates with the world generation system to place treasures in suitable locations.
 */
public class TreasureGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TreasureGenerator.class);
    
    private final Random random;
    private final long worldSeed;
    
    // Treasure generation parameters
    private static final int MIN_TREASURE_DISTANCE = 500; // Minimum distance between treasures
    private static final int MAX_GENERATION_ATTEMPTS = 100; // Max attempts to find suitable location
    
    // Generated treasures
    private final List<TreasureMap> generatedMaps;
    private final List<Vector3f> treasureLocations;
    
    // Configuration
    private static final int MAX_TREASURES_PER_REGION = 3; // Maximum treasures per 1000x1000 region
    private static final float ISLAND_PREFERENCE = 0.7f; // Preference for placing treasures on islands
    
    public TreasureGenerator(long worldSeed) {
        this.worldSeed = worldSeed;
        this.random = new Random(worldSeed);
        this.generatedMaps = new ArrayList<>();
        this.treasureLocations = new ArrayList<>();
        
        logger.info("Initialized treasure generator with seed: {}", worldSeed);
    }
    
    /**
     * Generates a new treasure map for the specified region.
     * @param regionX The region X coordinate (in chunks)
     * @param regionZ The region Z coordinate (in chunks)
     * @return A new treasure map, or null if no suitable location found
     */
    public TreasureMap generateTreasureMap(int regionX, int regionZ) {
        // Calculate region bounds (each region is 1000x1000 blocks)
        int regionSize = 1000;
        int minX = regionX * regionSize;
        int maxX = minX + regionSize;
        int minZ = regionZ * regionSize;
        int maxZ = minZ + regionSize;
        
        // Check if this region already has enough treasures
        long treasuresInRegion = treasureLocations.stream()
            .filter(pos -> pos.x >= minX && pos.x < maxX && pos.z >= minZ && pos.z < maxZ)
            .count();
        
        if (treasuresInRegion >= MAX_TREASURES_PER_REGION) {
            return null;
        }
        
        // Find a suitable treasure location
        Vector3f treasureLocation = findTreasureLocation(minX, maxX, minZ, maxZ);
        if (treasureLocation == null) {
            return null;
        }
        
        // Determine treasure type and difficulty based on distance from spawn
        float distanceFromSpawn = treasureLocation.length();
        TreasureMap.TreasureType treasureType = determineTreasureType(distanceFromSpawn);
        int difficulty = determineDifficulty(distanceFromSpawn);
        
        // Generate unique map ID
        String mapId = UUID.randomUUID().toString();
        
        // Create the treasure map
        TreasureMap treasureMap = new TreasureMap(mapId, treasureLocation, treasureType, difficulty);
        
        // Register the treasure
        generatedMaps.add(treasureMap);
        treasureLocations.add(treasureLocation);
        
        logger.info("Generated treasure map {} at ({}, {}, {}) with type {} and difficulty {}", 
                   mapId, treasureLocation.x, treasureLocation.y, treasureLocation.z, 
                   treasureType, difficulty);
        
        return treasureMap;
    }
    
    /**
     * Finds a suitable location for treasure within the specified bounds.
     */
    private Vector3f findTreasureLocation(int minX, int maxX, int minZ, int maxZ) {
        int attempts = 0;
        int maxAttempts = 50;
        
        while (attempts < maxAttempts) {
            // Generate random coordinates within bounds
            float x = minX + random.nextFloat() * (maxX - minX);
            float z = minZ + random.nextFloat() * (maxZ - minZ);
            
            // Check if location is far enough from existing treasures
            Vector3f candidate = new Vector3f(x, 0, z);
            if (isTooCloseToExistingTreasure(candidate)) {
                attempts++;
                continue;
            }
            
            // Check if location is suitable (on land, not in deep ocean, etc.)
            if (isSuitableLocation(x, z)) {
                // Set appropriate Y coordinate (sea level or slightly above)
                candidate.y = getGroundLevel(x, z);
                return candidate;
            }
            
            attempts++;
        }
        
        return null; // No suitable location found
    }
    
    /**
     * Checks if a location is too close to existing treasures.
     */
    private boolean isTooCloseToExistingTreasure(Vector3f location) {
        for (Vector3f existing : treasureLocations) {
            if (location.distance(existing) < MIN_TREASURE_DISTANCE) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if a location is suitable for treasure placement.
     */
    private boolean isSuitableLocation(float x, float z) {
        // Integrate with actual world generation system
        World world = Engine.getInstance().getWorld();
        if (world == null) {
            // Fallback to simple noise-based terrain check if world not available
            double noise = Math.sin(x * 0.01) * Math.cos(z * 0.01) + 
                          Math.sin(x * 0.005) * Math.cos(z * 0.005) * 0.5;
            return noise > -0.3; // Threshold for "land"
        }
        
        // Get terrain height at this location
        float terrainHeight = world.getHeightAt(x, z);
        
        // Check if location is on land (above sea level)
        boolean isOnLand = terrainHeight > World.getSeaLevel();
        
        // Check if location is not too high (avoid mountain peaks)
        boolean isReasonableHeight = terrainHeight < World.getSeaLevel() + 50;
        
        // Check if location is accessible (not too steep)
        boolean isAccessible = isLocationAccessible(world, x, z, terrainHeight);
        
        return isOnLand && isReasonableHeight && isAccessible;
    }
    
    /**
     * Checks if a location is accessible by checking the terrain slope.
     */
    private boolean isLocationAccessible(World world, float x, float z, float centerHeight) {
        // Check terrain height in a small radius to determine slope
        float radius = 5.0f;
        float[] heights = {
            world.getHeightAt(x + radius, z),
            world.getHeightAt(x - radius, z),
            world.getHeightAt(x, z + radius),
            world.getHeightAt(x, z - radius)
        };
        
        // Calculate maximum height difference
        float maxDiff = 0;
        for (float height : heights) {
            maxDiff = Math.max(maxDiff, Math.abs(height - centerHeight));
        }
        
        // Location is accessible if slope is not too steep
        return maxDiff < 15.0f; // Maximum height difference of 15 blocks
    }
    
    /**
     * Gets the ground level at the specified coordinates.
     */
    private float getGroundLevel(float x, float z) {
        // Integrate with actual world generation to get real ground level
        World world = Engine.getInstance().getWorld();
        if (world == null) {
            // Fallback to sea level with some variation if world not available
            return 64 + (float)(Math.sin(x * 0.1) * Math.cos(z * 0.1) * 10);
        }
        
        // Get the actual terrain height from the world generator
        float terrainHeight = world.getHeightAt(x, z);
        
        // Add a small offset to place treasure slightly above ground
        return terrainHeight + 1.0f;
    }
    
    /**
     * Determines the treasure type based on distance from spawn.
     */
    private TreasureMap.TreasureType determineTreasureType(float distanceFromSpawn) {
        if (distanceFromSpawn < 1000) {
            // Close to spawn - common treasures
            return random.nextBoolean() ? 
                TreasureMap.TreasureType.GOLD_COINS : 
                TreasureMap.TreasureType.RARE_MATERIALS;
        } else if (distanceFromSpawn < 3000) {
            // Medium distance - better treasures
            TreasureMap.TreasureType[] mediumTreasures = {
                TreasureMap.TreasureType.PRECIOUS_GEMS,
                TreasureMap.TreasureType.SHIP_BLUEPRINTS,
                TreasureMap.TreasureType.GOLD_COINS
            };
            return mediumTreasures[random.nextInt(mediumTreasures.length)];
        } else {
            // Far from spawn - rare treasures
            TreasureMap.TreasureType[] rareTreasures = {
                TreasureMap.TreasureType.ANCIENT_ARTIFACTS,
                TreasureMap.TreasureType.LEGENDARY_WEAPON,
                TreasureMap.TreasureType.PRECIOUS_GEMS
            };
            return rareTreasures[random.nextInt(rareTreasures.length)];
        }
    }
    
    /**
     * Determines the difficulty level based on distance from spawn.
     */
    private int determineDifficulty(float distanceFromSpawn) {
        if (distanceFromSpawn < 500) {
            return 1; // Easy
        } else if (distanceFromSpawn < 1500) {
            return 2; // Medium
        } else if (distanceFromSpawn < 3000) {
            return 3; // Hard
        } else {
            return 4; // Very Hard
        }
    }
    
    /**
     * Generates a random treasure map without specific region constraints.
     */
    public TreasureMap generateRandomTreasureMap() {
        // Generate in a random region within reasonable bounds
        int regionX = random.nextInt(20) - 10; // -10 to 10
        int regionZ = random.nextInt(20) - 10; // -10 to 10
        
        return generateTreasureMap(regionX, regionZ);
    }
    
    /**
     * Gets all generated treasure maps.
     */
    public List<TreasureMap> getGeneratedMaps() {
        return new ArrayList<>(generatedMaps);
    }
    
    /**
     * Gets all treasure locations.
     */
    public List<Vector3f> getTreasureLocations() {
        return new ArrayList<>(treasureLocations);
    }
    
    /**
     * Finds the nearest treasure to the specified location.
     */
    public TreasureMap findNearestTreasure(Vector3f location) {
        TreasureMap nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (TreasureMap map : generatedMaps) {
            float distance = location.distance(map.getTreasureLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = map;
            }
        }
        
        return nearest;
    }
    
    /**
     * Checks if there's a treasure at the specified location within the given radius.
     */
    public TreasureMap getTreasureAt(Vector3f location, float radius) {
        for (TreasureMap map : generatedMaps) {
            if (location.distance(map.getTreasureLocation()) <= radius) {
                return map;
            }
        }
        return null;
    }
    
    /**
     * Removes a treasure map (when treasure is found).
     */
    public void removeTreasureMap(TreasureMap map) {
        generatedMaps.remove(map);
        treasureLocations.remove(map.getTreasureLocation());
        logger.info("Removed treasure map {} after discovery", map.getMapId());
    }
    
    /**
     * Gets statistics about generated treasures.
     */
    public TreasureStats getStatistics() {
        return new TreasureStats(generatedMaps);
    }
    
    /**
     * Statistics about generated treasures.
     */
    public static class TreasureStats {
        private final int totalTreasures;
        private final int discoveredTreasures;
        private final Map<TreasureMap.TreasureType, Integer> treasureTypeCounts;
        
        public TreasureStats(List<TreasureMap> maps) {
            this.totalTreasures = maps.size();
            this.discoveredTreasures = (int) maps.stream().filter(TreasureMap::isDiscovered).count();
            this.treasureTypeCounts = new java.util.HashMap<>();
            
            for (TreasureMap map : maps) {
                treasureTypeCounts.merge(map.getTreasureType(), 1, Integer::sum);
            }
        }
        
        public int getTotalTreasures() { return totalTreasures; }
        public int getDiscoveredTreasures() { return discoveredTreasures; }
        public int getUndiscoveredTreasures() { return totalTreasures - discoveredTreasures; }
        public Map<TreasureMap.TreasureType, Integer> getTreasureTypeCounts() { return treasureTypeCounts; }
    }
}