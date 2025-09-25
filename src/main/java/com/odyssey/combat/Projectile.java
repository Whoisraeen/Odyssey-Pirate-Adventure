package com.odyssey.combat;

import com.odyssey.ship.DamageType;
import com.odyssey.ship.Ship;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

import java.util.List;

/**
 * Represents a projectile fired from a cannon or other weapon.
 * Handles physics simulation, trajectory calculation, and impact detection.
 */
public class Projectile {
    private static final Logger logger = Logger.getLogger(Projectile.class);
    
    // Projectile properties
    private final ProjectileType type;
    private final Vector3f position;
    private final Vector3f velocity;
    private final Vector3f acceleration;
    private final float damage;
    private final float range;
    private final float accuracy;
    private final DamageType damageType;
    private final Ship firingShip;
    
    // Physics state
    private float timeAlive;
    private float maxLifetime;
    private boolean isActive;
    private boolean hasExploded;
    
    // Environmental factors
    private float windResistance;
    private float gravityEffect;
    private float waterDrag;
    
    /**
     * Creates a new projectile
     */
    public Projectile(ProjectileType type, Vector3f startPosition, Vector3f initialVelocity,
                     float damage, float range, float accuracy, DamageType damageType, Ship firingShip) {
        this.type = type;
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(initialVelocity);
        this.acceleration = new Vector3f(0, -9.81f, 0); // Gravity
        this.damage = damage;
        this.range = range;
        this.accuracy = accuracy;
        this.damageType = damageType;
        this.firingShip = firingShip;
        
        this.timeAlive = 0.0f;
        this.maxLifetime = calculateMaxLifetime();
        this.isActive = true;
        this.hasExploded = false;
        
        // Apply accuracy deviation
        applyAccuracyDeviation();
        
        // Set environmental factors based on projectile type
        this.windResistance = type.getWindResistance();
        this.gravityEffect = type.getGravityEffect();
        this.waterDrag = type.getWaterDrag();
        
        logger.debug("Projectile created: {} at {} with velocity {}", type.getName(), position, velocity);
    }
    
    /**
     * Updates projectile physics and position
     */
    public void update(float deltaTime, Vector3f windVelocity) {
        if (!isActive) return;
        
        timeAlive += deltaTime;
        
        // Check if projectile has exceeded its lifetime
        if (timeAlive >= maxLifetime) {
            deactivate();
            return;
        }
        
        // Apply environmental forces
        applyEnvironmentalForces(windVelocity, deltaTime);
        
        // Update velocity with acceleration
        velocity.add(acceleration.x * deltaTime, acceleration.y * deltaTime, acceleration.z * deltaTime);
        
        // Update position with velocity
        position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
        
        // Check for water impact
        if (position.y <= 0.0f && !hasExploded) {
            handleWaterImpact();
        }
        
        // Check if projectile has traveled beyond its effective range
        Vector3f startPos = firingShip.getPosition();
        float distanceTraveled = position.distance(startPos);
        if (distanceTraveled > range) {
            deactivate();
        }
    }
    
    /**
     * Applies environmental forces like wind and drag
     */
    private void applyEnvironmentalForces(Vector3f windVelocity, float deltaTime) {
        // Wind effect
        Vector3f windForce = new Vector3f(windVelocity).mul(windResistance * deltaTime);
        velocity.add(windForce);
        
        // Air drag
        float speed = velocity.length();
        if (speed > 0) {
            Vector3f dragForce = new Vector3f(velocity).normalize().mul(-speed * speed * 0.01f * deltaTime);
            velocity.add(dragForce);
        }
        
        // Gravity adjustment
        acceleration.y = -9.81f * gravityEffect;
    }
    
    /**
     * Applies accuracy deviation to the initial velocity
     */
    private void applyAccuracyDeviation() {
        float inaccuracy = 1.0f - accuracy;
        float maxDeviation = inaccuracy * 0.2f; // Maximum 20% deviation for 0 accuracy
        
        // Apply random deviation to velocity components
        float xDeviation = (float)(Math.random() - 0.5) * 2 * maxDeviation;
        float yDeviation = (float)(Math.random() - 0.5) * 2 * maxDeviation * 0.5f; // Less vertical deviation
        float zDeviation = (float)(Math.random() - 0.5) * 2 * maxDeviation;
        
        velocity.mul(1.0f + xDeviation, 1.0f + yDeviation, 1.0f + zDeviation);
    }
    
    /**
     * Calculates maximum lifetime based on range and initial velocity
     */
    private float calculateMaxLifetime() {
        float initialSpeed = velocity.length();
        return (range / initialSpeed) * 1.5f; // 50% buffer for environmental factors
    }
    
    /**
     * Handles projectile impact with water
     */
    private void handleWaterImpact() {
        switch (type) {
            case CANNON_BALL:
                // Cannonballs sink immediately
                deactivate();
                break;
                
            case CHAIN_SHOT:
                // Chain shot skips once then sinks
                if (!hasExploded) {
                    velocity.y = Math.abs(velocity.y) * 0.3f; // Bounce with reduced energy
                    hasExploded = true;
                } else {
                    deactivate();
                }
                break;
                
            case GRAPE_SHOT:
                // Grape shot spreads on water impact
                createGrapeShotSpread();
                deactivate();
                break;
                
            case EXPLOSIVE_SHOT:
                // Explosive shot detonates on water impact
                createExplosion();
                deactivate();
                break;
        }
        
        logger.debug("Projectile {} impacted water at {}", type.getName(), position);
    }
    
