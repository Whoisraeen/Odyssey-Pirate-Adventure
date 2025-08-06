package com.odyssey.world.physics;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a block with physics properties and simulation state.
 * 
 * <p>Physics blocks extend the basic block concept with additional properties
 * needed for advanced physics simulations including:
 * <ul>
 *   <li>Structural integrity and support relationships</li>
 *   <li>Fluid flow and pressure dynamics</li>
 *   <li>Thermal properties for fire and heat transfer</li>
 *   <li>Material properties for cloth and rope physics</li>
 *   <li>Damage and wear states</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PhysicsBlock {
    private static final Logger logger = LoggerFactory.getLogger(PhysicsBlock.class);
    
    /** Default structural integrity value. */
    private static final float DEFAULT_INTEGRITY = 1.0f;
    
    /** Default temperature in Celsius. */
    private static final float DEFAULT_TEMPERATURE = 20.0f;
    
    /** Maximum age before block becomes inactive (milliseconds). */
    private static final long MAX_INACTIVE_AGE = 60000; // 1 minute
    
    private final Vector3f position;
    private final BlockType blockType;
    private final Map<String, Object> properties;
    
    // Physics state
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong creationTime = new AtomicLong(System.currentTimeMillis());
    
    // Structural properties
    private volatile float structuralIntegrity = DEFAULT_INTEGRITY;
    private volatile float supportStrength = 1.0f;
    private volatile boolean isSupporting = false;
    private volatile boolean needsSupport = true;
    
    // Fluid properties
    private volatile float fluidLevel = 0.0f;
    private volatile float fluidPressure = 0.0f;
    private volatile BlockType fluidType = BlockType.AIR;
    private volatile Vector3f fluidVelocity = new Vector3f();
    
    // Thermal properties
    private volatile float temperature = DEFAULT_TEMPERATURE;
    private volatile float heatCapacity = 1.0f;
    private volatile float thermalConductivity = 0.1f;
    private volatile boolean onFire = false;
    private volatile float fireIntensity = 0.0f;
    
    // Material properties
    private volatile float density = 1.0f;
    private volatile float elasticity = 0.0f;
    private volatile float friction = 0.5f;
    private volatile float hardness = 1.0f;
    
    // Damage and wear
    private volatile float damage = 0.0f;
    private volatile float wear = 0.0f;
    private volatile long damageTime = 0;
    
    /**
     * Creates a new physics block.
     * 
     * @param position the block position
     * @param blockType the block type
     * @param properties additional properties
     */
    public PhysicsBlock(Vector3f position, BlockType blockType, Map<String, Object> properties) {
        this.position = new Vector3f(position);
        this.blockType = blockType;
        this.properties = new ConcurrentHashMap<>(properties != null ? properties : Map.of());
        
        // Initialize properties based on block type
        initializeBlockProperties();
    }
    
    /**
     * Initializes block properties based on the block type.
     */
    private void initializeBlockProperties() {
        switch (blockType) {
            case STONE -> {
                supportStrength = 2.0f;
                needsSupport = false;
                density = 2.5f;
                hardness = 3.0f;
                heatCapacity = 0.8f;
                thermalConductivity = 0.3f;
            }
            case WOOD -> {
                supportStrength = 1.5f;
                needsSupport = true;
                density = 0.8f;
                hardness = 1.5f;
                heatCapacity = 1.2f;
                thermalConductivity = 0.1f;
                elasticity = 0.3f;
            }
            case WATER -> {
                supportStrength = 0.0f;
                needsSupport = false;
                density = 1.0f;
                hardness = 0.0f;
                heatCapacity = 4.2f;
                thermalConductivity = 0.6f;
                fluidLevel = 1.0f;
                fluidType = BlockType.WATER;
            }
            case LAVA -> {
                supportStrength = 0.0f;
                needsSupport = false;
                density = 3.0f;
                hardness = 0.0f;
                temperature = 1200.0f;
                heatCapacity = 1.0f;
                thermalConductivity = 2.0f;
                fluidLevel = 1.0f;
                fluidType = BlockType.LAVA;
            }
            case SAND -> {
                supportStrength = 0.5f;
                needsSupport = true;
                density = 1.5f;
                hardness = 0.5f;
                heatCapacity = 0.8f;
                thermalConductivity = 0.2f;
                friction = 0.8f;
            }
            case LEAVES -> {
                supportStrength = 0.2f;
                needsSupport = true;
                density = 0.3f;
                hardness = 0.2f;
                heatCapacity = 1.5f;
                thermalConductivity = 0.05f;
                elasticity = 0.8f;
            }
            case COAL_ORE -> {
                supportStrength = 1.8f;
                needsSupport = false;
                density = 3.0f;
                hardness = 2.5f;
                heatCapacity = 0.5f;
                thermalConductivity = 0.8f;
            }
            case IRON_ORE -> {
                supportStrength = 2.0f;
                needsSupport = false;
                density = 3.5f;
                hardness = 3.0f;
                heatCapacity = 0.4f;
                thermalConductivity = 1.2f;
            }
            case GOLD_ORE -> {
                supportStrength = 1.5f;
                needsSupport = false;
                density = 4.0f;
                hardness = 2.0f;
                heatCapacity = 0.3f;
                thermalConductivity = 3.0f;
            }
            case DIRT -> {
                supportStrength = 0.8f;
                needsSupport = true;
                density = 1.3f;
                hardness = 0.8f;
                heatCapacity = 1.0f;
                thermalConductivity = 0.15f;
                friction = 0.7f;
            }
            case GRASS -> {
                supportStrength = 0.9f;
                needsSupport = true;
                density = 1.2f;
                hardness = 0.9f;
                heatCapacity = 1.1f;
                thermalConductivity = 0.12f;
                friction = 0.6f;
            }

            case OBSIDIAN -> {
                supportStrength = 3.0f;
                needsSupport = false;
                density = 2.4f;
                hardness = 5.0f;
                heatCapacity = 0.7f;
                thermalConductivity = 0.2f;
            }
            default -> {
                // Default properties for unknown blocks
                supportStrength = 1.0f;
                needsSupport = true;
                density = 1.0f;
                hardness = 1.0f;
            }
        }
    }
    
    /**
     * Updates the physics block state.
     * 
     * @param deltaTime the time elapsed since last update
     */
    public void update(double deltaTime) {
        lastUpdateTime.set(System.currentTimeMillis());
        
        // Check if block should remain active
        long age = System.currentTimeMillis() - creationTime.get();
        if (age > MAX_INACTIVE_AGE && !hasActivePhysics()) {
            active.set(false);
            return;
        }
        
        // Update thermal properties
        updateThermalState(deltaTime);
        
        // Update fluid properties
        updateFluidState(deltaTime);
        
        // Update structural state
        updateStructuralState(deltaTime);
        
        // Update damage and wear
        updateDamageState(deltaTime);
    }
    
    /**
     * Updates thermal state including temperature and fire.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateThermalState(double deltaTime) {
        // Cool down over time
        if (temperature > DEFAULT_TEMPERATURE) {
            float coolingRate = 0.1f * (float) deltaTime;
            temperature = Math.max(DEFAULT_TEMPERATURE, temperature - coolingRate);
        }
        
        // Update fire state
        if (onFire) {
            fireIntensity = Math.max(0.0f, fireIntensity - 0.05f * (float) deltaTime);
            if (fireIntensity <= 0.0f) {
                onFire = false;
            }
        }
    }
    
    /**
     * Updates fluid state including flow and pressure.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateFluidState(double deltaTime) {
        if (fluidLevel > 0.0f) {
            // Apply gravity to fluid velocity
            fluidVelocity.y -= 9.81f * (float) deltaTime;
            
            // Apply friction
            fluidVelocity.mul(1.0f - friction * (float) deltaTime);
            
            // Update pressure based on fluid level
            fluidPressure = fluidLevel * density * 9.81f;
        }
    }
    
    /**
     * Updates structural state including integrity and support.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateStructuralState(double deltaTime) {
        // Reduce integrity over time if damaged
        if (damage > 0.0f) {
            float degradationRate = damage * 0.01f * (float) deltaTime;
            structuralIntegrity = Math.max(0.0f, structuralIntegrity - degradationRate);
        }
        
        // Update wear based on stress
        if (isSupporting) {
            wear += 0.001f * (float) deltaTime;
        }
    }
    
    /**
     * Updates damage and wear state.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateDamageState(double deltaTime) {
        // Heal minor damage over time
        if (damage > 0.0f && damage < 0.1f) {
            damage = Math.max(0.0f, damage - 0.01f * (float) deltaTime);
        }
        
        // Increase wear if block is under stress
        if (structuralIntegrity < 0.5f) {
            wear += 0.002f * (float) deltaTime;
        }
    }
    
    /**
     * Checks if the block has active physics that require simulation.
     * 
     * @return true if the block has active physics
     */
    private boolean hasActivePhysics() {
        return fluidLevel > 0.0f || onFire || damage > 0.0f || 
               temperature > DEFAULT_TEMPERATURE + 5.0f || isSupporting;
    }
    
    /**
     * Applies damage to the block.
     * 
     * @param damageAmount the amount of damage to apply
     * @param damageType the type of damage
     */
    public void applyDamage(float damageAmount, String damageType) {
        damage = Math.min(1.0f, damage + damageAmount);
        damageTime = System.currentTimeMillis();
        
        // Reduce structural integrity based on damage
        structuralIntegrity = Math.max(0.0f, structuralIntegrity - damageAmount * 0.5f);
        
        logger.debug("Applied {} damage of type {} to block at {}", damageAmount, damageType, position);
    }
    
    /**
     * Sets the block on fire.
     * 
     * @param intensity the fire intensity (0.0 to 1.0)
     */
    public void ignite(float intensity) {
        onFire = true;
        fireIntensity = Math.max(fireIntensity, Math.min(1.0f, intensity));
        temperature = Math.max(temperature, 300.0f + intensity * 700.0f);
        
        logger.debug("Ignited block at {} with intensity {}", position, intensity);
    }
    
    /**
     * Extinguishes fire on the block.
     */
    public void extinguish() {
        onFire = false;
        fireIntensity = 0.0f;
        
        logger.debug("Extinguished fire on block at {}", position);
    }
    
    /**
     * Adds fluid to the block.
     * 
     * @param fluidType the type of fluid
     * @param amount the amount of fluid to add
     */
    public void addFluid(BlockType fluidType, float amount) {
        if (this.fluidType == BlockType.AIR || this.fluidType == fluidType) {
            this.fluidType = fluidType;
            this.fluidLevel = Math.min(1.0f, this.fluidLevel + amount);
        }
    }
    
    /**
     * Removes fluid from the block.
     * 
     * @param amount the amount of fluid to remove
     * @return the actual amount removed
     */
    public float removeFluid(float amount) {
        float removed = Math.min(fluidLevel, amount);
        fluidLevel -= removed;
        
        if (fluidLevel <= 0.0f) {
            fluidType = BlockType.AIR;
            fluidVelocity.set(0, 0, 0);
        }
        
        return removed;
    }
    
    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public BlockType getBlockType() { return blockType; }
    public boolean isActive() { return active.get(); }
    public long getLastUpdateTime() { return lastUpdateTime.get(); }
    public long getAge() { return System.currentTimeMillis() - creationTime.get(); }
    
    public float getStructuralIntegrity() { return structuralIntegrity; }
    public void setStructuralIntegrity(float integrity) { this.structuralIntegrity = Math.max(0.0f, Math.min(1.0f, integrity)); }
    
    public float getSupportStrength() { return supportStrength; }
    public boolean isSupporting() { return isSupporting; }
    public void setSupporting(boolean supporting) { this.isSupporting = supporting; }
    public boolean needsSupport() { return needsSupport; }
    
    public float getFluidLevel() { return fluidLevel; }
    public void setFluidLevel(float level) { this.fluidLevel = Math.max(0.0f, Math.min(1.0f, level)); }
    public BlockType getFluidType() { return fluidType; }
    public Vector3f getFluidVelocity() { return new Vector3f(fluidVelocity); }
    public void setFluidVelocity(Vector3f velocity) { this.fluidVelocity.set(velocity); }
    public float getFluidPressure() { return fluidPressure; }
    
    public float getTemperature() { return temperature; }
    public void setTemperature(float temp) { this.temperature = temp; }
    public boolean isOnFire() { return onFire; }
    public float getFireIntensity() { return fireIntensity; }
    
    public float getDensity() { return density; }
    public float getElasticity() { return elasticity; }
    public float getFriction() { return friction; }
    public float getHardness() { return hardness; }
    
    public float getDamage() { return damage; }
    public float getWear() { return wear; }
    
    public Object getProperty(String key) { return properties.get(key); }
    public void setProperty(String key, Object value) { properties.put(key, value); }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PhysicsBlock that = (PhysicsBlock) obj;
        return Objects.equals(position, that.position);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(position);
    }
    
    @Override
    public String toString() {
        return String.format("PhysicsBlock{pos=%s, type=%s, integrity=%.2f, fluid=%.2f, temp=%.1f°C}", 
                           position, blockType, structuralIntegrity, fluidLevel, temperature);
    }
}