package com.odyssey.ship.components;

import com.odyssey.ship.ShipComponent;
import com.odyssey.ship.ComponentType;
import com.odyssey.ship.DamageType;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Cannon component - provides ranged combat capabilities for ships.
 * Handles ammunition, firing mechanics, targeting, and combat effectiveness.
 */
public class CannonComponent extends ShipComponent {
    
    // Cannon properties
    private final CannonType cannonType;
    private final float range;
    private final float damage;
    private final float accuracy;
    private final float reloadTime;
    
    // Firing state
    private float currentReloadTime;
    private boolean isLoaded;
    private boolean isFiring;
    private AmmoType loadedAmmo;
    
    // Targeting
    private Vector3f aimDirection;
    private float elevation; // Vertical aim angle
    private float traverse; // Horizontal aim angle
    private float maxTraverse; // Maximum horizontal rotation
    private float maxElevation; // Maximum vertical rotation
    
    // Ammunition storage
    private int cannonBalls;
    private int chainShot;
    private int grapeShot;
    private int explosiveShot;
    private int maxAmmoCapacity;
    
    // Cannon condition
    private float barrelWear; // Accumulated wear from firing
    private float overheating; // Heat buildup from rapid firing
    private boolean isMisfired; // Cannon has misfired and needs clearing
    private int consecutiveFires; // Track rapid firing
    
    // Crew requirements
    private int requiredCrew;
    private int assignedCrew;
    private float crewEfficiency;
    
    public CannonComponent(String name, Vector3f position, CannonType cannonType) {
        super(ComponentType.CANNON, name, position, 
              cannonType.getBaseHealth(), cannonType.getBaseMass());
        
        this.cannonType = cannonType;
        this.range = cannonType.getRange();
        this.damage = cannonType.getDamage();
        this.accuracy = cannonType.getAccuracy();
        this.reloadTime = cannonType.getReloadTime();
        
        this.currentReloadTime = 0.0f;
        this.isLoaded = false;
        this.isFiring = false;
        this.loadedAmmo = null;
        
        this.aimDirection = new Vector3f(1, 0, 0);
        this.elevation = 0.0f;
        this.traverse = 0.0f;
        this.maxTraverse = cannonType.getMaxTraverse();
        this.maxElevation = cannonType.getMaxElevation();
        
        // Initialize ammunition
        this.maxAmmoCapacity = cannonType.getAmmoCapacity();
        this.cannonBalls = maxAmmoCapacity / 2;
        this.chainShot = maxAmmoCapacity / 6;
        this.grapeShot = maxAmmoCapacity / 6;
        this.explosiveShot = maxAmmoCapacity / 12;
        
        this.barrelWear = 0.0f;
        this.overheating = 0.0f;
        this.isMisfired = false;
        this.consecutiveFires = 0;
        
        this.requiredCrew = cannonType.getRequiredCrew();
        this.assignedCrew = requiredCrew;
        this.crewEfficiency = 1.0f;
    }
    
    @Override
    protected void updateComponent(float deltaTime) {
        // Update reload progress
        updateReload(deltaTime);
        
        // Update overheating
        updateOverheating(deltaTime);
        
        // Update crew efficiency
        updateCrewEfficiency();
        
        // Check for maintenance needs
        checkMaintenance();
    }
    
    @Override
    public void takeDamage(float damage, DamageType damageType) {
        super.takeDamage(damage, damageType);
        
        // Cannon-specific damage effects
        switch (damageType) {
            case CANNON_BALL:
                // Direct hits can damage the barrel or carriage
                barrelWear += damage * 0.3f;
                if (damage > 50.0f) {
                    // Heavy damage might cause misfire
                    if (Math.random() < 0.2f) {
                        isMisfired = true;
                        logger.warn("Cannon {} accidentally discharged due to fire!", name);
                    }
                }
                break;
                
            case FIRE:
                // Fire can ignite gunpowder
                if (isLoaded && Math.random() < 0.3f) {
                     // Accidental discharge
                     logger.warn("Cannon {} accidentally discharged due to fire!", name);
                     fire(aimDirection);
                 }
                break;
                
            case EXPLOSION:
                // Explosions can damage ammunition
                int ammoLoss = (int)(damage * 0.1f);
                reduceAmmo(ammoLoss);
                break;
        }
    }
    
