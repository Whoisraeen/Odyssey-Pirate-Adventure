package com.odyssey.world.weather;

/**
 * Types of precipitation that can occur in the game world
 */
public enum PrecipitationType {
    
    NONE("None", 0.0f, 1.0f),
    DRIZZLE("Drizzle", 0.1f, 0.9f),
    RAIN("Rain", 0.3f, 0.7f),
    HEAVY_RAIN("Heavy Rain", 0.6f, 0.4f),
    THUNDERSTORM("Thunderstorm", 0.8f, 0.3f),
    HAIL("Hail", 0.5f, 0.5f),
    SNOW("Snow", 0.2f, 0.6f),
    SLEET("Sleet", 0.4f, 0.5f);
    
    private final String displayName;
    private final float intensityFactor;
    private final float visibilityFactor;
    
    PrecipitationType(String displayName, float intensityFactor, float visibilityFactor) {
        this.displayName = displayName;
        this.intensityFactor = intensityFactor;
        this.visibilityFactor = visibilityFactor;
    }
    
    /**
     * Gets the display name for this precipitation type
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the base intensity factor for this precipitation type
     */
    public float getIntensityFactor() {
        return intensityFactor;
    }
    
    /**
     * Gets the visibility reduction factor (1.0 = no reduction, 0.0 = complete reduction)
     */
    public float getVisibilityFactor() {
        return visibilityFactor;
    }
    
    /**
     * Checks if this precipitation type affects ship movement
     */
    public boolean affectsMovement() {
        return this != NONE && this != DRIZZLE;
    }
    
    /**
     * Checks if this precipitation type is dangerous for sailing
     */
    public boolean isDangerous() {
        return this == THUNDERSTORM || this == HAIL || this == HEAVY_RAIN;
    }
    
    /**
     * Gets the movement speed modifier for ships (1.0 = no change, 0.5 = half speed)
     */
    public float getMovementModifier() {
        switch (this) {
            case NONE:
            case DRIZZLE:
                return 1.0f;
            case RAIN:
                return 0.9f;
            case HEAVY_RAIN:
                return 0.7f;
            case THUNDERSTORM:
                return 0.5f;
            case HAIL:
                return 0.6f;
            case SNOW:
                return 0.8f;
            case SLEET:
                return 0.7f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the crew morale modifier (-1.0 to 1.0)
     */
    public float getMoraleModifier() {
        switch (this) {
            case NONE:
                return 0.1f;
            case DRIZZLE:
                return 0.0f;
            case RAIN:
                return -0.2f;
            case HEAVY_RAIN:
                return -0.4f;
            case THUNDERSTORM:
                return -0.6f;
            case HAIL:
                return -0.5f;
            case SNOW:
                return -0.1f;
            case SLEET:
                return -0.3f;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Gets the damage risk factor for ships (0.0 = no risk, 1.0 = high risk)
     */
    public float getDamageRisk() {
        switch (this) {
            case NONE:
            case DRIZZLE:
            case RAIN:
                return 0.0f;
            case HEAVY_RAIN:
                return 0.1f;
            case THUNDERSTORM:
                return 0.3f;
            case HAIL:
                return 0.4f;
            case SNOW:
                return 0.05f;
            case SLEET:
                return 0.1f;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Gets the sound effect name for this precipitation type
     */
    public String getSoundEffect() {
        switch (this) {
            case NONE:
                return null;
            case DRIZZLE:
                return "weather_drizzle";
            case RAIN:
                return "weather_rain";
            case HEAVY_RAIN:
                return "weather_heavy_rain";
            case THUNDERSTORM:
                return "weather_thunderstorm";
            case HAIL:
                return "weather_hail";
            case SNOW:
                return "weather_snow";
            case SLEET:
                return "weather_sleet";
            default:
                return null;
        }
    }
    
    /**
     * Gets the particle effect name for this precipitation type
     */
    public String getParticleEffect() {
        switch (this) {
            case NONE:
                return null;
            case DRIZZLE:
                return "particles_drizzle";
            case RAIN:
                return "particles_rain";
            case HEAVY_RAIN:
                return "particles_heavy_rain";
            case THUNDERSTORM:
                return "particles_thunderstorm";
            case HAIL:
                return "particles_hail";
            case SNOW:
                return "particles_snow";
            case SLEET:
                return "particles_sleet";
            default:
                return null;
        }
    }
}