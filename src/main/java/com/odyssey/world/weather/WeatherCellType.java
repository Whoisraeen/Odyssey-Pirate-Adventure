package com.odyssey.world.weather;

/**
 * Different types of weather cells that can form in the world
 */
public enum WeatherCellType {
    
    HIGH_PRESSURE(
        "High Pressure System",
        800.0f,     // Base radius
        1800000L,   // Base lifespan (30 minutes)
        0.5f,       // Movement speed
        10.0f,      // Rotation speed (degrees/sec)
        5.0f,       // Growth rate
        0.3f        // Intensity change rate
    ),
    
    LOW_PRESSURE(
        "Low Pressure System",
        600.0f,     // Base radius
        1200000L,   // Base lifespan (20 minutes)
        1.2f,       // Movement speed
        -15.0f,     // Rotation speed (counterclockwise)
        8.0f,       // Growth rate
        0.5f        // Intensity change rate
    ),
    
    WARM_FRONT(
        "Warm Front",
        1200.0f,    // Base radius
        900000L,    // Base lifespan (15 minutes)
        2.0f,       // Movement speed
        5.0f,       // Rotation speed
        3.0f,       // Growth rate
        0.4f        // Intensity change rate
    ),
    
    COLD_FRONT(
        "Cold Front",
        800.0f,     // Base radius
        600000L,    // Base lifespan (10 minutes)
        3.0f,       // Movement speed
        8.0f,       // Rotation speed
        6.0f,       // Growth rate
        0.6f        // Intensity change rate
    ),
    
    THERMAL_LOW(
        "Thermal Low",
        400.0f,     // Base radius
        480000L,    // Base lifespan (8 minutes)
        0.8f,       // Movement speed
        12.0f,      // Rotation speed
        4.0f,       // Growth rate
        0.7f        // Intensity change rate
    ),
    
    CONVERGENCE_ZONE(
        "Convergence Zone",
        300.0f,     // Base radius
        300000L,    // Base lifespan (5 minutes)
        1.5f,       // Movement speed
        20.0f,      // Rotation speed
        10.0f,      // Growth rate
        0.8f        // Intensity change rate
    );
    
    private final String displayName;
    private final float baseRadius;
    private final long baseLifespan;
    private final float movementSpeed;
    private final float rotationSpeed;
    private final float growthRate;
    private final float intensityChangeRate;
    
    WeatherCellType(String displayName, float baseRadius, long baseLifespan,
                    float movementSpeed, float rotationSpeed, float growthRate,
                    float intensityChangeRate) {
        this.displayName = displayName;
        this.baseRadius = baseRadius;
        this.baseLifespan = baseLifespan;
        this.movementSpeed = movementSpeed;
        this.rotationSpeed = rotationSpeed;
        this.growthRate = growthRate;
        this.intensityChangeRate = intensityChangeRate;
    }
    
    /**
     * Gets the formation probability for this cell type based on conditions
     */
    public float getFormationProbability(float temperature, float humidity, float pressure) {
        float probability = 0.0f;
        
        switch (this) {
            case HIGH_PRESSURE:
                // Forms in stable, dry conditions
                probability += (pressure > 1015.0f) ? 0.4f : 0.0f;
                probability += (humidity < 0.5f) ? 0.3f : 0.0f;
                probability += (temperature > 15.0f && temperature < 25.0f) ? 0.3f : 0.1f;
                break;
                
            case LOW_PRESSURE:
                // Forms in unstable, moist conditions
                probability += (pressure < 1010.0f) ? 0.4f : 0.0f;
                probability += (humidity > 0.6f) ? 0.3f : 0.0f;
                probability += (temperature > 20.0f) ? 0.3f : 0.1f;
                break;
                
            case WARM_FRONT:
                // Forms with moderate temperatures and humidity
                probability += (temperature > 18.0f) ? 0.4f : 0.1f;
                probability += (humidity > 0.5f && humidity < 0.8f) ? 0.3f : 0.1f;
                probability += (pressure > 1005.0f && pressure < 1020.0f) ? 0.3f : 0.1f;
                break;
                
            case COLD_FRONT:
                // Forms with temperature contrasts
                probability += (temperature < 18.0f) ? 0.4f : 0.1f;
                probability += (humidity > 0.4f) ? 0.3f : 0.0f;
                probability += (pressure < 1015.0f) ? 0.3f : 0.1f;
                break;
                
            case THERMAL_LOW:
                // Forms in hot, dry conditions
                probability += (temperature > 25.0f) ? 0.5f : 0.0f;
                probability += (humidity < 0.4f) ? 0.3f : 0.0f;
                probability += (pressure < 1010.0f) ? 0.2f : 0.0f;
                break;
                
            case CONVERGENCE_ZONE:
                // Forms where air masses meet
                probability += (humidity > 0.7f) ? 0.4f : 0.0f;
                probability += (temperature > 22.0f) ? 0.3f : 0.1f;
                probability += (pressure < 1012.0f) ? 0.3f : 0.1f;
                break;
        }
        
        return Math.max(0.0f, Math.min(1.0f, probability));
    }
    