    @Override
    protected float getDamageResistance(DamageType damageType) {
        switch (damageType) {
            case CANNON_BALL:
                return cannonType == CannonType.HEAVY_CANNON ? 0.6f : 0.4f;
            case FIRE:
                return 0.3f; // Metal provides some fire resistance
            case CORROSION:
                return 0.2f; // Iron cannons are susceptible to corrosion
            case LIGHTNING:
                return 0.1f; // Metal conducts electricity
            default:
                return 0.3f;
        }
    }
    
    @Override
    protected void onDestroyedEffect() {
        // Destroyed cannon might explode
        if (getTotalAmmo() > 0 && Math.random() < 0.4f) {
            logger.warn("Cannon {} exploded when destroyed!", name);
            createExplosionEffect();
        }
        
        // Clear all ammunition
        cannonBalls = 0;
        chainShot = 0;
        grapeShot = 0;
        explosiveShot = 0;
        isLoaded = false;
        loadedAmmo = null;
    }
    
    /**
     * Creates an explosion effect that damages nearby components on the same ship.
     * The explosion radius and damage are based on the amount of ammunition stored.
     */
    private void createExplosionEffect() {
        // Calculate explosion parameters based on stored ammunition
        int totalAmmo = getTotalAmmo();
        float explosionRadius = calculateExplosionRadius(totalAmmo);
        float explosionDamage = calculateExplosionDamage(totalAmmo);
        
        logger.info("Cannon {} explosion: radius={}, damage={}, ammo={}", 
                   name, explosionRadius, explosionDamage, totalAmmo);
        
        // Get the ship this cannon belongs to
        // Note: This requires access to the parent ship, which should be set during component initialization
        if (parentShip != null) {
            damageNearbyComponents(parentShip, explosionRadius, explosionDamage);
        } else {
            logger.warn("Cannot create explosion effect - cannon {} has no parent ship reference", name);
        }
    }
    
    /**
     * Calculates explosion radius based on ammunition count and type.
     */
    private float calculateExplosionRadius(int totalAmmo) {
        float baseRadius = 5.0f; // Base explosion radius in meters
        
        // Increase radius based on total ammunition
        float ammoMultiplier = 1.0f + (totalAmmo / 50.0f); // +1 radius per 50 rounds
        
        // Explosive shot increases radius significantly
        float explosiveMultiplier = 1.0f + (explosiveShot * 0.5f);
        
        // Cannon type affects explosion size
        float cannonMultiplier = switch (cannonType) {
            case LIGHT_CANNON, SWIVEL_GUN -> 0.8f;
            case MEDIUM_CANNON, CARRONADE -> 1.0f;
            case HEAVY_CANNON -> 1.3f;
        };
        
        return baseRadius * ammoMultiplier * explosiveMultiplier * cannonMultiplier;
    }
    
    /**
     * Calculates explosion damage based on ammunition count and type.
     */
    private float calculateExplosionDamage(int totalAmmo) {
        float baseDamage = damage * 0.8f; // Base explosion damage relative to cannon damage
        
        // Increase damage based on ammunition
        float ammoMultiplier = 1.0f + (totalAmmo / 30.0f); // +1 damage multiplier per 30 rounds
        
        // Different ammunition types contribute differently to explosion damage
        float explosiveDamage = explosiveShot * 15.0f; // Explosive shot adds significant damage
        float cannonBallDamage = cannonBalls * 3.0f;
        float chainShotDamage = chainShot * 2.0f;
        float grapeShotDamage = grapeShot * 1.5f;
        
        float totalAmmoDamage = explosiveDamage + cannonBallDamage + chainShotDamage + grapeShotDamage;
        
        return baseDamage * ammoMultiplier + totalAmmoDamage;
    }
    
