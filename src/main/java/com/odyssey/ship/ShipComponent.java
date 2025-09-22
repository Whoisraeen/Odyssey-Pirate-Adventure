package com.odyssey.ship;

import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Abstract base class for all ship components.
 * Components are modular parts that can be added, removed, damaged, and repaired.
 */
public abstract class ShipComponent {
    
    // Logger instance
    protected final Logger logger;
    
    // Component identification
    protected final ComponentType type;
    protected final String name;
    protected final int componentId;
    
    // Position and orientation
    protected final Vector3f position;
    protected final Vector3f rotation;
    
    // Component stats
    protected float health;
    protected float maxHealth;
    protected float mass;
    protected float durability;
    
    // Component state
    protected boolean isActive;
    protected boolean isDestroyed;
    protected float efficiency; // 0.0 to 1.0 based on damage
    
    // Upgrade system
    protected int upgradeLevel;
    protected static final int MAX_UPGRADE_LEVEL = 5;
    
    // Static ID counter
    private static int nextId = 1;
    
    public ShipComponent(ComponentType type, String name, Vector3f position, float maxHealth, float mass) {
        this.logger = Logger.getLogger(ShipComponent.class);
        this.type = type;
        this.name = name;
        this.componentId = nextId++;
        
        this.position = new Vector3f(position);
        this.rotation = new Vector3f();
        
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.mass = mass;
        this.durability = 1.0f;
        
        this.isActive = true;
        this.isDestroyed = false;
        this.efficiency = 1.0f;
        
        this.upgradeLevel = 0;
    }
    
    /**
     * Updates the component each frame
     */
    public void update(float deltaTime) {
        // Update efficiency based on health
        updateEfficiency();
        
        // Check if component is destroyed
        if (health <= 0 && !isDestroyed) {
            onDestroyed();
        }
        
        // Component-specific update logic
        updateComponent(deltaTime);
    }
    
    /**
     * Component-specific update logic (to be implemented by subclasses)
     */
    protected abstract void updateComponent(float deltaTime);
    
    /**
     * Takes damage to this component
     */
    public void takeDamage(float damage, DamageType damageType) {
        if (isDestroyed) return;
        
        // Apply damage resistance based on component type and damage type
        float actualDamage = calculateActualDamage(damage, damageType);
        
        health -= actualDamage;
        health = Math.max(0, health);
        
        logger.warn(Logger.WORLD, "Component {} took {} {} damage (health: {}/{})",
                   name, damage, damageType, health, maxHealth);
        
        // Check for destruction
        if (health <= 0) {
            onDestroyed();
        }
    }
    
    /**
     * Repairs this component
     */
    public void repair(float repairAmount) {
        if (isDestroyed) {
            // Can't repair destroyed components without replacement
            return;
        }
        
        health += repairAmount;
        health = Math.min(maxHealth, health);
        
        // Reactivate if repaired above threshold
        if (!isActive && health > maxHealth * 0.25f) {
            isActive = true;
        }
    }
    
    /**
     * Upgrades this component
     */
    public boolean upgrade() {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL) {
            return false;
        }
        
        upgradeLevel++;
        
        // Apply upgrade bonuses
        applyUpgradeBonus();
        