    /**
     * Creates grape shot spread effect
     */
    private void createGrapeShotSpread() {
        // This would create multiple smaller projectiles
        // For now, just increase damage area
        logger.debug("Grape shot spread created at {}", position);
    }
    
    /**
     * Creates explosion effect
     */
    private void createExplosion() {
        // This would create an explosion with area damage
        logger.debug("Explosion created at {} with damage {}", position, damage * 1.5f);
    }
    
    /**
     * Checks if projectile hits a target ship
     */
    public boolean checkCollision(Ship targetShip) {
        if (!isActive || targetShip == firingShip) return false;
        
        Vector3f shipPos = targetShip.getPosition();
        float shipLength = targetShip.getShipType().getLength();
        float shipWidth = targetShip.getShipType().getWidth();
        float shipHeight = targetShip.getShipType().getHeight();
        
        // Simple bounding box collision
        return position.x >= shipPos.x - shipWidth/2 && position.x <= shipPos.x + shipWidth/2 &&
               position.y >= shipPos.y - shipHeight/2 && position.y <= shipPos.y + shipHeight/2 &&
               position.z >= shipPos.z - shipLength/2 && position.z <= shipPos.z + shipLength/2;
    }
    
    /**
     * Applies damage to target ship
     */
    public void applyDamage(Ship targetShip) {
        if (!isActive) return;
        
        // Calculate impact position relative to ship
        Vector3f impactPos = new Vector3f(position).sub(targetShip.getPosition());
        
        // Apply damage based on projectile type
        float actualDamage = calculateActualDamage(targetShip);
        targetShip.takeDamage(impactPos, actualDamage, damageType);
        
        // Handle special effects
        handleSpecialEffects(targetShip);
        
        deactivate();
        logger.info("Projectile {} hit {} for {} damage", type.getName(), targetShip.getName(), actualDamage);
    }
    
    /**
     * Calculates actual damage based on projectile type and target
     */
    private float calculateActualDamage(Ship targetShip) {
        float actualDamage = damage;
        
        // Apply projectile-specific damage modifiers
        switch (type) {
            case CANNON_BALL:
                // Standard damage, no modifier
                break;
            case CHAIN_SHOT:
                // Extra damage to sails and rigging
                actualDamage *= 1.2f;
                break;
            case GRAPE_SHOT:
                // Extra damage to crew
                actualDamage *= 1.5f;
                break;
            case EXPLOSIVE_SHOT:
                // Area damage
                actualDamage *= 1.8f;
                break;
        }
        
        // Consider impact velocity
        float impactSpeed = velocity.length();
        float speedMultiplier = Math.min(2.0f, impactSpeed / 50.0f); // Cap at 2x damage
        actualDamage *= speedMultiplier;
        
        return actualDamage;
    }
    
    /**
     * Handles special effects based on projectile type
     */
    private void handleSpecialEffects(Ship targetShip) {
        switch (type) {
            case CANNON_BALL:
                // Standard projectile, no special effects
                break;
                
            case CHAIN_SHOT:
                // Damage sails specifically
                targetShip.getComponents().values().stream()
                    .flatMap(List::stream)
                    .filter(c -> c.getType().toString().contains("SAIL"))
                    .forEach(sail -> sail.takeDamage(damage * 0.5f, DamageType.CANNON_BALL));
                break;
                
            case EXPLOSIVE_SHOT:
                // Fire damage over time
                // This would be handled by a fire system
                break;
                
            case GRAPE_SHOT:
                // Crew casualties
                // This would be handled by a crew management system
                break;
        }
    }
    
    /**
     * Deactivates the projectile
     */
    public void deactivate() {
        isActive = false;
        logger.debug("Projectile {} deactivated", type.getName());
    }
    
    // Getters
    public ProjectileType getType() { return type; }
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public float getDamage() { return damage; }
    public float getRange() { return range; }
    public boolean isActive() { return isActive; }
    public Ship getFiringShip() { return firingShip; }
    public float getTimeAlive() { return timeAlive; }
    
    /**
     * Projectile types with different characteristics
     */
    public enum ProjectileType {
        CANNON_BALL("Cannon Ball", 1.0f, 1.0f, 0.8f),
        CHAIN_SHOT("Chain Shot", 1.2f, 0.9f, 1.0f),
        GRAPE_SHOT("Grape Shot", 0.8f, 1.1f, 0.9f),
        EXPLOSIVE_SHOT("Explosive Shot", 0.9f, 1.0f, 0.7f);
        
        private final String name;
        private final float windResistance;
        private final float gravityEffect;
        private final float waterDrag;
        
        ProjectileType(String name, float windResistance, float gravityEffect, float waterDrag) {
            this.name = name;
            this.windResistance = windResistance;
            this.gravityEffect = gravityEffect;
            this.waterDrag = waterDrag;
        }
        
        public String getName() { return name; }
        public float getWindResistance() { return windResistance; }
        public float getGravityEffect() { return gravityEffect; }
        public float getWaterDrag() { return waterDrag; }
    }
}