package com.odyssey.ship.components;

import com.odyssey.ship.ShipComponent;
import com.odyssey.ship.ComponentType;
import com.odyssey.ship.DamageType;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Rudder component - provides steering control for ships.
 * Handles steering mechanics, turning forces, and maneuverability calculations.
 */
public class RudderComponent extends ShipComponent {
    
    // Rudder properties
    private final RudderType rudderType;
    private final float rudderArea; // Surface area affecting turning force
    private final float maxAngle; // Maximum rudder deflection angle
    private final float responseTime; // Time to reach target angle
    
    // Steering state
    private float currentAngle; // Current rudder angle (-maxAngle to +maxAngle)
    private float targetAngle; // Desired rudder angle
    private float angularVelocity; // Rate of angle change
    private boolean isResponsive; // Whether rudder responds to input
    
    // Performance characteristics
    private float turningEfficiency; // How effectively rudder creates turning force
    private float dragCoefficient; // Drag created by rudder deflection
    private float stallAngle; // Angle at which rudder becomes less effective
    private float minEffectiveSpeed; // Minimum ship speed for rudder effectiveness
    
    // Rudder condition
    private float structuralDamage; // Damage affecting rudder integrity
    private float fouling; // Barnacles/debris reducing effectiveness
    private boolean isJammed; // Rudder stuck in position
    private float jammedAngle; // Angle rudder is stuck at
    
    // Mechanical systems
    private float steeringChainTension; // Tension in steering mechanism
    private float mechanicalWear; // Wear on steering components
    private boolean hasHydraulicAssist; // Whether rudder has power assistance
    private float hydraulicPressure; // Pressure in hydraulic system
    
    /**
     * Rudder types with different characteristics
     */
    public enum RudderType {
        SIMPLE_RUDDER("Simple Rudder", 1.0f, 35.0f, 2.0f, 0.8f),
        BALANCED_RUDDER("Balanced Rudder", 1.2f, 40.0f, 1.5f, 0.9f),
        SPADE_RUDDER("Spade Rudder", 1.4f, 45.0f, 1.0f, 1.0f),
        TWIN_RUDDER("Twin Rudder", 1.6f, 35.0f, 1.2f, 1.1f);
        
        private final String displayName;
        private final float efficiencyMultiplier;
        private final float maxDeflection;
        private final float responseTime;
        private final float dragMultiplier;
        
        RudderType(String displayName, float efficiencyMultiplier, float maxDeflection, 
                  float responseTime, float dragMultiplier) {
            this.displayName = displayName;
            this.efficiencyMultiplier = efficiencyMultiplier;
            this.maxDeflection = maxDeflection;
            this.responseTime = responseTime;
            this.dragMultiplier = dragMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public float getEfficiencyMultiplier() { return efficiencyMultiplier; }
        public float getMaxDeflection() { return maxDeflection; }
        public float getResponseTime() { return responseTime; }
        public float getDragMultiplier() { return dragMultiplier; }
    }
    
    /**
     * Creates a new rudder component
     */
    public RudderComponent(String name, Vector3f position, RudderType rudderType) {
        super(ComponentType.RUDDER, name, position, 150.0f, 75.0f);
        
        this.rudderType = rudderType;
        this.rudderArea = calculateRudderArea();
        this.maxAngle = rudderType.getMaxDeflection();
        this.responseTime = rudderType.getResponseTime();
        
        // Initialize steering state
        this.currentAngle = 0.0f;
        this.targetAngle = 0.0f;
        this.angularVelocity = 0.0f;
        this.isResponsive = true;
        
        // Initialize performance characteristics
        this.turningEfficiency = rudderType.getEfficiencyMultiplier();
        this.dragCoefficient = rudderType.getDragMultiplier();
        this.stallAngle = maxAngle * 0.8f; // Stall at 80% of max angle
        this.minEffectiveSpeed = 0.5f; // Minimum speed for effectiveness
        
        // Initialize condition
        this.structuralDamage = 0.0f;
        this.fouling = 0.0f;
        this.isJammed = false;
        this.jammedAngle = 0.0f;
        
        // Initialize mechanical systems
        this.steeringChainTension = 1.0f;
        this.mechanicalWear = 0.0f;
        this.hasHydraulicAssist = false;
        this.hydraulicPressure = 0.0f;
        
        logger.debug(Logger.WORLD, "Created {} rudder component: {}", rudderType.getDisplayName(), name);
    }
    
