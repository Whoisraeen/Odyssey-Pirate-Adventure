package com.odyssey.combat;

import com.odyssey.ship.Ship;
import com.odyssey.ship.components.CannonComponent;
import com.odyssey.util.Logger;
import com.odyssey.util.MathUtils;
import org.joml.Vector3f;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * CombatManager coordinates all combat activities in the game world.
 * It manages multiple CombatSystem instances and handles ship-to-ship combat,
 * damage resolution, and combat state management.
 * 
 * This class serves as the high-level interface for all combat operations,
 * integrating with ship systems, world physics, and damage calculations.
 */
public class CombatManager {
    private static final Logger logger = Logger.getLogger(CombatManager.class);
    
    private final CombatSystem combatSystem;
    private final Map<String, Ship> activeShips;
    private final List<CombatEngagement> activeEngagements;
    
    // Combat configuration
    private static final float MAX_COMBAT_RANGE = 2000.0f; // Maximum range for combat detection
    private static final float ENGAGEMENT_TIMEOUT = 300.0f; // 5 minutes without combat = disengagement
    private static final float DAMAGE_MULTIPLIER = 1.0f; // Global damage scaling
    
    /**
     * Creates a new CombatManager.
     */
    public CombatManager() {
        this.combatSystem = new CombatSystem();
        this.activeShips = new ConcurrentHashMap<>();
        this.activeEngagements = new ArrayList<>();
        
        logger.info("CombatManager initialized");
    }
    
    /**
     * Updates all combat systems and manages active engagements.
     * Should be called every frame.
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(float deltaTime) {
        // Update the core combat system (projectiles, physics)
        List<Ship> shipList = new ArrayList<>(activeShips.values());
        combatSystem.update(deltaTime, shipList);
        
        // Update active combat engagements
        updateEngagements(deltaTime);
        
        // Check for new potential engagements
        checkForNewEngagements();
        
        // Clean up expired engagements
        cleanupEngagements();
    }
    
    /**
     * Registers a ship for combat tracking.
     * 
     * @param ship The ship to register
     */
    public void registerShip(Ship ship) {
        if (ship != null && ship.getName() != null) {
            activeShips.put(ship.getName(), ship);
            logger.debug("Registered ship '{}' for combat tracking", ship.getName());
        }
    }
    
    /**
     * Unregisters a ship from combat tracking.
     * 
     * @param shipName The name of the ship to unregister
     */
    public void unregisterShip(String shipName) {
        Ship removed = activeShips.remove(shipName);
        if (removed != null) {
            // Remove ship from any active engagements
            activeEngagements.removeIf(engagement -> 
                engagement.getAttacker().getName().equals(shipName) ||
                engagement.getDefender().getName().equals(shipName));
            
            logger.debug("Unregistered ship '{}' from combat tracking", shipName);
        }
    }
    
