package com.odyssey.world;

/**
 * Defines different biome types for the pirate world
 */
public enum BiomeType {
    // Ocean biomes
    OCEAN("Ocean", 0.5f, 0.7f, 0.0f, 0.0f),
    DEEP_OCEAN("Deep Ocean", 0.3f, 0.8f, 0.0f, 0.0f),
    SHALLOW_WATER("Shallow Water", 0.7f, 0.6f, 0.0f, 0.0f),
    
    // Coastal biomes
    BEACH("Beach", 0.8f, 0.4f, 0.1f, 0.2f),
    
    // Tropical biomes
    TROPICAL_FOREST("Tropical Forest", 0.9f, 0.9f, 0.8f, 0.7f),
    JUNGLE("Jungle", 0.95f, 0.95f, 0.9f, 0.8f),
    TROPICAL_GRASSLAND("Tropical Grassland", 0.85f, 0.7f, 0.4f, 0.6f),
    
    // Temperate biomes
    FOREST("Forest", 0.6f, 0.7f, 0.7f, 0.6f),
    GRASSLAND("Grassland", 0.7f, 0.5f, 0.3f, 0.5f),
    HILLS("Hills", 0.65f, 0.6f, 0.4f, 0.4f),
    
    // Arid biomes
    DESERT("Desert", 0.95f, 0.1f, 0.05f, 0.1f),
    SAVANNA("Savanna", 0.8f, 0.3f, 0.2f, 0.3f),
    
    // Mountain biomes
    MOUNTAIN("Mountain", 0.4f, 0.5f, 0.2f, 0.2f),
    
    // Volcanic biomes
    VOLCANIC_PEAK("Volcanic Peak", 0.7f, 0.3f, 0.1f, 0.05f),
    VOLCANIC_SLOPES("Volcanic Slopes", 0.75f, 0.4f, 0.3f, 0.2f),
    VOLCANIC_PLAINS("Volcanic Plains", 0.8f, 0.5f, 0.4f, 0.4f),
    
    // Swamp biomes
    SWAMP("Swamp", 0.75f, 0.95f, 0.6f, 0.5f),
    SWAMP_HILLS("Swamp Hills", 0.7f, 0.9f, 0.5f, 0.4f),
    
    // Legacy biomes (for compatibility with existing WorldGenerator)
    PLAINS("Plains", 0.7f, 0.4f, 0.3f, 0.4f),
    TUNDRA("Tundra", 0.2f, 0.3f, 0.1f, 0.1f);
    
    private final String displayName;
    private final float temperature;
    private final float humidity;
    private final float vegetationDensity;
    private final float resourceRichness;
    
    BiomeType(String displayName, float temperature, float humidity, 
              float vegetationDensity, float resourceRichness) {
        this.displayName = displayName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.vegetationDensity = vegetationDensity;
        this.resourceRichness = resourceRichness;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getVegetationDensity() { return vegetationDensity; }
    public float getResourceRichness() { return resourceRichness; }
    
    /**
     * Gets the tree density for this biome
     */
    public float getTreeDensity() {
        switch (this) {
            case TROPICAL_FOREST:
            case JUNGLE:
                return 0.8f;
            case FOREST:
                return 0.6f;
            case TROPICAL_GRASSLAND:
            case GRASSLAND:
                return 0.2f;
            case SAVANNA:
                return 0.1f;
            case SWAMP:
            case SWAMP_HILLS:
                return 0.4f;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Gets the coral spawn chance for this biome
     */
    public float getCoralSpawnChance() {
        switch (this) {
            case SHALLOW_WATER:
                return 0.3f;
            case OCEAN:
                return 0.1f;
            case BEACH:
                return 0.05f;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Checks if this biome is underwater
     */
    public boolean isUnderwater() {
        return this == OCEAN || this == DEEP_OCEAN || this == SHALLOW_WATER;
    }
    
    /**
     * Checks if this biome is coastal
     */
    public boolean isCoastal() {
        return this == BEACH || this == SHALLOW_WATER;
    }
    
    /**
     * Checks if this biome supports vegetation
     */
    public boolean supportsVegetation() {
        return vegetationDensity > 0.0f && !isUnderwater();
    }
    
    /**
     * Gets the navigation difficulty multiplier for this biome
     */
    public float getNavigationDifficulty() {
        switch (this) {
            case DEEP_OCEAN:
                return 0.8f;
            case OCEAN:
                return 1.0f;
            case SHALLOW_WATER:
                return 1.3f;
            case SWAMP:
            case SWAMP_HILLS:
                return 1.5f;
            default:
                return 1.0f; // Land biomes don't affect navigation directly
        }
    }
}