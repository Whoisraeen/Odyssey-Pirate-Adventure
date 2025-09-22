package com.odyssey.world;

/**
 * Defines different types of vegetation for the pirate world
 */
public enum VegetationType {
    // No vegetation
    NONE("None", 0.0f, 0.0f, 0.0f),
    
    // Forest vegetation
    DENSE_FOREST("Dense Forest", 0.9f, 0.8f, 0.7f),
    TROPICAL_TREES("Tropical Trees", 0.8f, 0.7f, 0.6f),
    TEMPERATE_FOREST("Temperate Forest", 0.7f, 0.6f, 0.5f),
    SCATTERED_TREES("Scattered Trees", 0.5f, 0.4f, 0.3f),
    SWAMP_TREES("Swamp Trees", 0.6f, 0.5f, 0.4f),
    PALM_TREES("Palm Trees", 0.4f, 0.3f, 0.5f),
    
    // Grass vegetation
    TALL_GRASS("Tall Grass", 0.3f, 0.2f, 0.1f),
    SHORT_GRASS("Short Grass", 0.2f, 0.1f, 0.05f),
    BEACH_GRASS("Beach Grass", 0.1f, 0.05f, 0.02f),
    
    // Arid vegetation
    CACTI("Cacti", 0.2f, 0.1f, 0.3f),
    SPARSE_VEGETATION("Sparse Vegetation", 0.1f, 0.05f, 0.02f),
    
    // Mountain vegetation
    MOUNTAIN_SHRUBS("Mountain Shrubs", 0.3f, 0.2f, 0.1f);
    
    private final String displayName;
    private final float density;
    private final float height;
    private final float resourceValue;
    
    VegetationType(String displayName, float density, float height, float resourceValue) {
        this.displayName = displayName;
        this.density = density;
        this.height = height;
        this.resourceValue = resourceValue;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getDensity() { return density; }
    public float getHeight() { return height; }
    public float getResourceValue() { return resourceValue; }
    
    /**
     * Checks if this vegetation type provides wood resources
     */
    public boolean providesWood() {
        switch (this) {
            case DENSE_FOREST:
            case TROPICAL_TREES:
            case TEMPERATE_FOREST:
            case SCATTERED_TREES:
            case SWAMP_TREES:
            case PALM_TREES:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this vegetation type provides food resources
     */
    public boolean providesFood() {
        switch (this) {
            case TROPICAL_TREES:
            case PALM_TREES:
            case CACTI:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the movement speed modifier for this vegetation type
     */
    public float getMovementSpeedModifier() {
        switch (this) {
            case DENSE_FOREST:
                return 0.5f;
            case TROPICAL_TREES:
            case TEMPERATE_FOREST:
                return 0.7f;
            case SWAMP_TREES:
                return 0.4f;
            case SCATTERED_TREES:
                return 0.8f;
            case TALL_GRASS:
                return 0.9f;
            case SHORT_GRASS:
            case BEACH_GRASS:
                return 1.0f;
            case CACTI:
            case SPARSE_VEGETATION:
                return 0.9f;
            case MOUNTAIN_SHRUBS:
                return 0.8f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the visibility modifier for this vegetation type
     */
    public float getVisibilityModifier() {
        switch (this) {
            case DENSE_FOREST:
                return 0.3f;
            case TROPICAL_TREES:
            case TEMPERATE_FOREST:
                return 0.5f;
            case SWAMP_TREES:
                return 0.4f;
            case SCATTERED_TREES:
                return 0.7f;
            case TALL_GRASS:
                return 0.8f;
            case SHORT_GRASS:
            case BEACH_GRASS:
            case SPARSE_VEGETATION:
                return 0.9f;
            case CACTI:
            case MOUNTAIN_SHRUBS:
                return 0.8f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the fire risk for this vegetation type
     */
    public float getFireRisk() {
        switch (this) {
            case DENSE_FOREST:
            case TEMPERATE_FOREST:
                return 0.8f;
            case TROPICAL_TREES:
                return 0.6f;
            case SCATTERED_TREES:
                return 0.5f;
            case TALL_GRASS:
            case SHORT_GRASS:
                return 0.7f;
            case BEACH_GRASS:
            case SPARSE_VEGETATION:
                return 0.4f;
            case SWAMP_TREES:
                return 0.2f;
            case CACTI:
                return 0.3f;
            case MOUNTAIN_SHRUBS:
                return 0.6f;
            default:
                return 0.0f;
        }
    }
}