    /**
     * Damages all components within the explosion radius on the same ship.
     */
    private void damageNearbyComponents(com.odyssey.ship.Ship ship, float explosionRadius, float explosionDamage) {
        Vector3f explosionCenter = new Vector3f(position);
        int componentsHit = 0;
        
        // Get all components from the ship
        java.util.Map<com.odyssey.ship.ComponentType, java.util.List<com.odyssey.ship.ShipComponent>> allComponents = 
            ship.getComponents();
        
        for (java.util.List<com.odyssey.ship.ShipComponent> componentList : allComponents.values()) {
            for (com.odyssey.ship.ShipComponent component : componentList) {
                // Skip self
                if (component == this) continue;
                
                // Calculate distance to component
                float distance = explosionCenter.distance(component.getPosition());
                
                // Check if component is within explosion radius
                if (distance <= explosionRadius) {
                    // Calculate damage falloff based on distance
                    float damageMultiplier = calculateDamageFalloff(distance, explosionRadius);
                    float actualDamage = explosionDamage * damageMultiplier;
                    
                    // Apply explosion damage
                    component.takeDamage(actualDamage, DamageType.EXPLOSION);
                    componentsHit++;
                    
                    logger.debug("Explosion hit {} at distance {} for {} damage", 
                               component.getName(), distance, actualDamage);
                    
                    // Special effects for different component types
                    applySpecialExplosionEffects(component, actualDamage);
                }
            }
        }
        
        logger.info("Cannon explosion affected {} nearby components", componentsHit);
    }
    
    /**
     * Calculates damage falloff based on distance from explosion center.
     * Uses inverse square law with minimum damage threshold.
     */
    private float calculateDamageFalloff(float distance, float maxRadius) {
        if (distance <= 0) return 1.0f; // Full damage at center
        
        // Linear falloff from center to edge
        float falloff = 1.0f - (distance / maxRadius);
        
        // Ensure minimum damage of 20% at the edge
        return Math.max(0.2f, falloff);
    }
    
    /**
     * Applies special explosion effects to different component types.
     */
    private void applySpecialExplosionEffects(com.odyssey.ship.ShipComponent component, float damage) {
        // Different components react differently to explosions
        if (component instanceof CannonComponent otherCannon) {
            // Other cannons might chain explode if heavily damaged
            if (damage > otherCannon.getMaxHealth() * 0.6f && Math.random() < 0.15f) {
                logger.warn("Chain explosion triggered in cannon {}", otherCannon.getName());
                // The other cannon's onDestroyedEffect will handle its own explosion
            }
        } else if (component.getType() == com.odyssey.ship.ComponentType.ENGINE) {
            // Engines might catch fire from explosions
            if (damage > 30.0f && Math.random() < 0.25f) {
                logger.warn("Engine {} caught fire from explosion", component.getName());
                // Apply additional fire damage over time
                component.takeDamage(damage * 0.3f, DamageType.FIRE);
            }
        } else if (component.getType() == com.odyssey.ship.ComponentType.SAIL) {
            // Sails are vulnerable to fire and shrapnel
            if (Math.random() < 0.4f) {
                logger.info("Sail {} damaged by explosion shrapnel", component.getName());
                component.takeDamage(damage * 0.5f, DamageType.FIRE);
            }
        }
    }
    
    @Override
    protected void applySpecificUpgradeBonus() {
        // Upgrades improve accuracy, reduce reload time, and increase durability
        // These bonuses are applied in the respective calculation methods
        
        // Reduce barrel wear
        barrelWear *= 0.9f;
        
        logger.info("Cannon {} upgraded to level {}", name, upgradeLevel);
    }
    
