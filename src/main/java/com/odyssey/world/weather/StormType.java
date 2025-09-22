package com.odyssey.world.weather;

/**
 * Different types of storms that can occur in the pirate world
 */
public enum StormType {
    
    THUNDERSTORM(
        "Thunderstorm",
        500.0f,     // Base radius
        300000L,    // Base duration (5 minutes)
        25.0f,      // Max wind speed
        980.0f,     // Min pressure
        0.8f,       // Precipitation intensity
        5.0f,       // Lightning frequency (per minute)
        2.0f,       // Movement speed
        200.0f,     // Min visibility
        PrecipitationType.THUNDERSTORM
    ),
    
    HURRICANE(
        "Hurricane",
        1500.0f,    // Base radius
        1800000L,   // Base duration (30 minutes)
        40.0f,      // Max wind speed
        950.0f,     // Min pressure
        0.9f,       // Precipitation intensity
        0.0f,       // Lightning frequency
        1.0f,       // Movement speed
        100.0f,     // Min visibility
        PrecipitationType.HEAVY_RAIN
    ),
    
    SQUALL(
        "Squall",
        300.0f,     // Base radius
        120000L,    // Base duration (2 minutes)
        20.0f,      // Max wind speed
        990.0f,     // Min pressure
        0.6f,       // Precipitation intensity
        1.0f,       // Lightning frequency
        4.0f,       // Movement speed
        300.0f,     // Min visibility
        PrecipitationType.RAIN
    ),
    
    TROPICAL_STORM(
        "Tropical Storm",
        800.0f,     // Base radius
        900000L,    // Base duration (15 minutes)
        30.0f,      // Max wind speed
        970.0f,     // Min pressure
        0.7f,       // Precipitation intensity
        2.0f,       // Lightning frequency
        1.5f,       // Movement speed
        150.0f,     // Min visibility
        PrecipitationType.HEAVY_RAIN
    ),
    
    HAILSTORM(
        "Hailstorm",
        400.0f,     // Base radius
        180000L,    // Base duration (3 minutes)
        15.0f,      // Max wind speed
        985.0f,     // Min pressure
        0.5f,       // Precipitation intensity
        3.0f,       // Lightning frequency
        3.0f,       // Movement speed
        250.0f,     // Min visibility
        PrecipitationType.HAIL
    ),
    
    WATERSPOUT(
        "Waterspout",
        150.0f,     // Base radius
        60000L,     // Base duration (1 minute)
        35.0f,      // Max wind speed
        975.0f,     // Min pressure
        0.3f,       // Precipitation intensity
        0.0f,       // Lightning frequency
        2.5f,       // Movement speed
        100.0f,     // Min visibility
        PrecipitationType.RAIN
    );
    
    private final String displayName;
    private final float baseRadius;
    private final long baseDuration;
    private final float maxWindSpeed;
    private final float minPressure;
    private final float precipitationIntensity;
    private final float lightningFrequency;
    private final float movementSpeed;
    private final float minVisibility;
    private final PrecipitationType precipitationType;
    
    StormType(String displayName, float baseRadius, long baseDuration, 
              float maxWindSpeed, float minPressure, float precipitationIntensity,
              float lightningFrequency, float movementSpeed, float minVisibility,
              PrecipitationType precipitationType) {
        this.displayName = displayName;
        this.baseRadius = baseRadius;
        this.baseDuration = baseDuration;
        this.maxWindSpeed = maxWindSpeed;
        this.minPressure = minPressure;
        this.precipitationIntensity = precipitationIntensity;
        this.lightningFrequency = lightningFrequency;
        this.movementSpeed = movementSpeed;
        this.minVisibility = minVisibility;
        this.precipitationType = precipitationType;
    }
    
