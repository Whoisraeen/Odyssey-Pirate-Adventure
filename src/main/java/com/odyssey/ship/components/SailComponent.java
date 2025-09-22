package com.odyssey.ship.components;

import com.odyssey.ship.ShipComponent;
import com.odyssey.ship.ComponentType;
import com.odyssey.ship.DamageType;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Sail component - provides wind-powered propulsion for ships.
 * Handles wind interaction, sail configuration, and propulsion calculations.
 */
public class SailComponent extends ShipComponent {
    
    // Sail properties
    private final SailType sailType;
    private final float sailArea;
    private float windEfficiency;
    private float currentDeployment; // 0.0 to 1.0 (furled to fully deployed)
    private float targetDeployment;
    private float deploymentSpeed;
    
    // Wind interaction
    private Vector3f windDirection;
    private float windSpeed;
    private float windCatchAngle; // Angle between sail and wind
    private float thrustGenerated;
    
    // Sail condition
    private float tearDamage; // Accumulated tears and holes
    private boolean isReefed; // Reduced sail area for storms
    private float reefFactor; // How much sail is reefed (0.0 to 0.8)
    
    // Mast connection
    private int mastId; // ID of the mast this sail is attached to
    private Vector3f mastOffset; // Offset from mast position
    
    public SailComponent(String name, Vector3f position, SailType sailType, int mastId) {
        super(ComponentType.SAIL, name, position, 
              sailType.getBaseHealth(), sailType.getBaseMass());
        
        this.sailType = sailType;
        this.sailArea = sailType.getSailArea();
        this.mastId = mastId;
        this.mastOffset = new Vector3f();
        
        this.windEfficiency = sailType.getWindEfficiency();
        this.currentDeployment = 0.0f;
        this.targetDeployment = 1.0f;
        this.deploymentSpeed = sailType.getDeploymentSpeed();
        
        this.windDirection = new Vector3f(1, 0, 0);
        this.windSpeed = 0.0f;
        this.windCatchAngle = 0.0f;
        this.thrustGenerated = 0.0f;
        
        this.tearDamage = 0.0f;
        this.isReefed = false;
        this.reefFactor = 0.0f;
    }
    
    @Override
    protected void updateComponent(float deltaTime) {
        // Update sail deployment
        updateDeployment(deltaTime);
        
        // Calculate wind interaction
        calculateWindInteraction();
        
        // Update thrust generation
        calculateThrust();
        
        // Check for sail stress and damage
        checkSailStress(deltaTime);
    }
    
    @Override
    public void takeDamage(float damage, DamageType damageType) {
        super.takeDamage(damage, damageType);
        
        // Sail-specific damage effects
        switch (damageType) {
            case CANNON_BALL:
                // Cannon balls create holes in sails
                tearDamage += damage * 0.3f;
                logger.warn(Logger.WORLD, "Sail {} has new tears from cannon fire", name);
                break;
                
            case STORM:
            case LIGHTNING:
                // Weather damage can tear sails
                tearDamage += damage * 0.3f;
                if (currentDeployment > 0.5f) {
                    // Force reef sails in storm
                    setReefed(true, 0.6f);
                }
                break;
                
            case FIRE:
                // Fire spreads quickly on sails
                tearDamage += damage * 2.0f;
                logger.warn(Logger.WORLD, "Sail {} is burning!", name);
                break;
        }
        
        // Update wind efficiency based on damage
        updateWindEfficiency();
    }
    
    @Override
    protected float getDamageResistance(DamageType damageType) {
        switch (damageType) {
            case CANNON_BALL:
                return 0.1f; // Sails offer little resistance to cannon balls
            case FIRE:
                return sailType == SailType.MAGICAL_SILK ? 0.6f : 0.0f;
            case STORM:
                return sailType.getStormResistance();
            case LIGHTNING:
                return 0.2f; // Some resistance due to rigging
            default:
                return 0.1f;
        }
    }
    
    @Override
    protected void onDestroyedEffect() {
        // Destroyed sail provides no thrust
        thrustGenerated = 0.0f;
        currentDeployment = 0.0f;
        targetDeployment = 0.0f;
        
        logger.error(Logger.WORLD, "Sail {} destroyed - no longer providing thrust", name);
    }
    
    @Override
    protected void applySpecificUpgradeBonus() {
        // Upgrade improves wind efficiency and reduces deployment time
        windEfficiency *= 1.1f;
        deploymentSpeed *= 1.2f;
        
        // Reduce tear damage
        tearDamage *= 0.8f;
        
        logger.info(Logger.WORLD, "Sail {} upgraded: efficiency={}, deployment speed={}", 
                   name, windEfficiency, deploymentSpeed);
    }
    