    /**
     * Loads the cannon with specified ammunition type
     */
    public boolean loadCannon(AmmoType ammoType) {
        if (isLoaded || isFiring || currentReloadTime > 0 || isMisfired) {
            return false;
        }
        
        // Check if we have the ammunition
         if (!hasAmmo(ammoType)) {
             logger.warn("No {} ammunition available for cannon {}", ammoType.getName(), name);
             return false;
         }
        
        // Consume ammunition
        consumeAmmo(ammoType);
        
        // Load the cannon
        isLoaded = true;
        loadedAmmo = ammoType;
        currentReloadTime = getEffectiveReloadTime();
        
        logger.info("Cannon {} loaded with {}", name, ammoType.getName());
        return true;
    }
    
    /**
     * Fires the cannon in the specified direction
     */
    public boolean fire(Vector3f targetDirection) {
        if (!canFire()) {
            return false;
        }
        
        // Set firing state
        isFiring = true;
        isLoaded = false;
        consecutiveFires++;
        
        // Add barrel wear and heat
        barrelWear += 1.0f + (upgradeLevel * -0.1f); // Upgrades reduce wear
        overheating += 10.0f;
        
        // Calculate firing effectiveness
        float effectiveness = calculateFiringEffectiveness();
        float actualDamage = damage * loadedAmmo.getDamageMultiplier() * effectiveness;
        float actualRange = range * loadedAmmo.getRangeMultiplier() * effectiveness;
        float actualAccuracy = accuracy * loadedAmmo.getAccuracyMultiplier() * effectiveness;
        
        // Create projectile (this would interface with the combat system)
        createProjectile(targetDirection, actualDamage, actualRange, actualAccuracy);
        
        // Check for misfire
         if (Math.random() < getMisfireChance()) {
             isMisfired = true;
             logger.warn("Cannon {} misfired!", name);
         }
        
        // Reset firing state
        isFiring = false;
        loadedAmmo = null;
        currentReloadTime = getEffectiveReloadTime();
        
        logger.info("Cannon {} fired! Damage: {}, Range: {}, Accuracy: {}", 
                     name, actualDamage, actualRange, actualAccuracy);
        
        return true;
    }
    
    /**
     * Aims the cannon at a target position
     */
    public boolean aimAt(Vector3f targetPosition) {
        if (isFiring || isMisfired) return false;
        
        // Calculate required angles
        Vector3f direction = new Vector3f(targetPosition).sub(position);
        float distance = direction.length();
        direction.normalize();
        
        // Calculate elevation angle
        float requiredElevation = (float) Math.toDegrees(Math.asin(direction.y));
        
        // Calculate traverse angle
        float requiredTraverse = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
        
        // Check if angles are within limits
        if (Math.abs(requiredElevation) > maxElevation || 
            Math.abs(requiredTraverse) > maxTraverse) {
            return false; // Target out of range
        }
        
        // Set aim angles
        elevation = requiredElevation;
        traverse = requiredTraverse;
        aimDirection.set(direction);
        
        return true;
    }
    
    /**
     * Clears a misfired cannon
     */
    public boolean clearMisfire() {
        if (!isMisfired) return false;
        
        // Requires crew and time
        if (assignedCrew < requiredCrew) return false;
        
        isMisfired = false;
        currentReloadTime = reloadTime * 0.5f; // Takes time to clear
        
        logger.info("Cannon {} misfire cleared", name);
        return true;
    }
    
    /**
     * Performs maintenance on the cannon
     */
    public void performMaintenance(float maintenanceAmount) {
        barrelWear = Math.max(0, barrelWear - maintenanceAmount);
        overheating = Math.max(0, overheating - maintenanceAmount * 2.0f);
        
        logger.debug("Cannon {} maintenance performed: wear={}, heat={}", 
                     name, barrelWear, overheating);
    }
    
