package com.odyssey.world;

/**
 * Defines different size categories for islands
 */
public enum IslandSize {
    SMALL("Small", 15f, 40f, 0.8f),
    MEDIUM("Medium", 35f, 70f, 1.0f),
    LARGE("Large", 60f, 120f, 1.2f);
    
    private final String displayName;
    private final float minRadius;
    private final float maxRadius;
    private final float complexityMultiplier;
    
    IslandSize(String displayName, float minRadius, float maxRadius, float complexityMultiplier) {
        this.displayName = displayName;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.complexityMultiplier = complexityMultiplier;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getMinRadius() { return minRadius; }
    public float getMaxRadius() { return maxRadius; }
    public float getComplexityMultiplier() { return complexityMultiplier; }
    
    /**
     * Gets the average radius for this size category
     */
    public float getAverageRadius() {
        return (minRadius + maxRadius) / 2.0f;
    }
    
    /**
     * Gets the expected number of features for this size
     */
    public int getExpectedFeatureCount() {
        switch (this) {
            case SMALL:
                return 2;
            case MEDIUM:
                return 5;
            case LARGE:
                return 10;
            default:
                return 3;
        }
    }
    
    /**
     * Gets the expected number of structures for this size
     */
    public int getExpectedStructureCount() {
        switch (this) {
            case SMALL:
                return 1;
            case MEDIUM:
                return 3;
            case LARGE:
                return 6;
            default:
                return 2;
        }
    }
    
    /**
     * Gets the generation time multiplier for this size
     */
    public float getGenerationTimeMultiplier() {
        switch (this) {
            case SMALL:
                return 0.5f;
            case MEDIUM:
                return 1.0f;
            case LARGE:
                return 2.0f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Determines if this size supports multiple biomes
     */
    public boolean supportsMultipleBiomes() {
        return this != SMALL;
    }
    
    /**
     * Gets the minimum distance between features for this size
     */
    public float getMinFeatureDistance() {
        switch (this) {
            case SMALL:
                return 5.0f;
            case MEDIUM:
                return 10.0f;
            case LARGE:
                return 15.0f;
            default:
                return 8.0f;
        }
    }
}