    /**
     * Gets the rarity of this storm type (0.0 = very common, 1.0 = very rare)
     */
    public float getRarity() {
        switch (this) {
            case SQUALL:
                return 0.1f;
            case THUNDERSTORM:
                return 0.3f;
            case HAILSTORM:
                return 0.5f;
            case TROPICAL_STORM:
                return 0.7f;
            case WATERSPOUT:
                return 0.8f;
            case HURRICANE:
                return 0.9f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the danger level of this storm type (0.0 = safe, 1.0 = extremely dangerous)
     */
    public float getDangerLevel() {
        switch (this) {
            case SQUALL:
                return 0.3f;
            case THUNDERSTORM:
                return 0.5f;
            case HAILSTORM:
                return 0.6f;
            case TROPICAL_STORM:
                return 0.7f;
            case WATERSPOUT:
                return 0.8f;
            case HURRICANE:
                return 1.0f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the ship damage potential (0.0 = no damage, 1.0 = severe damage)
     */
    public float getDamagePotential() {
        switch (this) {
            case SQUALL:
                return 0.1f;
            case THUNDERSTORM:
                return 0.3f;
            case HAILSTORM:
                return 0.5f;
            case TROPICAL_STORM:
                return 0.4f;
            case WATERSPOUT:
                return 0.7f;
            case HURRICANE:
                return 0.9f;
            default:
                return 0.3f;
        }
    }
    
    /**
     * Checks if this storm type can occur in the given season
     */
    public boolean canOccurInSeason(String season) {
        switch (this) {
            case HURRICANE:
            case TROPICAL_STORM:
                return season.equals("summer") || season.equals("autumn");
            case HAILSTORM:
                return season.equals("spring") || season.equals("summer");
            case WATERSPOUT:
                return season.equals("summer") || season.equals("autumn");
            case THUNDERSTORM:
            case SQUALL:
                return true; // Can occur any season
            default:
                return true;
        }
    }
    
    /**
     * Gets the preferred ocean temperature for this storm type
     */
    public float getPreferredTemperature() {
        switch (this) {
            case HURRICANE:
            case TROPICAL_STORM:
                return 26.0f; // Warm tropical waters
            case WATERSPOUT:
                return 24.0f; // Warm waters
            case THUNDERSTORM:
                return 20.0f; // Moderate temperatures
            case HAILSTORM:
                return 15.0f; // Cooler temperatures
            case SQUALL:
                return 18.0f; // Mild temperatures
            default:
                return 20.0f;
        }
    }
    
    /**
     * Gets the formation probability based on conditions (0.0 to 1.0)
     */
    public float getFormationProbability(float temperature, float humidity, float pressure) {
        float probability = 0.0f;
        
        // Temperature factor
        float tempDiff = Math.abs(temperature - getPreferredTemperature());
        float tempFactor = Math.max(0.0f, 1.0f - tempDiff / 10.0f);
        probability += tempFactor * 0.4f;
        
        // Humidity factor (storms need high humidity)
        probability += humidity * 0.3f;
        
        // Pressure factor (low pressure favors storm formation)
        float pressureFactor = Math.max(0.0f, (1013.0f - pressure) / 50.0f);
        probability += Math.min(1.0f, pressureFactor) * 0.3f;
        
        // Apply rarity modifier
        probability *= (1.0f - getRarity());
        
        return Math.max(0.0f, Math.min(1.0f, probability));
    }
    
    /**
     * Gets the warning message for this storm type
     */
    public String getWarningMessage() {
        switch (this) {
            case HURRICANE:
                return "HURRICANE WARNING: Seek immediate shelter! Extreme winds and flooding expected.";
            case TROPICAL_STORM:
                return "TROPICAL STORM WARNING: Strong winds and heavy rain approaching.";
            case THUNDERSTORM:
                return "THUNDERSTORM WARNING: Lightning and strong winds detected.";
            case WATERSPOUT:
                return "WATERSPOUT WARNING: Dangerous rotating water column spotted!";
            case HAILSTORM:
                return "HAILSTORM WARNING: Large hail and damaging winds incoming.";
            case SQUALL:
                return "SQUALL WARNING: Brief but intense winds and rain approaching.";
            default:
                return "STORM WARNING: Severe weather conditions detected.";
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getBaseRadius() { return baseRadius; }
    public long getBaseDuration() { return baseDuration; }
    public float getMaxWindSpeed() { return maxWindSpeed; }
    public float getMinPressure() { return minPressure; }
    public float getPrecipitationIntensity() { return precipitationIntensity; }
    public float getLightningFrequency() { return lightningFrequency; }
    public float getMovementSpeed() { return movementSpeed; }
    public float getMinVisibility() { return minVisibility; }
    public PrecipitationType getPrecipitationType() { return precipitationType; }
}