    /**
     * Adds ammunition to the cannon's storage
     */
    public void addAmmo(AmmoType ammoType, int amount) {
        int currentTotal = getTotalAmmo();
        if (currentTotal >= maxAmmoCapacity) return;
        
        int spaceAvailable = maxAmmoCapacity - currentTotal;
        int actualAmount = Math.min(amount, spaceAvailable);
        
        switch (ammoType) {
            case CANNON_BALL:
                cannonBalls += actualAmount;
                break;
            case CHAIN_SHOT:
                chainShot += actualAmount;
                break;
            case GRAPE_SHOT:
                grapeShot += actualAmount;
                break;
            case EXPLOSIVE_SHOT:
                explosiveShot += actualAmount;
                break;
        }
        
        logger.info("Added {} {} to cannon {}", actualAmount, ammoType.getName(), name);
    }
    
    /**
     * Assigns crew to this cannon
     */
    public void assignCrew(int crewCount) {
        assignedCrew = Math.max(0, Math.min(crewCount, requiredCrew * 2)); // Can over-crew for efficiency
        updateCrewEfficiency();
    }
    
    private boolean canFire() {
        return isLoaded && !isFiring && !isMisfired && currentReloadTime <= 0 && 
               assignedCrew >= requiredCrew && !isDestroyed && isActive;
    }
    
    private void updateReload(float deltaTime) {
        if (currentReloadTime > 0) {
            float reloadSpeed = crewEfficiency * (1.0f + upgradeLevel * 0.1f);
            currentReloadTime -= deltaTime * reloadSpeed;
            currentReloadTime = Math.max(0, currentReloadTime);
        }
    }
    
    private void updateOverheating(float deltaTime) {
        if (overheating > 0) {
            // Natural cooling
            float coolingRate = 2.0f * deltaTime;
            overheating = Math.max(0, overheating - coolingRate);
        }
        
        // Reset consecutive fires if cooled down
        if (overheating < 5.0f) {
            consecutiveFires = 0;
        }
    }
    
    private void updateCrewEfficiency() {
        if (assignedCrew >= requiredCrew) {
            // Full crew or over-crewed
            crewEfficiency = Math.min(1.5f, (float) assignedCrew / requiredCrew);
        } else {
            // Under-crewed penalty
            crewEfficiency = (float) assignedCrew / requiredCrew * 0.7f;
        }
    }
    
    private void checkMaintenance() {
        if (barrelWear > maxHealth * 0.5f) {
             logger.warn("Warning: Cannon {} needs maintenance - high barrel wear", name);
         }
         
         if (overheating > 50.0f) {
             logger.warn("Warning: Cannon {} overheating - reduce firing rate", name);
         }
    }
    
    private float calculateFiringEffectiveness() {
        float effectiveness = getEffectiveness(); // Base component effectiveness
        
        // Crew efficiency
        effectiveness *= crewEfficiency;
        
        // Barrel wear penalty
        float wearPenalty = Math.min(0.5f, barrelWear / maxHealth);
        effectiveness *= (1.0f - wearPenalty);
        
        // Overheating penalty
        float heatPenalty = Math.min(0.3f, overheating / 100.0f);
        effectiveness *= (1.0f - heatPenalty);
        
        // Upgrade bonus
        effectiveness *= (1.0f + upgradeLevel * 0.1f);
        
        return Math.max(0.1f, effectiveness);
    }
    
    private float getEffectiveReloadTime() {
        float effectiveTime = reloadTime;
        
        // Crew efficiency affects reload time
        effectiveTime /= crewEfficiency;
        
        // Upgrade bonus
        effectiveTime *= (1.0f - upgradeLevel * 0.1f);
        
        // Overheating penalty
        effectiveTime *= (1.0f + overheating / 100.0f);
        
        return Math.max(1.0f, effectiveTime);
    }
    
