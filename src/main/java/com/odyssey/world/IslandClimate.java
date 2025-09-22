package com.odyssey.world;

/**
 * Defines different climate types for islands
 */
public enum IslandClimate {
    TROPICAL("Tropical", 25.0f, 35.0f, 0.8f, 0.9f),
    TEMPERATE("Temperate", 10.0f, 25.0f, 0.5f, 0.7f),
    ARID("Arid", 20.0f, 40.0f, 0.1f, 0.3f),
    VOLCANIC("Volcanic", 15.0f, 30.0f, 0.4f, 0.6f),
    SWAMP("Swamp", 18.0f, 28.0f, 0.9f, 1.0f);
    
    private final String displayName;
    private final float minTemperature;
    private final float maxTemperature;
    private final float minHumidity;
    private final float maxHumidity;
    
    IslandClimate(String displayName, float minTemperature, float maxTemperature, 
                  float minHumidity, float maxHumidity) {
        this.displayName = displayName;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.minHumidity = minHumidity;
        this.maxHumidity = maxHumidity;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getMinTemperature() { return minTemperature; }
    public float getMaxTemperature() { return maxTemperature; }
    public float getMinHumidity() { return minHumidity; }
    public float getMaxHumidity() { return maxHumidity; }
    
    /**
     * Gets the average temperature for this climate
     */
    public float getAverageTemperature() {
        return (minTemperature + maxTemperature) / 2.0f;
    }
    
    /**
     * Gets the average humidity for this climate
     */
    public float getAverageHumidity() {
        return (minHumidity + maxHumidity) / 2.0f;
    }
    
    /**
     * Determines if this climate supports lush vegetation
     */
    public boolean supportsLushVegetation() {
        return this == TROPICAL || (this == TEMPERATE && getAverageHumidity() > 0.6f) || this == SWAMP;
    }
    
    /**
     * Gets the rainfall factor for this climate
     */
    public float getRainfallFactor() {
        switch (this) {
            case TROPICAL:
            case SWAMP:
                return 1.0f;
            case TEMPERATE:
                return 0.7f;
            case VOLCANIC:
                return 0.5f;
            case ARID:
                return 0.2f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the wind intensity factor for this climate
     */
    public float getWindIntensity() {
        switch (this) {
            case VOLCANIC:
                return 0.8f; // Thermal updrafts
            case ARID:
                return 0.7f; // Hot air currents
            case TEMPERATE:
                return 0.5f; // Moderate winds
            case TROPICAL:
                return 0.4f; // Trade winds
            case SWAMP:
                return 0.2f; // Stagnant air
            default:
                return 0.5f;
        }
    }
}