        logger.info(Logger.WORLD, "Upgraded component {} to level {}", name, upgradeLevel);
        return true;
    }
    
    /**
     * Replaces a destroyed component
     */
    public void replace() {
        if (!isDestroyed) return;
        
        health = maxHealth;
        isDestroyed = false;
        isActive = true;
        efficiency = 1.0f;
        
        logger.info(Logger.WORLD, "Replaced destroyed component {}", name);
    }
    
    /**
     * Gets the repair cost for this component
     */
    public int getRepairCost() {
        float damagePercent = 1.0f - (health / maxHealth);
        return (int) (mass * damagePercent * 10.0f);
    }
    
    /**
     * Gets the upgrade cost for the next level
     */
    public int getUpgradeCost() {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL) return -1;
        return (int) (mass * (upgradeLevel + 1) * 50.0f);
    }
    
    /**
     * Checks if this component can be upgraded
     */
    public boolean canUpgrade() {
        return upgradeLevel < MAX_UPGRADE_LEVEL && !isDestroyed && health > maxHealth * 0.8f;
    }
    
    /**
     * Gets component effectiveness (0.0 to 1.0)
     */
    public float getEffectiveness() {
        if (isDestroyed || !isActive) return 0.0f;
        return efficiency;
    }
    
    /**
     * Calculates actual damage after resistances
     */
    protected float calculateActualDamage(float damage, DamageType damageType) {
        float resistance = getDamageResistance(damageType);
        float durabilityFactor = durability + (upgradeLevel * 0.1f);
        
        return damage * (1.0f - resistance) / durabilityFactor;
    }
    
    /**
     * Gets damage resistance for a specific damage type
     */
    protected abstract float getDamageResistance(DamageType damageType);
    
    /**
     * Updates component efficiency based on health and other factors
     */
    protected void updateEfficiency() {
        if (isDestroyed) {
            efficiency = 0.0f;
            return;
        }
        
        // Base efficiency from health
        float healthRatio = health / maxHealth;
        efficiency = healthRatio;
        
        // Upgrade bonus
        efficiency += upgradeLevel * 0.05f;
        
        // Clamp to valid range
        efficiency = Math.max(0.0f, Math.min(1.0f, efficiency));
        
        // Deactivate if efficiency is too low
        if (efficiency < 0.25f && isActive) {
            isActive = false;
            logger.warn(Logger.WORLD, "Component {} deactivated due to low efficiency", name);
        }
    }
    
    /**
     * Called when the component is destroyed
     */
    protected void onDestroyed() {
        isDestroyed = true;
        isActive = false;
        efficiency = 0.0f;
        
        logger.error(Logger.WORLD, "Component {} has been destroyed!", name);
        
        // Component-specific destruction effects
        onDestroyedEffect();
    }
    
    /**
     * Component-specific destruction effects
     */
    protected abstract void onDestroyedEffect();
    
    /**
     * Applies upgrade bonuses
     */
    protected void applyUpgradeBonus() {
        // Increase max health
        maxHealth *= 1.1f;
        health = Math.min(health * 1.1f, maxHealth);
        
        // Increase durability
        durability *= 1.05f;
        
        // Component-specific upgrade effects
        applySpecificUpgradeBonus();
    }
    
    /**
     * Component-specific upgrade bonuses
     */
    protected abstract void applySpecificUpgradeBonus();
    
    /**
     * Gets component information for UI display
     */
    public ComponentInfo getInfo() {
        return new ComponentInfo(
            name,
            type,
            health,
            maxHealth,
            mass,
            efficiency,
            upgradeLevel,
            isActive,
            isDestroyed
        );
    }
    
    // Getters
    public ComponentType getType() { return type; }
    public ComponentType getComponentType() { return type; } // Alias for compatibility
    public String getName() { return name; }
    public int getComponentId() { return componentId; }
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getHealthPercentage() { return health / maxHealth; }
    public float getMass() { return mass; }
    public float getDurability() { return durability; }
    public boolean isActive() { return isActive; }
    public boolean isDestroyed() { return isDestroyed; }
    public float getEfficiency() { return efficiency; }
    public int getUpgradeLevel() { return upgradeLevel; }
    
    // Setters
    public void setActive(boolean active) { this.isActive = active; }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setRotation(Vector3f rotation) { this.rotation.set(rotation); }
    public void setUpgradeLevel(int level) { 
        this.upgradeLevel = Math.max(0, Math.min(level, MAX_UPGRADE_LEVEL)); 
    }
    
    /**
     * Component information for UI display
     */
    public static class ComponentInfo {
        public final String name;
        public final ComponentType type;
        public final float health;
        public final float maxHealth;
        public final float mass;
        public final float efficiency;
        public final int upgradeLevel;
        public final boolean isActive;
        public final boolean isDestroyed;
        
        public ComponentInfo(String name, ComponentType type, float health, float maxHealth,
                           float mass, float efficiency, int upgradeLevel, 
                           boolean isActive, boolean isDestroyed) {
            this.name = name;
            this.type = type;
            this.health = health;
            this.maxHealth = maxHealth;
            this.mass = mass;
            this.efficiency = efficiency;
            this.upgradeLevel = upgradeLevel;
            this.isActive = isActive;
            this.isDestroyed = isDestroyed;
        }
        
        public float getHealthPercentage() {
            return maxHealth > 0 ? health / maxHealth : 0;
        }
    }
}