    private float getMisfireChance() {
        float baseChance = 0.02f; // 2% base chance
        
        // Barrel wear increases misfire chance
        baseChance += barrelWear / maxHealth * 0.1f;
        
        // Overheating increases misfire chance
        baseChance += overheating / 100.0f * 0.05f;
        
        // Consecutive fires increase chance
        baseChance += consecutiveFires * 0.01f;
        
        // Upgrades reduce misfire chance
        baseChance *= (1.0f - upgradeLevel * 0.1f);
        
        return Math.min(0.2f, baseChance); // Max 20% chance
    }
    
    private void createProjectile(Vector3f direction, float damage, float range, float accuracy) {
        // Interface with combat system to create actual projectile
        // This method is called by the fire() method and should be connected to CombatSystem
        logger.debug("Projectile fired from cannon {} - Damage: {}, Range: {}, Accuracy: {}", 
                     name, damage, range, accuracy);
        
        // Note: The actual projectile creation is handled by CombatSystem.fireProjectile()
        // This method serves as a placeholder for the firing mechanics
    }
    
    private boolean hasAmmo(AmmoType ammoType) {
        switch (ammoType) {
            case CANNON_BALL: return cannonBalls > 0;
            case CHAIN_SHOT: return chainShot > 0;
            case GRAPE_SHOT: return grapeShot > 0;
            case EXPLOSIVE_SHOT: return explosiveShot > 0;
            default: return false;
        }
    }
    
    private void consumeAmmo(AmmoType ammoType) {
        switch (ammoType) {
            case CANNON_BALL: cannonBalls--; break;
            case CHAIN_SHOT: chainShot--; break;
            case GRAPE_SHOT: grapeShot--; break;
            case EXPLOSIVE_SHOT: explosiveShot--; break;
        }
    }
    
    private void reduceAmmo(int amount) {
        int remaining = amount;
        
        // Remove ammo in order of abundance
        int cannonBallLoss = Math.min(remaining, cannonBalls);
        cannonBalls -= cannonBallLoss;
        remaining -= cannonBallLoss;
        
        if (remaining > 0) {
            int chainShotLoss = Math.min(remaining, chainShot);
            chainShot -= chainShotLoss;
            remaining -= chainShotLoss;
        }
        
        if (remaining > 0) {
            int grapeShotLoss = Math.min(remaining, grapeShot);
            grapeShot -= grapeShotLoss;
            remaining -= grapeShotLoss;
        }
        
        if (remaining > 0) {
            int explosiveShotLoss = Math.min(remaining, explosiveShot);
            explosiveShot -= explosiveShotLoss;
        }
    }
    
    private int getTotalAmmo() {
        return cannonBalls + chainShot + grapeShot + explosiveShot;
    }
    
    // Getters
    public CannonType getCannonType() { return cannonType; }
    public float getRange() { return range; }
    public float getDamage() { return damage; }
    public float getAccuracy() { return accuracy; }
    public float getReloadTime() { return reloadTime; }
    public float getCurrentReloadTime() { return currentReloadTime; }
    public boolean isLoaded() { return isLoaded; }
    public boolean isFiring() { return isFiring; }
    public AmmoType getLoadedAmmo() { return loadedAmmo; }
    public Vector3f getAimDirection() { return new Vector3f(aimDirection); }
    public float getElevation() { return elevation; }
    public float getTraverse() { return traverse; }
    public float getMaxTraverse() { return maxTraverse; }
    public float getMaxElevation() { return maxElevation; }
    public int getCannonBalls() { return cannonBalls; }
    public int getChainShot() { return chainShot; }
    public int getGrapeShot() { return grapeShot; }
    public int getExplosiveShot() { return explosiveShot; }
    public int getMaxAmmoCapacity() { return maxAmmoCapacity; }
    public float getBarrelWear() { return barrelWear; }
    public float getOverheating() { return overheating; }
    public boolean isMisfired() { return isMisfired; }
    public int getRequiredCrew() { return requiredCrew; }
    public int getAssignedCrew() { return assignedCrew; }
    public float getCrewEfficiency() { return crewEfficiency; }
    
