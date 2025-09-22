package com.odyssey.world;

/**
 * Defines different types of islands with their characteristics
 */
public enum IslandType {
    // Volcanic islands
    VOLCANIC_SMALL(30f, 45f, IslandClimate.VOLCANIC, IslandShape.VOLCANIC, 1.2f),
    VOLCANIC_LARGE(80f, 120f, IslandClimate.VOLCANIC, IslandShape.VOLCANIC, 1.5f),
    
    // Mountainous islands
    MOUNTAINOUS_MEDIUM(50f, 80f, IslandClimate.TEMPERATE, IslandShape.MOUNTAINOUS, 1.3f),
    MOUNTAINOUS_LARGE(90f, 150f, IslandClimate.TEMPERATE, IslandShape.MOUNTAINOUS, 1.4f),
    
    // Tropical islands
    TROPICAL_SMALL(25f, 35f, IslandClimate.TROPICAL, IslandShape.FLAT, 0.8f),
    TROPICAL_MEDIUM(45f, 50f, IslandClimate.TROPICAL, IslandShape.FLAT, 0.9f),
    
    // Desert islands
    DESERT_MEDIUM(40f, 25f, IslandClimate.ARID, IslandShape.FLAT, 0.6f),
    
    // Rocky islands
    ROCKY_SMALL(20f, 30f, IslandClimate.TEMPERATE, IslandShape.MOUNTAINOUS, 1.1f),
    
    // Swamp islands
    SWAMP_MEDIUM(35f, 15f, IslandClimate.SWAMP, IslandShape.FLAT, 0.7f),
    
    // Coral atolls
    CORAL_ATOLL(60f, 8f, IslandClimate.TROPICAL, IslandShape.ATOLL, 0.3f);
    
    private final float radius;
    private final float maxHeight;
    private final IslandClimate climate;
    private final IslandShape shape;
    private final float terrainRoughness;
    
    IslandType(float radius, float maxHeight, IslandClimate climate, IslandShape shape, float terrainRoughness) {
        this.radius = radius;
        this.maxHeight = maxHeight;
        this.climate = climate;
        this.shape = shape;
        this.terrainRoughness = terrainRoughness;
    }
    
    // Getters
    public float getRadius() { return radius; }
    public float getMaxHeight() { return maxHeight; }
    public IslandClimate getClimate() { return climate; }
    public IslandShape getShape() { return shape; }
    public float getTerrainRoughness() { return terrainRoughness; }
    
    /**
     * Gets the display name for this island type
     */
    public String getDisplayName() {
        return name().replace("_", " ").toLowerCase();
    }
    
    /**
     * Gets the name for compatibility with IslandGenerator
     */
    public String getName() {
        return getDisplayName();
    }
    
    /**
     * Gets the rarity of this island type (0.0 = common, 1.0 = very rare)
     */
    public float getRarity() {
        switch (this) {
            case VOLCANIC_LARGE:
            case MOUNTAINOUS_LARGE:
                return 0.9f;
            case VOLCANIC_SMALL:
            case CORAL_ATOLL:
                return 0.7f;
            case MOUNTAINOUS_MEDIUM:
            case DESERT_MEDIUM:
                return 0.5f;
            case TROPICAL_MEDIUM:
            case SWAMP_MEDIUM:
                return 0.3f;
            case TROPICAL_SMALL:
            case ROCKY_SMALL:
                return 0.1f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the resource richness of this island type
     */
    public float getResourceRichness() {
        switch (this) {
            case VOLCANIC_LARGE:
            case VOLCANIC_SMALL:
                return 0.9f; // Rich in minerals
            case MOUNTAINOUS_LARGE:
            case MOUNTAINOUS_MEDIUM:
                return 0.8f; // Good ore deposits
            case TROPICAL_MEDIUM:
            case TROPICAL_SMALL:
                return 0.6f; // Moderate resources
            case DESERT_MEDIUM:
                return 0.4f; // Limited water, some minerals
            case SWAMP_MEDIUM:
                return 0.3f; // Poor resources
            case ROCKY_SMALL:
                return 0.2f; // Very limited
            case CORAL_ATOLL:
                return 0.1f; // Mostly coral and fish
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the navigation difficulty around this island type
     */
    public float getNavigationDifficulty() {
        switch (this) {
            case CORAL_ATOLL:
                return 0.9f; // Shallow reefs
            case VOLCANIC_LARGE:
            case VOLCANIC_SMALL:
                return 0.7f; // Rough seas, possible eruptions
            case MOUNTAINOUS_LARGE:
            case MOUNTAINOUS_MEDIUM:
                return 0.6f; // Rocky coastlines
            case ROCKY_SMALL:
                return 0.5f; // Rocky but small
            case SWAMP_MEDIUM:
                return 0.4f; // Muddy waters
            case DESERT_MEDIUM:
                return 0.3f; // Clear but hot
            case TROPICAL_MEDIUM:
            case TROPICAL_SMALL:
                return 0.2f; // Generally safe
            default:
                return 0.4f;
        }
    }
}