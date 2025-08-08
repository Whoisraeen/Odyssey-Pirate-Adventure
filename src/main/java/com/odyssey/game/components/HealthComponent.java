package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;

/**
 * Component that manages entity health and damage.
 */
public class HealthComponent implements Component {
    public float maxHealth;
    public float currentHealth;
    public float armor; // Damage reduction (0-1, where 0.5 = 50% damage reduction)
    public boolean invulnerable; // If true, takes no damage
    public float regenRate; // Health regeneration per second
    public float lastDamageTime; // Time since last damage (for regen delay)
    public float regenDelay; // Delay before regeneration starts after taking damage
    
    public HealthComponent() {
        this(100.0f);
    }
    
    public HealthComponent(float maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.armor = 0.0f;
        this.invulnerable = false;
        this.regenRate = 0.0f;
        this.lastDamageTime = 0.0f;
        this.regenDelay = 5.0f; // 5 seconds default
    }
    
    /**
     * Deal damage to this entity.
     * @param damage The amount of damage to deal
     * @param gameTime Current game time for tracking damage timing
     * @return The actual damage dealt after armor reduction
     */
    public float takeDamage(float damage, float gameTime) {
        if (invulnerable || isDead()) {
            return 0.0f;
        }
        
        // Apply armor reduction
        float actualDamage = damage * (1.0f - armor);
        actualDamage = Math.max(0, actualDamage);
        
        currentHealth -= actualDamage;
        currentHealth = Math.max(0, currentHealth);
        
        lastDamageTime = gameTime;
        
        return actualDamage;
    }
    
    /**
     * Heal this entity.
     * @param amount The amount of health to restore
     * @return The actual amount healed
     */
    public float heal(float amount) {
        if (isDead()) {
            return 0.0f;
        }
        
        float oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        return currentHealth - oldHealth;
    }
    
    /**
     * Set the current health directly.
     */
    public void setHealth(float health) {
        currentHealth = Math.max(0, Math.min(maxHealth, health));
    }
    
    /**
     * Set the maximum health and optionally adjust current health.
     */
    public void setMaxHealth(float maxHealth, boolean adjustCurrent) {
        this.maxHealth = Math.max(1, maxHealth);
        if (adjustCurrent) {
            currentHealth = Math.min(currentHealth, this.maxHealth);
        }
    }
    
    /**
     * Check if this entity is dead.
     */
    public boolean isDead() {
        return currentHealth <= 0;
    }
    
    /**
     * Check if this entity is at full health.
     */
    public boolean isFullHealth() {
        return currentHealth >= maxHealth;
    }
    
    /**
     * Get the health percentage (0.0 to 1.0).
     */
    public float getHealthPercentage() {
        return maxHealth > 0 ? currentHealth / maxHealth : 0;
    }
    
    /**
     * Update regeneration (call this every frame).
     */
    public void updateRegeneration(float deltaTime, float gameTime) {
        if (regenRate > 0 && !isDead() && !isFullHealth()) {
            // Check if enough time has passed since last damage
            if (gameTime - lastDamageTime >= regenDelay) {
                heal(regenRate * deltaTime);
            }
        }
    }
    
    /**
     * Fully restore health.
     */
    public void fullHeal() {
        currentHealth = maxHealth;
    }
    
    /**
     * Kill this entity instantly.
     */
    public void kill() {
        currentHealth = 0;
    }
    
    /**
     * Revive this entity with a percentage of max health.
     */
    public void revive(float healthPercentage) {
        if (isDead()) {
            currentHealth = maxHealth * Math.max(0, Math.min(1, healthPercentage));
        }
    }
    
    @Override
    public Component copy() {
        HealthComponent copy = new HealthComponent(maxHealth);
        copy.currentHealth = currentHealth;
        copy.armor = armor;
        copy.invulnerable = invulnerable;
        copy.regenRate = regenRate;
        copy.lastDamageTime = lastDamageTime;
        copy.regenDelay = regenDelay;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Health{%.1f/%.1f (%.1f%%), armor=%.1f, regen=%.1f}", 
                           currentHealth, maxHealth, getHealthPercentage() * 100, 
                           armor * 100, regenRate);
    }
}