    /**
     * Cannon types with different characteristics
     */
    public enum CannonType {
        LIGHT_CANNON("Light Cannon", 200.0f, 150.0f, 300.0f, 50.0f, 0.8f, 8.0f, 60.0f, 30.0f, 50, 2),
        MEDIUM_CANNON("Medium Cannon", 300.0f, 200.0f, 400.0f, 75.0f, 0.7f, 12.0f, 45.0f, 20.0f, 40, 3),
        HEAVY_CANNON("Heavy Cannon", 500.0f, 300.0f, 500.0f, 100.0f, 0.6f, 18.0f, 30.0f, 15.0f, 30, 4),
        SWIVEL_GUN("Swivel Gun", 100.0f, 80.0f, 200.0f, 30.0f, 0.9f, 4.0f, 180.0f, 45.0f, 20, 1),
        CARRONADE("Carronade", 250.0f, 180.0f, 250.0f, 120.0f, 0.5f, 10.0f, 30.0f, 25.0f, 25, 2);
        
        private final String name;
        private final float baseHealth;
        private final float baseMass;
        private final float range;
        private final float damage;
        private final float accuracy;
        private final float reloadTime;
        private final float maxTraverse;
        private final float maxElevation;
        private final int ammoCapacity;
        private final int requiredCrew;
        
        CannonType(String name, float baseHealth, float baseMass, float range, float damage,
                  float accuracy, float reloadTime, float maxTraverse, float maxElevation,
                  int ammoCapacity, int requiredCrew) {
            this.name = name;
            this.baseHealth = baseHealth;
            this.baseMass = baseMass;
            this.range = range;
            this.damage = damage;
            this.accuracy = accuracy;
            this.reloadTime = reloadTime;
            this.maxTraverse = maxTraverse;
            this.maxElevation = maxElevation;
            this.ammoCapacity = ammoCapacity;
            this.requiredCrew = requiredCrew;
        }
        
        public String getName() { return name; }
        public float getBaseHealth() { return baseHealth; }
        public float getBaseMass() { return baseMass; }
        public float getRange() { return range; }
        public float getDamage() { return damage; }
        public float getAccuracy() { return accuracy; }
        public float getReloadTime() { return reloadTime; }
        public float getMaxTraverse() { return maxTraverse; }
        public float getMaxElevation() { return maxElevation; }
        public int getAmmoCapacity() { return ammoCapacity; }
        public int getRequiredCrew() { return requiredCrew; }
    }
    
    /**
     * Ammunition types with different effects
     */
    public enum AmmoType {
        CANNON_BALL("Cannon Ball", 1.0f, 1.0f, 1.0f, DamageType.CANNON_BALL),
        CHAIN_SHOT("Chain Shot", 0.8f, 0.9f, 1.2f, DamageType.CANNON_BALL), // Good against sails/rigging
        GRAPE_SHOT("Grape Shot", 1.5f, 0.6f, 0.8f, DamageType.CANNON_BALL), // Good against crew
        EXPLOSIVE_SHOT("Explosive Shot", 2.0f, 0.8f, 0.7f, DamageType.FIRE); // Explosive damage
        
        private final String name;
        private final float damageMultiplier;
        private final float rangeMultiplier;
        private final float accuracyMultiplier;
        private final DamageType damageType;
        
        AmmoType(String name, float damageMultiplier, float rangeMultiplier, 
                float accuracyMultiplier, DamageType damageType) {
            this.name = name;
            this.damageMultiplier = damageMultiplier;
            this.rangeMultiplier = rangeMultiplier;
            this.accuracyMultiplier = accuracyMultiplier;
            this.damageType = damageType;
        }
        
        public String getName() { return name; }
        public float getDamageMultiplier() { return damageMultiplier; }
        public float getRangeMultiplier() { return rangeMultiplier; }
        public float getAccuracyMultiplier() { return accuracyMultiplier; }
        public DamageType getDamageType() { return damageType; }
    }
}