    /**
     * Sets the target deployment level (0.0 to 1.0)
     */
    public void setDeployment(float deployment) {
        targetDeployment = Math.max(0.0f, Math.min(1.0f, deployment));
    }
    
    /**
     * Fully deploys the sail
     */
    public void deploySail() {
        setDeployment(1.0f);
    }
    
    /**
     * Furls (retracts) the sail
     */
    public void furlSail() {
        setDeployment(0.0f);
    }
    
    /**
     * Reefs the sail (reduces area for storm conditions)
     */
    public void setReefed(boolean reefed, float reefAmount) {
        this.isReefed = reefed;
        this.reefFactor = reefed ? Math.max(0.0f, Math.min(0.8f, reefAmount)) : 0.0f;
        
        if (reefed) {
            logger.info(Logger.WORLD, "Sail {} reefed by {}%", name, (int)(reefFactor * 100));
        } else {
            logger.info(Logger.WORLD, "Sail {} unreefed", name);
        }
    }
    
    /**
     * Updates wind conditions for this sail
     */
    public void updateWind(Vector3f windDir, float windSpd) {
        this.windDirection.set(windDir);
        this.windSpeed = windSpd;
    }
    
    /**
     * Sets the sail angle relative to the wind
     */
    public void setSailAngle(float angle) {
        this.windCatchAngle = angle;
    }
    
    /**
     * Repairs tears in the sail
     */
    public void repairTears(float repairAmount) {
        tearDamage = Math.max(0, tearDamage - repairAmount);
        updateWindEfficiency();
        
        logger.info(Logger.WORLD, "Sail {} tears repaired: remaining damage={}", name, tearDamage);
    }
    
    /**
     * Gets the current sail deployment amount (0.0 to 1.0)
     */
    public float getSailAmount() {
        return currentDeployment;
    }
    
    /**
     * Gets the effective sail area considering deployment and damage
     */
    public float getEffectiveSailArea() {
        if (isDestroyed || !isActive) return 0.0f;
        
        float effectiveArea = sailArea * currentDeployment;
        
        // Reduce area due to reefing
        if (isReefed) {
            effectiveArea *= (1.0f - reefFactor);
        }
        
        // Reduce area due to tears
        float tearReduction = Math.min(0.8f, tearDamage / maxHealth);
        effectiveArea *= (1.0f - tearReduction);
        
        return effectiveArea;
    }
    
    /**
     * Gets the current thrust being generated
     */
    public float getThrust() {
        return thrustGenerated;
    }
    
    /**
     * Gets the optimal wind angle for this sail type
     */
    public float getOptimalWindAngle() {
        return sailType.getOptimalWindAngle();
    }
    
    private void updateDeployment(float deltaTime) {
        if (isDestroyed) return;
        
        float deploymentChange = deploymentSpeed * deltaTime;
        
        if (currentDeployment < targetDeployment) {
            currentDeployment = Math.min(targetDeployment, currentDeployment + deploymentChange);
        } else if (currentDeployment > targetDeployment) {
            currentDeployment = Math.max(targetDeployment, currentDeployment - deploymentChange);
        }
    }
    
    private void calculateWindInteraction() {
        if (windSpeed <= 0 || currentDeployment <= 0) {
            thrustGenerated = 0.0f;
            return;
        }
        
        // Calculate wind effectiveness based on angle
        float angleEffectiveness = sailType.getAngleEffectiveness(windCatchAngle);
        
        // Calculate base thrust
        float baseThrust = windSpeed * windSpeed * getEffectiveSailArea() * angleEffectiveness;
        
        // Apply wind efficiency
        thrustGenerated = baseThrust * windEfficiency * getEffectiveness();
    }
    
    private void calculateThrust() {
        // Thrust is already calculated in calculateWindInteraction
        // This method can be used for additional thrust calculations or modifications
        
        // Apply storm penalties
        if (windSpeed > 25.0f && !isReefed) {
            // High winds without reefing can damage sails
            thrustGenerated *= 0.7f; // Reduced efficiency
        }
        
        // Apply upgrade bonuses
        thrustGenerated *= (1.0f + upgradeLevel * 0.1f);
    }
    