    /**
     * Updates rudder state and steering response
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        if (!isActive || isDestroyed) {
            return;
        }
        
        // Update rudder angle towards target
        updateRudderAngle(deltaTime);
        
        // Update mechanical wear
        updateMechanicalWear(deltaTime);
        
        // Update fouling accumulation
        updateFouling(deltaTime);
        
        // Check for jamming conditions
        checkForJamming();
        
        // Update efficiency based on condition
        updateEfficiency();
    }
    
    /**
     * Sets the target rudder angle
     */
    public void setRudderAngle(float angle) {
        if (!isResponsive || isJammed) {
            return;
        }
        
        // Clamp angle to valid range
        this.targetAngle = Math.max(-maxAngle, Math.min(maxAngle, angle));
        
        logger.debug(Logger.WORLD, "Rudder {} target angle set to: {}", name, targetAngle);
    }
    
    /**
     * Gets the current rudder angle
     */
    public float getCurrentAngle() {
        return isJammed ? jammedAngle : currentAngle;
    }
    
    /**
     * Calculates turning force generated by rudder
     */
    public Vector3f calculateTurningForce(float shipSpeed, Vector3f shipVelocity) {
        Vector3f turningForce = new Vector3f();
        
        if (!isActive || isDestroyed || shipSpeed < minEffectiveSpeed) {
            return turningForce;
        }
        
        float effectiveAngle = getCurrentAngle();
        
        // Reduce effectiveness at high angles (stall)
        float angleEfficiency = 1.0f;
        if (Math.abs(effectiveAngle) > stallAngle) {
            angleEfficiency = 1.0f - ((Math.abs(effectiveAngle) - stallAngle) / (maxAngle - stallAngle)) * 0.5f;
        }
        
        // Calculate force magnitude based on speed, angle, and rudder area
        float forceMagnitude = shipSpeed * shipSpeed * Math.abs(effectiveAngle) * rudderArea * 
                              turningEfficiency * angleEfficiency * efficiency;
        
        // Force is perpendicular to ship direction
        Vector3f lateral = new Vector3f(1, 0, 0); // Right direction
        lateral.mul(forceMagnitude * Math.signum(effectiveAngle));
        
        return lateral;
    }
    
    /**
     * Calculates drag force from rudder deflection
     */
    public Vector3f calculateDragForce(float shipSpeed) {
        Vector3f dragForce = new Vector3f();
        
        if (!isActive || isDestroyed) {
            return dragForce;
        }
        
        float effectiveAngle = getCurrentAngle();
        
        // Drag increases with angle deflection
        float dragMagnitude = shipSpeed * shipSpeed * Math.abs(effectiveAngle) * 
                             dragCoefficient * rudderArea * 0.1f;
        
        // Drag opposes forward motion
        Vector3f forward = new Vector3f(0, 0, -1); // Backward direction
        forward.mul(dragMagnitude);
        
        return forward;
    }
    
    /**
     * Updates the rudder component each frame
     */
    @Override
    protected void updateComponent(float deltaTime) {
        // Update rudder angle towards target
        updateRudderAngle(deltaTime);
        
        // Update mechanical wear from usage
        updateMechanicalWear(deltaTime);
        
        // Update fouling accumulation
        updateFouling(deltaTime);
        
        // Check for jamming conditions
        checkForJamming();
        
        // Update hydraulic system if present
        if (hasHydraulicAssist) {
            // Simple hydraulic pressure maintenance
            hydraulicPressure = Math.max(0.8f, hydraulicPressure - (mechanicalWear * 0.1f));
        }
        
        // Update component efficiency
        updateEfficiency();
    }