    /**
     * Initiates cannon fire from a ship's cannon component.
     * 
     * @param firingShip The ship firing the cannon
     * @param cannonComponent The specific cannon being fired
     * @param targetPosition The target position to fire at
     * @return true if the shot was fired successfully
     */
    public boolean fireCannon(Ship firingShip, CannonComponent cannonComponent, Vector3f targetPosition) {
        if (firingShip == null || cannonComponent == null || targetPosition == null) {
            return false;
        }
        
        // Check if cannon can fire
        if (!cannonComponent.isLoaded() || cannonComponent.isFiring() || 
            cannonComponent.isMisfired() || cannonComponent.getCurrentReloadTime() > 0) {
            logger.debug("Cannon '{}' on ship '{}' cannot fire", 
                        cannonComponent.getName(), firingShip.getName());
            return false;
        }
        
        // Calculate firing solution
        Vector3f cannonPosition = calculateCannonWorldPosition(firingShip, cannonComponent);
        // Calculate direction vector using JOML Vector3f methods
        Vector3f direction = new Vector3f(targetPosition).sub(cannonPosition).normalize();
        
        // Create projectile through combat system
        boolean fired = combatSystem.fireProjectile(cannonComponent, firingShip, direction);
        
        if (fired) {
            // Trigger cannon firing effects
            cannonComponent.fire(direction);
            
            // Create or update combat engagement
            Ship targetShip = findNearestShip(targetPosition);
            if (targetShip != null && !targetShip.equals(firingShip)) {
                createOrUpdateEngagement(firingShip, targetShip);
            }
            
            logger.debug("Cannon fired from ship '{}' towards target", firingShip.getName());
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles damage application to a ship from combat.
     * 
     * @param targetShip The ship taking damage
     * @param damage The amount of damage to apply
     * @param damagePosition The world position where damage occurred
     * @param attackerName The name of the attacking ship (can be null)
     */
    public void applyDamage(Ship targetShip, float damage, Vector3f damagePosition, String attackerName) {
        if (targetShip == null || damage <= 0) {
            return;
        }
        
        // Apply damage multiplier
        float finalDamage = damage * DAMAGE_MULTIPLIER;
        
        // Apply damage to ship (this will distribute to components)
        // Convert world position to local position relative to ship
        Vector3f localPosition = new Vector3f(damagePosition).sub(targetShip.getPosition());
        targetShip.takeDamage(localPosition, finalDamage, com.odyssey.ship.DamageType.CANNON_BALL);
        
        logger.info("Ship '{}' took {} damage from {}", 
                   targetShip.getName(), finalDamage, 
                   attackerName != null ? attackerName : "unknown source");
        
        // Update engagement if this was from another ship
        if (attackerName != null) {
            Ship attacker = activeShips.get(attackerName);
            if (attacker != null) {
                createOrUpdateEngagement(attacker, targetShip);
            }
        }
    }
    
    /**
     * Checks if two ships are within combat range of each other.
     * 
     * @param ship1 First ship
     * @param ship2 Second ship
     * @return true if ships are within combat range
     */
    public boolean areShipsInCombatRange(Ship ship1, Ship ship2) {
        if (ship1 == null || ship2 == null) {
            return false;
        }
        
        Vector3f pos1 = ship1.getPosition();
        Vector3f pos2 = ship2.getPosition();
        float distance = MathUtils.distance(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
        return distance <= MAX_COMBAT_RANGE;
    }
    
    /**
     * Gets all active combat engagements.
     * 
     * @return List of active combat engagements
     */
    public List<CombatEngagement> getActiveEngagements() {
        return new ArrayList<>(activeEngagements);
    }
    
    /**
     * Sets wind conditions for projectile physics.
     * 
     * @param windDirection Wind direction vector
     * @param windStrength Wind strength (0.0 to 1.0)
     */
    public void setWindConditions(Vector3f windDirection, float windStrength) {
        combatSystem.setWindConditions(windDirection, windStrength);
    }
    
    /**
     * Gets the underlying combat system for direct access.
     * 
     * @return The CombatSystem instance
     */
    public CombatSystem getCombatSystem() {
        return combatSystem;
    }
    
    // Private helper methods
    
    private void updateEngagements(float deltaTime) {
        for (CombatEngagement engagement : activeEngagements) {
            engagement.update(deltaTime);
        }
    }
    
    private void checkForNewEngagements() {
        List<Ship> ships = new ArrayList<>(activeShips.values());
        
        for (int i = 0; i < ships.size(); i++) {
            for (int j = i + 1; j < ships.size(); j++) {
                Ship ship1 = ships.get(i);
                Ship ship2 = ships.get(j);
                
                if (areShipsInCombatRange(ship1, ship2)) {
                    // Check if engagement already exists
                    boolean engagementExists = activeEngagements.stream()
                        .anyMatch(e -> e.involvesShips(ship1, ship2));
                    
                    if (!engagementExists) {
                        // Determine if ships are hostile (simplified logic)
                        if (areShipsHostile(ship1, ship2)) {
                            createOrUpdateEngagement(ship1, ship2);
                        }
                    }
                }
            }
        }
    }
    
    private void cleanupEngagements() {
        activeEngagements.removeIf(engagement -> 
            engagement.getTimeSinceLastCombat() > ENGAGEMENT_TIMEOUT ||
            !areShipsInCombatRange(engagement.getAttacker(), engagement.getDefender()));
    }
    
    private void createOrUpdateEngagement(Ship attacker, Ship defender) {
        // Find existing engagement
        CombatEngagement existing = activeEngagements.stream()
            .filter(e -> e.involvesShips(attacker, defender))
            .findFirst()
            .orElse(null);
        
        if (existing != null) {
            existing.recordCombatActivity();
        } else {
            CombatEngagement newEngagement = new CombatEngagement(attacker, defender);
            activeEngagements.add(newEngagement);
            logger.info("Combat engagement started between '{}' and '{}'", 
                       attacker.getName(), defender.getName());
        }
    }
    
    private Vector3f calculateCannonWorldPosition(Ship ship, CannonComponent cannon) {
        // Simplified calculation - in a real implementation, this would account for
        // ship rotation, cannon mount position, etc.
        Vector3f shipPos = ship.getPosition();
        Vector3f cannonOffset = cannon.getPosition(); // Use component position instead of getMountPosition
        
        // For now, just add the offset to ship position
        return new Vector3f(shipPos).add(cannonOffset);
    }
    
    private Ship findNearestShip(Vector3f position) {
        Ship nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (Ship ship : activeShips.values()) {
            Vector3f shipPos = ship.getPosition();
            float distance = MathUtils.distance(position.x, position.y, position.z, 
                                              shipPos.x, shipPos.y, shipPos.z);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = ship;
            }
        }
        
        return nearest;
    }
    
    private boolean areShipsHostile(Ship ship1, Ship ship2) {
        // Simplified hostility check - in a real implementation, this would
        // check faction relationships, player vs AI, etc.
        // For now, assume all ships are potentially hostile
        return true;
    }
}