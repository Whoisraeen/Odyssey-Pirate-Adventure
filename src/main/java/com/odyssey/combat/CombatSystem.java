package com.odyssey.combat;

import com.odyssey.ship.Ship;
import com.odyssey.ship.components.CannonComponent;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages naval combat system including projectile physics, targeting, and damage resolution.
 * Handles all active projectiles and combat interactions between ships.
 */
public class CombatSystem {
    private static final Logger logger = Logger.getLogger(CombatSystem.class);
    
    // Active projectiles in the world
    private final List<Projectile> activeProjectiles;
    
    // Combat settings
    private float maxCombatRange;
    private boolean friendlyFireEnabled;
    private float windStrength;
    private Vector3f windDirection;
    
    // Performance tracking
    private int maxProjectiles;
    private long lastCleanupTime;
    private static final long CLEANUP_INTERVAL = 5000; // 5 seconds
    
    /**
     * Creates a new combat system
     */
    public CombatSystem() {
        this.activeProjectiles = new CopyOnWriteArrayList<>();
        this.maxCombatRange = 1000.0f;
        this.friendlyFireEnabled = false;
        this.windStrength = 5.0f;
        this.windDirection = new Vector3f(1, 0, 0);
        this.maxProjectiles = 500;
        this.lastCleanupTime = System.currentTimeMillis();
        
        logger.info("Combat system initialized with max range: {}, max projectiles: {}", 
                   maxCombatRange, maxProjectiles);
    }
    
    /**
     * Updates all active projectiles and handles collisions
     */
    public void update(float deltaTime, List<Ship> ships) {
        // Calculate wind velocity
        Vector3f windVelocity = new Vector3f(windDirection).mul(windStrength);
        
        // Update all projectiles
        Iterator<Projectile> projectileIterator = activeProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            
            if (!projectile.isActive()) {
                projectileIterator.remove();
                continue;
            }
            
            // Update projectile physics
            projectile.update(deltaTime, windVelocity);
            
            // Check collisions with ships
            checkProjectileCollisions(projectile, ships);
        }
        