    /**
     * Updates rudder angle towards target angle
     */
    private void updateRudderAngle(float deltaTime) {
        if (isJammed) {
            currentAngle = jammedAngle;
            return;
        }
        
        float angleDifference = targetAngle - currentAngle;
        
        if (Math.abs(angleDifference) > 0.1f) {
            // Calculate angular velocity based on response time
            float maxAngularVelocity = maxAngle / responseTime;
            
            // Apply mechanical wear to response time
            float wearFactor = 1.0f + mechanicalWear * 0.5f;
            maxAngularVelocity /= wearFactor;
            
            // Update angular velocity
            angularVelocity = Math.signum(angleDifference) * 
                             Math.min(maxAngularVelocity, Math.abs(angleDifference) / deltaTime);
            
            // Update current angle
            currentAngle += angularVelocity * deltaTime;
            
            // Clamp to valid range
            currentAngle = Math.max(-maxAngle, Math.min(maxAngle, currentAngle));
        } else {
            angularVelocity = 0.0f;
        }
    }
    
    /**
     * Updates mechanical wear from usage
     */
    private void updateMechanicalWear(float deltaTime) {
        if (Math.abs(angularVelocity) > 0.1f) {
            // Wear increases with usage
            float wearRate = Math.abs(angularVelocity) * 0.0001f;
            mechanicalWear += wearRate * deltaTime;
            mechanicalWear = Math.min(mechanicalWear, 1.0f);
            
            // Update steering chain tension
            steeringChainTension = 1.0f - mechanicalWear * 0.3f;
        }
    }
    
    /**
     * Updates fouling accumulation
     */
    private void updateFouling(float deltaTime) {
        // Fouling accumulates slowly over time
        fouling += 0.00001f * deltaTime;
        fouling = Math.min(fouling, 0.5f); // Max 50% fouling
    }
    
    /**
     * Checks for jamming conditions
     */
    private void checkForJamming() {
        // Jamming can occur from severe damage or extreme wear
        if (structuralDamage > 0.8f || mechanicalWear > 0.9f) {
            if (!isJammed && Math.random() < 0.001f) { // Small chance per update
                isJammed = true;
                jammedAngle = currentAngle;
                logger.warn(Logger.WORLD, "Rudder {} has jammed at angle: {}", name, jammedAngle);
            }
        }
    }
    
    /**
     * Updates component efficiency based on damage and condition
     */
    @Override
    protected void updateEfficiency() {
        // Base efficiency from component damage
        float baseEfficiency = super.getEffectiveness();
        
        // Reduce efficiency from structural damage
        float damageReduction = structuralDamage * 0.3f;
        
        // Reduce efficiency from fouling
        float foulingReduction = fouling * 0.2f;
        
        // Reduce efficiency from mechanical wear
        float wearReduction = mechanicalWear * 0.15f;
        
        // Calculate final efficiency
        efficiency = Math.max(0.1f, baseEfficiency - damageReduction - foulingReduction - wearReduction);
        
        // Jammed rudder has very low efficiency
        if (isJammed) {
            efficiency *= 0.1f;
        }
        
        // Update responsiveness
        isResponsive = efficiency > 0.3f && !isJammed;
        
        Logger.world("Rudder efficiency updated: {}", efficiency);
    }
    
    /**
     * Calculates rudder area based on type
     */
    private float calculateRudderArea() {
        switch (rudderType) {
            case SIMPLE_RUDDER: return 8.0f;
            case BALANCED_RUDDER: return 10.0f;
            case SPADE_RUDDER: return 12.0f;
            case TWIN_RUDDER: return 16.0f;
            default: return 8.0f;
        }
    }
    
    /**
     * Repairs rudder jamming
     */
    public void repairJamming() {
        if (isJammed) {
            isJammed = false;
            jammedAngle = 0.0f;
            logger.info(Logger.WORLD, "Rudder {} jamming repaired", name);
        }
    }
    
    /**
     * Cleans fouling from rudder
     */
    public void cleanFouling() {
        fouling = 0.0f;
        logger.info(Logger.WORLD, "Rudder {} fouling cleaned", name);
    }
    