    /**
     * Gets the stability of this cell type (0.0 = very unstable, 1.0 = very stable)
     */
    public float getStability() {
        switch (this) {
            case HIGH_PRESSURE:
                return 0.9f;
            case WARM_FRONT:
                return 0.7f;
            case LOW_PRESSURE:
                return 0.4f;
            case COLD_FRONT:
                return 0.3f;
            case THERMAL_LOW:
                return 0.2f;
            case CONVERGENCE_ZONE:
                return 0.1f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the weather severity this cell type can produce (0.0 = mild, 1.0 = severe)
     */
    public float getWeatherSeverity() {
        switch (this) {
            case HIGH_PRESSURE:
                return 0.1f;
            case WARM_FRONT:
                return 0.3f;
            case THERMAL_LOW:
                return 0.4f;
            case LOW_PRESSURE:
                return 0.6f;
            case COLD_FRONT:
                return 0.7f;
            case CONVERGENCE_ZONE:
                return 0.9f;
            default:
                return 0.5f;
        }
    }
    
    /**
     * Gets the interaction strength with other cell types
     */
    public float getInteractionStrength(WeatherCellType other) {
        // Opposite pressure systems interact strongly
        if ((this == HIGH_PRESSURE && other == LOW_PRESSURE) ||
            (this == LOW_PRESSURE && other == HIGH_PRESSURE)) {
            return 0.9f;
        }
        
        // Fronts interact with pressure systems
        if ((this == WARM_FRONT || this == COLD_FRONT) &&
            (other == HIGH_PRESSURE || other == LOW_PRESSURE)) {
            return 0.7f;
        }
        
        // Warm and cold fronts interact strongly
        if ((this == WARM_FRONT && other == COLD_FRONT) ||
            (this == COLD_FRONT && other == WARM_FRONT)) {
            return 0.8f;
        }
        
        // Convergence zones interact with everything
        if (this == CONVERGENCE_ZONE || other == CONVERGENCE_ZONE) {
            return 0.6f;
        }
        
        // Similar types have weak interaction
        if (this == other) {
            return 0.2f;
        }
        
        // Default moderate interaction
        return 0.4f;
    }
    
    /**
     * Checks if this cell type can spawn storms
     */
    public boolean canSpawnStorms() {
        switch (this) {
            case LOW_PRESSURE:
            case COLD_FRONT:
            case CONVERGENCE_ZONE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the preferred storm type this cell can spawn
     */
    public StormType getPreferredStormType() {
        switch (this) {
            case LOW_PRESSURE:
                return StormType.TROPICAL_STORM;
            case COLD_FRONT:
                return StormType.SQUALL;
            case CONVERGENCE_ZONE:
                return StormType.THUNDERSTORM;
            default:
                return StormType.THUNDERSTORM;
        }
    }
    
    /**
     * Gets the storm spawn probability (0.0 to 1.0)
     */
    public float getStormSpawnProbability(float intensity) {
        if (!canSpawnStorms()) return 0.0f;
        
        float baseProbability = 0.0f;
        switch (this) {
            case LOW_PRESSURE:
                baseProbability = 0.3f;
                break;
            case COLD_FRONT:
                baseProbability = 0.2f;
                break;
            case CONVERGENCE_ZONE:
                baseProbability = 0.4f;
                break;
        }
        
        return baseProbability * intensity;
    }
    
    /**
     * Gets the color representation for visualization
     */
    public int getColor() {
        switch (this) {
            case HIGH_PRESSURE:
                return 0xFF4169E1; // Royal Blue
            case LOW_PRESSURE:
                return 0xFFDC143C; // Crimson
            case WARM_FRONT:
                return 0xFFFF6347; // Tomato
            case COLD_FRONT:
                return 0xFF4682B4; // Steel Blue
            case THERMAL_LOW:
                return 0xFFFF4500; // Orange Red
            case CONVERGENCE_ZONE:
                return 0xFF9932CC; // Dark Orchid
            default:
                return 0xFF808080; // Gray
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getBaseRadius() { return baseRadius; }
    public long getBaseLifespan() { return baseLifespan; }
    public float getMovementSpeed() { return movementSpeed; }
    public float getRotationSpeed() { return rotationSpeed; }
    public float getGrowthRate() { return growthRate; }
    public float getIntensityChangeRate() { return intensityChangeRate; }
}