        // Periodic cleanup
        performPeriodicCleanup();
    }
    
    /**
     * Fires a projectile from a cannon
     */
    public boolean fireProjectile(CannonComponent cannon, Ship firingShip, Vector3f targetDirection) {
        if (activeProjectiles.size() >= maxProjectiles) {
            logger.warn("Maximum projectile limit reached, cannot fire");
            return false;
        }
        
        // Calculate firing position (cannon position + ship position)
        Vector3f firingPosition = new Vector3f(cannon.getPosition()).add(firingShip.getPosition());
        
        // Calculate initial velocity based on cannon properties and target direction
        Vector3f initialVelocity = calculateInitialVelocity(cannon, targetDirection);
        
        // Determine projectile type based on loaded ammunition
        Projectile.ProjectileType projectileType = getProjectileType(cannon.getLoadedAmmo());
        
        // Create projectile
        Projectile projectile = new Projectile(
            projectileType,
            firingPosition,
            initialVelocity,
            cannon.getDamage(),
            cannon.getRange(),
            cannon.getAccuracy(),
            cannon.getLoadedAmmo().getDamageType(),
            firingShip
        );
        
        activeProjectiles.add(projectile);
        
        logger.info("Projectile fired from {} cannon on ship {}: {} projectiles active", 
                   cannon.getName(), firingShip.getName(), activeProjectiles.size());
        
        return true;
    }
    
    /**
     * Calculates initial velocity for projectile based on cannon and target
     */
    private Vector3f calculateInitialVelocity(CannonComponent cannon, Vector3f targetDirection) {
        // Base muzzle velocity (realistic for naval cannons: 400-600 m/s)
        float muzzleVelocity = 500.0f;
        
        // Apply cannon-specific modifiers
        switch (cannon.getCannonType()) {
            case LIGHT_CANNON:
                muzzleVelocity = 450.0f;
                break;
            case MEDIUM_CANNON:
                muzzleVelocity = 500.0f;
                break;
            case HEAVY_CANNON:
                muzzleVelocity = 550.0f;
                break;
            case SWIVEL_GUN:
                muzzleVelocity = 300.0f;
                break;
            case CARRONADE:
                muzzleVelocity = 400.0f;
                break;
        }
        
        // Apply upgrade bonuses
        muzzleVelocity *= (1.0f + cannon.getUpgradeLevel() * 0.05f);
        
        // Apply crew efficiency
        muzzleVelocity *= cannon.getCrewEfficiency();
        
        // Apply barrel wear penalty
        float wearPenalty = Math.min(0.3f, cannon.getBarrelWear() / cannon.getMaxHealth());
        muzzleVelocity *= (1.0f - wearPenalty);
        
        // Calculate trajectory with slight upward angle for range
        Vector3f velocity = new Vector3f(targetDirection).normalize();
        velocity.y += 0.1f; // Slight elevation for optimal range
        velocity.normalize().mul(muzzleVelocity);
        
        return velocity;
    }
    
    /**
     * Converts ammunition type to projectile type
     */
    private Projectile.ProjectileType getProjectileType(CannonComponent.AmmoType ammoType) {
        if (ammoType == null) return Projectile.ProjectileType.CANNON_BALL;
        
        switch (ammoType) {
            case CANNON_BALL:
                return Projectile.ProjectileType.CANNON_BALL;
            case CHAIN_SHOT:
                return Projectile.ProjectileType.CHAIN_SHOT;
            case GRAPE_SHOT:
                return Projectile.ProjectileType.GRAPE_SHOT;
            case EXPLOSIVE_SHOT:
                return Projectile.ProjectileType.EXPLOSIVE_SHOT;
            default:
                return Projectile.ProjectileType.CANNON_BALL;
        }
    }
    
    /**
     * Checks projectile collisions with all ships
     */
    private void checkProjectileCollisions(Projectile projectile, List<Ship> ships) {
        for (Ship ship : ships) {
            // Skip collision with firing ship unless friendly fire is enabled
            if (ship == projectile.getFiringShip() && !friendlyFireEnabled) {
                continue;
            }
            
            // Check if projectile hits ship
            if (projectile.checkCollision(ship)) {
                projectile.applyDamage(ship);
                
                // Log combat event
                logger.info("Combat hit: {} projectile from {} hit {} for {} damage",
                           projectile.getType().getName(),
                           projectile.getFiringShip().getName(),
                           ship.getName(),
                           projectile.getDamage());
                
                break; // Projectile is consumed on first hit
            }
        }
    }
    
    /**
     * Calculates optimal firing solution for hitting a moving target
     */
    public Vector3f calculateFiringSolution(Vector3f firingPosition, Vector3f targetPosition, 
                                          Vector3f targetVelocity, float projectileSpeed) {
        
        // Simple predictive targeting
        Vector3f relativePosition = new Vector3f(targetPosition).sub(firingPosition);
        float distance = relativePosition.length();
        float timeToTarget = distance / projectileSpeed;
        
        // Predict target position
        Vector3f predictedPosition = new Vector3f(targetPosition)
            .add(new Vector3f(targetVelocity).mul(timeToTarget));
        
        // Calculate firing direction
        Vector3f firingDirection = new Vector3f(predictedPosition).sub(firingPosition).normalize();
        
        return firingDirection;
    }
    
    /**
     * Checks if two ships are within combat range
     */
    public boolean isInCombatRange(Ship ship1, Ship ship2) {
        float distance = ship1.getPosition().distance(ship2.getPosition());
        return distance <= maxCombatRange;
    }
    
    /**
     * Gets all ships within combat range of a given ship
     */
    public List<Ship> getShipsInRange(Ship ship, List<Ship> allShips) {
        List<Ship> shipsInRange = new ArrayList<>();
        
        for (Ship otherShip : allShips) {
            if (otherShip != ship && isInCombatRange(ship, otherShip)) {
                shipsInRange.add(otherShip);
            }
        }
        
        return shipsInRange;
    }
    
    /**
     * Performs periodic cleanup of inactive projectiles and optimization
     */
    private void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            int initialCount = activeProjectiles.size();
            
            // Remove inactive projectiles
            activeProjectiles.removeIf(projectile -> !projectile.isActive());
            
            int removedCount = initialCount - activeProjectiles.size();
            if (removedCount > 0) {
                logger.debug("Cleaned up {} inactive projectiles, {} remaining", 
                           removedCount, activeProjectiles.size());
            }
            
            lastCleanupTime = currentTime;
        }
    }
    
    /**
     * Sets wind conditions affecting projectile trajectories
     */
    public void setWindConditions(Vector3f direction, float strength) {
        this.windDirection = new Vector3f(direction).normalize();
        this.windStrength = strength;
        logger.debug("Wind conditions updated: direction={}, strength={}", windDirection, windStrength);
    }
    
    /**
     * Clears all active projectiles (useful for scene transitions)
     */
    public void clearAllProjectiles() {
        int count = activeProjectiles.size();
        activeProjectiles.clear();
        logger.info("Cleared {} active projectiles", count);
    }
    
    // Getters and setters
    public List<Projectile> getActiveProjectiles() { 
        return new ArrayList<>(activeProjectiles); 
    }
    
    public int getActiveProjectileCount() { 
        return activeProjectiles.size(); 
    }
    
    public float getMaxCombatRange() { 
        return maxCombatRange; 
    }
    
    public void setMaxCombatRange(float maxCombatRange) { 
        this.maxCombatRange = maxCombatRange; 
    }
    
    public boolean isFriendlyFireEnabled() { 
        return friendlyFireEnabled; 
    }
    
    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) { 
        this.friendlyFireEnabled = friendlyFireEnabled; 
    }
    
    public int getMaxProjectiles() { 
        return maxProjectiles; 
    }
    
    public void setMaxProjectiles(int maxProjectiles) { 
        this.maxProjectiles = maxProjectiles; 
    }
    
    public Vector3f getWindDirection() { 
        return new Vector3f(windDirection); 
    }
    
    public float getWindStrength() { 
        return windStrength; 
    }
}