    /**
     * Applies damage to rudder
     */
    @Override
    public void takeDamage(float damage, DamageType damageType) {
        super.takeDamage(damage, damageType);
        
        // Structural damage affects rudder integrity
        if (damageType == DamageType.COLLISION || damageType == DamageType.EXPLOSION) {
            structuralDamage += damage * 0.01f;
            structuralDamage = Math.min(structuralDamage, 1.0f);
        }
        
        // Mechanical damage affects steering systems
        if (damageType == DamageType.FATIGUE || damageType == DamageType.COLLISION) {
            mechanicalWear += damage * 0.005f;
            mechanicalWear = Math.min(mechanicalWear, 1.0f);
        }
    }
    
    /**
     * Repairs rudder component
     */
    @Override
    public void repair(float repairAmount) {
        super.repair(repairAmount);
        
        // Repair structural damage
        structuralDamage -= repairAmount * 0.01f;
        structuralDamage = Math.max(structuralDamage, 0.0f);
        
        // Repair mechanical wear
        mechanicalWear -= repairAmount * 0.005f;
        mechanicalWear = Math.max(mechanicalWear, 0.0f);
        
        // Restore steering chain tension
        steeringChainTension = 1.0f - mechanicalWear * 0.3f;
    }
    
    /**
     * Applies upgrade bonuses specific to rudder components
     */
    @Override
    protected void applySpecificUpgradeBonus() {
        // Upgrades improve turning efficiency and reduce mechanical wear
        turningEfficiency += upgradeLevel * 0.05f;
        
        // Reduce mechanical wear from upgrades
        mechanicalWear *= Math.max(0.5f, 1.0f - (upgradeLevel * 0.1f));
        
        // Reduce fouling accumulation
        fouling *= Math.max(0.6f, 1.0f - (upgradeLevel * 0.08f));
        
        Logger.world("Rudder {} upgraded to level {}", name, upgradeLevel);
    }

    /**
     * Gets damage resistance for specific damage types
     */
    @Override
    protected float getDamageResistance(DamageType damageType) {
        switch (damageType) {
            case COLLISION:
                return 0.2f; // Moderate collision resistance
            case FATIGUE:
                return 0.1f; // Low fatigue resistance
            case CORROSION:
                return 0.3f; // Good corrosion resistance (treated wood/metal)
            case CANNON_BALL:
                return 0.1f; // Low resistance to direct hits
            case EXPLOSION:
                return 0.15f; // Low explosion resistance
            case FIRE:
                return 0.25f; // Moderate fire resistance
            default:
                return 0.0f; // No resistance to other damage types
        }
    }

    /**
     * Effect when rudder is destroyed
     */
    @Override
    protected void onDestroyedEffect() {
        // Jam the rudder when destroyed
        isJammed = true;
        jammedAngle = currentAngle;
        isResponsive = false;
        
        // Set efficiency to zero
        efficiency = 0.0f;
        
        Logger.world("Rudder {} has been destroyed and is jammed at angle {}", name, jammedAngle);
    }

    // Getters for component properties
    public RudderType getRudderType() { return rudderType; }
    public float getRudderArea() { return rudderArea; }
    public float getMaxAngle() { return maxAngle; }
    public float getTargetAngle() { return targetAngle; }
    public float getAngularVelocity() { return angularVelocity; }
    public boolean isResponsive() { return isResponsive && !isJammed; }
    public float getTurningEfficiency() { return turningEfficiency * efficiency; }
    public float getDragCoefficient() { return dragCoefficient; }
    public float getStructuralDamage() { return structuralDamage; }
    public float getFouling() { return fouling; }
    public boolean isJammed() { return isJammed; }
    public float getJammedAngle() { return jammedAngle; }
    public float getSteeringChainTension() { return steeringChainTension; }
    public float getMechanicalWear() { return mechanicalWear; }
    public boolean hasHydraulicAssist() { return hasHydraulicAssist; }
    public float getHydraulicPressure() { return hydraulicPressure; }
    
    // Setters for component state
    public void setResponsive(boolean responsive) { this.isResponsive = responsive; }
    public void setHydraulicAssist(boolean hasAssist) { this.hasHydraulicAssist = hasAssist; }
    public void setHydraulicPressure(float pressure) { this.hydraulicPressure = Math.max(0.0f, pressure); }
}