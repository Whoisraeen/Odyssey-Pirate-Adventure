package com.odyssey.world;

import com.odyssey.world.BiomeType;
import com.odyssey.world.feature.Feature;
import com.odyssey.world.structure.Structure;
import com.odyssey.world.VegetationType;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a procedurally generated island with terrain, biomes, features, and structures
 */
public class Island {
    
    // Core properties
    private final float centerX;
    private final float centerZ;
    private final IslandType type;
    
    // Generated data
    private float[][] heightmap;
    private BiomeType[][] biomeMap;
    private VegetationType[][] vegetationMap;
    private int size;
    
    // Features and structures
    private List<Feature> features;
    private List<Structure> structures;
    
    // Generation metadata
    private boolean fullyGenerated;
    private long generationTime;
    
    /**
     * Creates a new island at the specified world coordinates
     */
    public Island(float centerX, float centerZ, IslandType type) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.type = type;
        this.features = new ArrayList<>();
        this.structures = new ArrayList<>();
        this.fullyGenerated = false;
        this.generationTime = System.currentTimeMillis();
    }
    
    // Core getters
    public float getCenterX() { return centerX; }
    public float getCenterZ() { return centerZ; }
    public IslandType getType() { return type; }
    
    // Terrain data
    public float[][] getHeightmap() { return heightmap; }
    public void setHeightmap(float[][] heightmap) { this.heightmap = heightmap; }
    
    public BiomeType[][] getBiomeMap() { return biomeMap; }
    public void setBiomeMap(BiomeType[][] biomeMap) { this.biomeMap = biomeMap; }
    
    public VegetationType[][] getVegetationMap() { return vegetationMap; }
    public void setVegetationMap(VegetationType[][] vegetationMap) { this.vegetationMap = vegetationMap; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    // Features and structures
    public List<Feature> getFeatures() { return features; }
    public void setFeatures(List<Feature> features) { this.features = features; }
    public void addFeature(Feature feature) { this.features.add(feature); }
    
    public List<Structure> getStructures() { return structures; }
    public void setStructures(List<Structure> structures) { this.structures = structures; }
    public void addStructure(Structure structure) { this.structures.add(structure); }
    
    // Generation status
    public boolean isFullyGenerated() { return fullyGenerated; }
    public void setFullyGenerated(boolean fullyGenerated) { this.fullyGenerated = fullyGenerated; }
    public long getGenerationTime() { return generationTime; }
    
    /**
     * Gets the height at specific world coordinates
     */
    public float getHeightAt(float worldX, float worldZ) {
        if (heightmap == null) return -10.0f;
        
        float radius = type.getRadius();
        
        // Convert world coordinates to heightmap coordinates
        int x = (int) (worldX - centerX + radius);
        int z = (int) (worldZ - centerZ + radius);
        
        if (x >= 0 && x < size && z >= 0 && z < size) {
            return heightmap[x][z];
        }
        
        return -10.0f; // Default ocean depth
    }
    
    /**
     * Gets the biome at specific world coordinates
     */
    public BiomeType getBiomeAt(float worldX, float worldZ) {
        if (biomeMap == null) return BiomeType.OCEAN;
        
        float radius = type.getRadius();
        
        // Convert world coordinates to biome map coordinates
        int x = (int) (worldX - centerX + radius);
        int z = (int) (worldZ - centerZ + radius);
        
        if (x >= 0 && x < size && z >= 0 && z < size) {
            return biomeMap[x][z];
        }
        
        return BiomeType.OCEAN;
    }
    
    /**
     * Gets the vegetation at specific world coordinates
     */
    public VegetationType getVegetationAt(float worldX, float worldZ) {
        if (vegetationMap == null) return VegetationType.NONE;
        
        float radius = type.getRadius();
        
        // Convert world coordinates to vegetation map coordinates
        int x = (int) (worldX - centerX + radius);
        int z = (int) (worldZ - centerZ + radius);
        
        if (x >= 0 && x < size && z >= 0 && z < size) {
            return vegetationMap[x][z];
        }
        
        return VegetationType.NONE;
    }
    
    /**
     * Checks if a point is within the island's bounds
     */
    public boolean containsPoint(float worldX, float worldZ) {
        float distance = (float) Math.sqrt(
            (worldX - centerX) * (worldX - centerX) +
            (worldZ - centerZ) * (worldZ - centerZ)
        );
        
        return distance <= type.getRadius();
    }
    
    /**
     * Gets the distance from a point to the island center
     */
    public float getDistanceFromCenter(float worldX, float worldZ) {
        return (float) Math.sqrt(
            (worldX - centerX) * (worldX - centerX) +
            (worldZ - centerZ) * (worldZ - centerZ)
        );
    }
    
    /**
     * Gets all features of a specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends Feature> List<T> getFeaturesOfType(Class<T> featureType) {
        List<T> result = new ArrayList<>();
        for (Feature feature : features) {
            if (featureType.isInstance(feature)) {
                result.add((T) feature);
            }
        }
        return result;
    }
    
    /**
     * Gets all structures of a specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends Structure> List<T> getStructuresOfType(Class<T> structureType) {
        List<T> result = new ArrayList<>();
        for (Structure structure : structures) {
            if (structureType.isInstance(structure)) {
                result.add((T) structure);
            }
        }
        return result;
    }
    
    /**
     * Calculates the island's total area
     */
    public float getArea() {
        return (float) Math.PI * type.getRadius() * type.getRadius();
    }
    
    /**
     * Gets the island's bounding box
     */
    public IslandBounds getBounds() {
        float radius = type.getRadius();
        return new IslandBounds(
            centerX - radius, centerZ - radius,
            centerX + radius, centerZ + radius
        );
    }
    
    /**
     * Gets the island's display name
     */
    public String getName() {
        return type.getDisplayName() + " Island";
    }
    
    /**
     * Gets the goods produced by this island based on its type and climate
     */
    public List<String> getProducedGoods() {
        List<String> goods = new ArrayList<>();
        switch (type.getClimate()) {
            case TROPICAL:
                goods.add("sugar");
                goods.add("spices");
                goods.add("cotton");
                break;
            case TEMPERATE:
                goods.add("wood");
                goods.add("food");
                break;
            case VOLCANIC:
                goods.add("iron");
                goods.add("sulfur");
                break;
            case ARID:
                goods.add("salt");
                break;
            case SWAMP:
                goods.add("wood");
                break;
        }
        return goods;
    }
    
    /**
     * Gets the goods needed by this island
     */
    public List<String> getNeededGoods() {
        List<String> goods = new ArrayList<>();
        switch (type.getClimate()) {
            case VOLCANIC:
            case ARID:
                goods.add("water");
                goods.add("food");
                break;
            case SWAMP:
                goods.add("food");
                goods.add("weapons");
                break;
            default:
                goods.add("tools");
                goods.add("weapons");
                break;
        }
        return goods;
    }
    
    /**
     * Gets the production rate for a specific good
     */
    public float getProductionRate(String good) {
        if (getProducedGoods().contains(good)) {
            return 1.0f + type.getResourceRichness() * 0.5f;
        }
        return 0.0f;
    }
    
    /**
     * Gets the demand rate for a specific good
     */
    public float getDemandRate(String good) {
        if (getNeededGoods().contains(good)) {
            return 1.0f + (1.0f - type.getResourceRichness()) * 0.5f;
        }
        return 0.5f;
    }
    
    @Override
    public String toString() {
        return String.format("Island[%s at (%.1f, %.1f), radius=%.1f]", 
            type.name(), centerX, centerZ, type.getRadius());
    }
    
    /**
     * Represents the bounding box of an island
     */
    public static class IslandBounds {
        public final float minX, minZ, maxX, maxZ;
        
        public IslandBounds(float minX, float minZ, float maxX, float maxZ) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }
        
        public boolean contains(float x, float z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
        
        public boolean intersects(IslandBounds other) {
            return !(other.maxX < minX || other.minX > maxX || 
                     other.maxZ < minZ || other.minZ > maxZ);
        }
    }
}