    private void checkSailStress(float deltaTime) {
        if (windSpeed > 20.0f && currentDeployment > 0.8f && !isReefed) {
            // High wind stress can cause additional damage
            float stressDamage = (windSpeed - 20.0f) * 0.1f * deltaTime;
            tearDamage += stressDamage;
            
            if (tearDamage > maxHealth * 0.5f) {
                logger.warn(Logger.WORLD, "Warning: Sail {} under severe stress - consider reefing", name);
            }
        }
    }
    
    private void updateWindEfficiency() {
        // Base efficiency from sail type
        windEfficiency = sailType.getWindEfficiency();
        
        // Reduce efficiency due to tears
        float tearPenalty = Math.min(0.7f, tearDamage / maxHealth);
        windEfficiency *= (1.0f - tearPenalty);
        
        // Health penalty
        windEfficiency *= getHealthPercentage();
        
        // Upgrade bonus
        windEfficiency *= (1.0f + upgradeLevel * 0.05f);
    }
    
    // Getters
    public SailType getSailType() { return sailType; }
    public float getSailArea() { return sailArea; }
    public float getWindEfficiency() { return windEfficiency; }
    public float getCurrentDeployment() { return currentDeployment; }
    public float getTargetDeployment() { return targetDeployment; }
    public Vector3f getWindDirection() { return new Vector3f(windDirection); }
    public float getWindSpeed() { return windSpeed; }
    public float getWindCatchAngle() { return windCatchAngle; }
    public float getTearDamage() { return tearDamage; }
    public boolean isReefed() { return isReefed; }
    public float getReefFactor() { return reefFactor; }
    public int getMastId() { return mastId; }
    public Vector3f getMastOffset() { return new Vector3f(mastOffset); }
    
    /**
     * Sail types with different characteristics
     */
    public enum SailType {
        SQUARE_SAIL("Square Sail", 100.0f, 30.0f, 0.8f, 0.5f, 90.0f, 0.3f),
        LATEEN_SAIL("Lateen Sail", 80.0f, 25.0f, 0.9f, 0.7f, 45.0f, 0.4f),
        JIB_SAIL("Jib Sail", 40.0f, 15.0f, 1.0f, 0.8f, 30.0f, 0.5f),
        SPINNAKER("Spinnaker", 120.0f, 20.0f, 1.2f, 0.3f, 180.0f, 0.2f),
        TOPSAIL("Topsail", 60.0f, 20.0f, 0.9f, 0.6f, 60.0f, 0.4f),
        MAGICAL_SILK("Magical Silk", 90.0f, 10.0f, 1.5f, 1.0f, 0.0f, 0.8f); // Can catch wind from any angle
        
        private final String name;
        private final float sailArea;
        private final float baseMass;
        private final float windEfficiency;
        private final float deploymentSpeed;
        private final float optimalWindAngle;
        private final float stormResistance;
        
        SailType(String name, float sailArea, float baseMass, float windEfficiency, 
                float deploymentSpeed, float optimalWindAngle, float stormResistance) {
            this.name = name;
            this.sailArea = sailArea;
            this.baseMass = baseMass;
            this.windEfficiency = windEfficiency;
            this.deploymentSpeed = deploymentSpeed;
            this.optimalWindAngle = optimalWindAngle;
            this.stormResistance = stormResistance;
        }
        
        public String getName() { return name; }
        public float getSailArea() { return sailArea; }
        public float getBaseMass() { return baseMass; }
        public float getBaseHealth() { return sailArea * 2.0f; }
        public float getWindEfficiency() { return windEfficiency; }
        public float getDeploymentSpeed() { return deploymentSpeed; }
        public float getOptimalWindAngle() { return optimalWindAngle; }
        public float getStormResistance() { return stormResistance; }
        
        /**
         * Gets the effectiveness of this sail at a given wind angle
         */
        public float getAngleEffectiveness(float windAngle) {
            if (this == MAGICAL_SILK) {
                return 1.0f; // Magical sails work at any angle
            }
            
            float angleDifference = Math.abs(windAngle - optimalWindAngle);
            
            // Effectiveness drops off as angle deviates from optimal
            if (angleDifference <= 15.0f) {
                return 1.0f; // Full effectiveness within 15 degrees
            } else if (angleDifference <= 45.0f) {
                return 1.0f - (angleDifference - 15.0f) / 30.0f * 0.5f; // 50% to 100%
            } else if (angleDifference <= 90.0f) {
                return 0.5f - (angleDifference - 45.0f) / 45.0f * 0.4f; // 10% to 50%
            } else {
                return 0.1f; // Minimum effectiveness
            }
        }
    }
}