package com.odyssey.world.weather;

import org.joml.Vector2f;

/**
 * Represents a weather pattern that influences wind and atmospheric conditions
 */
public class WeatherPattern {
    
    public enum PatternType {
        PERSISTENT,  // Always active
        SEASONAL,    // Varies with seasons
        CYCLICAL,    // Repeating cycles
        RANDOM       // Random variations
    }
    
    private final String name;
    private final Vector2f direction;
    private final float strength;
    private final PatternType type;
    private final float period;
    private final float phase;
    
    public WeatherPattern(String name, Vector2f direction, float strength, PatternType type) {
        this.name = name;
        this.direction = new Vector2f(direction);
        this.strength = strength;
        this.type = type;
        this.period = 24.0f; // Default 24 hour period
        this.phase = 0.0f;
    }
    
    public WeatherPattern(String name, Vector2f direction, float strength, PatternType type, float period, float phase) {
        this.name = name;
        this.direction = new Vector2f(direction);
        this.strength = strength;
        this.type = type;
        this.period = period;
        this.phase = phase;
    }
    
    /**
     * Calculate the influence of this pattern at a given time and seasonal factor
     */
    public float getInfluence(float globalTime, float seasonalFactor) {
        switch (type) {
            case PERSISTENT:
                return 1.0f;
                
            case SEASONAL:
                // Stronger during certain seasons
                return 0.5f + 0.5f * (float) Math.sin(seasonalFactor * Math.PI * 2);
                
            case CYCLICAL:
                // Repeating pattern based on time
                return 0.5f + 0.5f * (float) Math.sin((globalTime + phase) / period * Math.PI * 2);
                
            case RANDOM:
                // Random variations with some persistence
                return 0.3f + 0.7f * (float) Math.sin(globalTime * 0.1f + phase);
                
            default:
                return 1.0f;
        }
    }
    
    public String getName() {
        return name;
    }
    
    public Vector2f getDirection() {
        return new Vector2f(direction);
    }
    
    public float getStrength() {
        return strength;
    }
    
    public PatternType getType() {
        return type;
    }
    
    public float getPeriod() {
        return period;
    }
    
    public float getPhase() {
        return phase;
    }
}