package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;

/**
 * Component that handles buoyancy physics for entities in water.
 * Applies upward force based on water displacement and fluid dynamics.
 */
public class BuoyancyComponent implements Component {
    
    /** Volume of the entity in cubic meters */
    private float volume;
    
    /** Density of the entity in kg/m³ */
    private float density;
    
    /** Drag coefficient for water resistance (0.0 - 1.0) */
    private float dragCoefficient;
    
    /** Current water level the entity is in */
    private float waterLevel;
    
    /** Percentage of entity submerged in water (0.0 - 1.0) */
    private float submersionLevel;
    
    /** Whether the entity is currently in water */
    private boolean inWater;
    
    /** Buoyancy force multiplier for fine-tuning */
    private float buoyancyMultiplier;
    
    /** Water density constant (kg/m³) */
    private static final float WATER_DENSITY = 1000.0f;
    
    /** Gravitational acceleration constant (m/s²) */
    private static final float GRAVITY = 9.81f;
    
    /**
     * Creates a new BuoyancyComponent with default values.
     * 
     * @param volume Volume of the entity in cubic meters
     * @param density Density of the entity in kg/m³
     */
    public BuoyancyComponent(float volume, float density) {
        this(volume, density, 0.5f, 1.0f);
    }
    
    /**
     * Creates a new BuoyancyComponent with specified parameters.
     * 
     * @param volume Volume of the entity in cubic meters
     * @param density Density of the entity in kg/m³
     * @param dragCoefficient Drag coefficient for water resistance
     * @param buoyancyMultiplier Multiplier for buoyancy force
     */
    public BuoyancyComponent(float volume, float density, float dragCoefficient, float buoyancyMultiplier) {
        this.volume = volume;
        this.density = density;
        this.dragCoefficient = Math.max(0.0f, Math.min(1.0f, dragCoefficient));
        this.buoyancyMultiplier = buoyancyMultiplier;
        this.waterLevel = 0.0f;
        this.submersionLevel = 0.0f;
        this.inWater = false;
    }
    
    /**
     * Calculates the buoyancy force based on Archimedes' principle.
     * F_buoyancy = ρ_water * V_displaced * g
     * 
     * @return Buoyancy force in Newtons (upward)
     */
    public float calculateBuoyancyForce() {
        if (!inWater || submersionLevel <= 0.0f) {
            return 0.0f;
        }
        
        float displacedVolume = volume * submersionLevel;
        float buoyancyForce = WATER_DENSITY * displacedVolume * GRAVITY * buoyancyMultiplier;
        
        return buoyancyForce;
    }
    
    /**
     * Calculates the net vertical force (buoyancy - weight).
     * Positive values indicate upward force, negative indicate sinking.
     * 
     * @return Net vertical force in Newtons
     */
    public float calculateNetVerticalForce() {
        float weight = density * volume * GRAVITY;
        float buoyancy = calculateBuoyancyForce();
        return buoyancy - weight;
    }
    
    /**
     * Calculates drag force opposing motion in water.
     * F_drag = 0.5 * ρ_water * v² * C_d * A
     * 
     * @param velocity Current velocity magnitude
     * @return Drag force magnitude
     */
    public float calculateDragForce(float velocity) {
        if (!inWater || velocity <= 0.0f) {
            return 0.0f;
        }
        
        // Approximate cross-sectional area from volume
        float crossSectionalArea = (float) Math.pow(volume, 2.0f / 3.0f);
        
        return 0.5f * WATER_DENSITY * velocity * velocity * dragCoefficient * crossSectionalArea;
    }
    
    /**
     * Updates the water interaction state.
     * 
     * @param waterLevel Current water level
     * @param entityY Entity's Y position
     * @param entityHeight Entity's height
     */
    public void updateWaterInteraction(float waterLevel, float entityY, float entityHeight) {
        this.waterLevel = waterLevel;
        
        float entityBottom = entityY;
        float entityTop = entityY + entityHeight;
        
        if (entityTop <= waterLevel) {
            // Fully submerged
            this.inWater = true;
            this.submersionLevel = 1.0f;
        } else if (entityBottom < waterLevel) {
            // Partially submerged
            this.inWater = true;
            this.submersionLevel = (waterLevel - entityBottom) / entityHeight;
        } else {
            // Not in water
            this.inWater = false;
            this.submersionLevel = 0.0f;
        }
    }
    
    /**
     * Checks if the entity will float based on its density.
     * 
     * @return true if the entity's density is less than water density
     */
    public boolean willFloat() {
        return density < WATER_DENSITY;
    }
    
    /**
     * Gets the stability factor for the entity in water.
     * Higher values indicate more stable floating.
     * 
     * @return Stability factor (0.0 - 1.0)
     */
    public float getStabilityFactor() {
        if (!inWater) {
            return 0.0f;
        }
        
        float densityRatio = density / WATER_DENSITY;
        return Math.max(0.0f, Math.min(1.0f, 1.0f - densityRatio));
    }
    
    // Getters and setters
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, volume);
    }
    
    public float getDensity() {
        return density;
    }
    
    public void setDensity(float density) {
        this.density = Math.max(0.0f, density);
    }
    
    public float getDragCoefficient() {
        return dragCoefficient;
    }
    
    public void setDragCoefficient(float dragCoefficient) {
        this.dragCoefficient = Math.max(0.0f, Math.min(1.0f, dragCoefficient));
    }
    
    public float getWaterLevel() {
        return waterLevel;
    }
    
    public float getSubmersionLevel() {
        return submersionLevel;
    }
    
    public boolean isInWater() {
        return inWater;
    }
    
    public float getBuoyancyMultiplier() {
        return buoyancyMultiplier;
    }
    
    public void setBuoyancyMultiplier(float buoyancyMultiplier) {
        this.buoyancyMultiplier = buoyancyMultiplier;
    }
    
    @Override
    public Component copy() {
        BuoyancyComponent copy = new BuoyancyComponent(volume, density, dragCoefficient, buoyancyMultiplier);
        copy.waterLevel = waterLevel;
        copy.submersionLevel = submersionLevel;
        copy.inWater = inWater;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Buoyancy{vol=%.2f, density=%.1f, submerged=%.1f%%, inWater=%s, force=%.1fN}", 
                           volume, density, submersionLevel * 100, inWater, calculateBuoyancyForce());
    }
}