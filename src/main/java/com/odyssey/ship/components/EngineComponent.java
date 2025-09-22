package com.odyssey.ship.components;

import com.odyssey.ship.ShipComponent;
import com.odyssey.ship.ComponentType;
import com.odyssey.ship.DamageType;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Engine component - provides steam-powered propulsion for ships.
 * Handles fuel consumption, power generation, mechanical systems, and maintenance.
 */
public class EngineComponent extends ShipComponent {
    
    // Engine properties
    private final EngineType engineType;
    private final float maxPower; // Maximum power output in horsepower
    private final float fuelConsumption; // Fuel consumption per hour at max power
    private final float maxRPM; // Maximum revolutions per minute
    
    // Engine state
    private float currentPower; // Current power output (0-1 scale)
    private float currentRPM;
    private float targetPower; // Desired power level
    private boolean isRunning;
    private boolean isStarting;
    private float startupTime; // Time to reach full power
    private float shutdownTime; // Time to fully stop
    
    // Fuel system
    private FuelType fuelType;
    private float fuelAmount; // Current fuel in tons
    private float maxFuelCapacity; // Maximum fuel capacity
    private float fuelQuality; // Quality affects efficiency (0.5-1.5)
    private float fuelEfficiency; // Current efficiency multiplier
    
    // Mechanical condition
    private float boilerPressure; // Steam pressure (0-100 PSI)
    private float engineTemperature; // Engine temperature
    private float mechanicalWear; // Accumulated wear from operation
    private float vibration; // Engine vibration level
    private boolean isOverheating;
    private boolean isPressureHigh;
    
    // Maintenance
    private float maintenanceLevel; // How well maintained (0-1)
    private float timeSinceService; // Hours since last service
    private float serviceInterval; // Hours between services
    private boolean needsMaintenance;
    
    // Crew requirements
    private int requiredCrew; // Engineers needed
    private int assignedCrew;
    private float crewSkill; // Average skill level of assigned crew
    
    // Performance modifiers
    private float powerEfficiency; // Overall power efficiency
    private float reliabilityModifier; // Affects breakdown chance
    
    public EngineComponent(String name, Vector3f position, EngineType engineType) {
        super(ComponentType.ENGINE, name, position, 
              engineType.getBaseHealth(), engineType.getBaseMass());
        
        this.engineType = engineType;
        this.maxPower = engineType.getMaxPower();
        this.fuelConsumption = engineType.getFuelConsumption();
        this.maxRPM = engineType.getMaxRPM();
        
        this.currentPower = 0.0f;
        this.currentRPM = 0.0f;
        this.targetPower = 0.0f;
        this.isRunning = false;
        this.isStarting = false;
        this.startupTime = engineType.getStartupTime();
        this.shutdownTime = engineType.getShutdownTime();
        
        this.fuelType = FuelType.COAL; // Default fuel
        this.maxFuelCapacity = engineType.getFuelCapacity();
        this.fuelAmount = maxFuelCapacity * 0.8f; // Start with 80% fuel
        this.fuelQuality = 1.0f;
        this.fuelEfficiency = 1.0f;
        
        this.boilerPressure = 0.0f;
        this.engineTemperature = 20.0f; // Ambient temperature
        this.mechanicalWear = 0.0f;
        this.vibration = 0.0f;
        this.isOverheating = false;
        this.isPressureHigh = false;
        
        this.maintenanceLevel = 1.0f;
        this.timeSinceService = 0.0f;
        this.serviceInterval = engineType.getServiceInterval();
        this.needsMaintenance = false;
        
        this.requiredCrew = engineType.getRequiredCrew();
        this.assignedCrew = requiredCrew;
        this.crewSkill = 0.7f; // Average skill
        
        this.powerEfficiency = 1.0f;
        this.reliabilityModifier = 1.0f;
    }
    
    @Override
    protected void updateComponent(float deltaTime) {
        // Update engine operation
        updateEngineOperation(deltaTime);
        
        // Update fuel consumption
        updateFuelConsumption(deltaTime);
        
        // Update mechanical systems
        updateMechanicalSystems(deltaTime);
        
        // Update maintenance status
        updateMaintenance(deltaTime);
        
        // Check for problems
        checkEngineProblems();
    }
    
    @Override
    public void takeDamage(float damage, DamageType damageType) {
        super.takeDamage(damage, damageType);
        
        // Engine-specific damage effects
        switch (damageType) {
            case CANNON_BALL:
                // Direct hits can damage boiler or machinery
                mechanicalWear += damage * 0.2f;
                if (damage > 100.0f) {
                    // Heavy damage might cause steam leak
                    boilerPressure *= 0.7f;
                    logger.warn(Logger.WORLD, "Engine {} damaged - steam pressure lost!", name);
                }
                break;
                
            case FIRE:
                // Fire can damage fuel lines and overheat engine
                engineTemperature += damage * 2.0f;
                if (engineTemperature > 150.0f) {
                    isOverheating = true;
                    logger.error(Logger.WORLD, "Engine {} overheating due to fire damage!", name);
                }
                break;
                
            case EXPLOSION:
                // Explosions can cause catastrophic damage
                mechanicalWear += damage * 0.5f;
                vibration += damage * 0.1f;
                if (damage > 200.0f) {
                    // Might cause emergency shutdown
                    emergencyShutdown();
                }
                break;
                
            case WATER:
                // Water can damage electrical systems and cause corrosion
                powerEfficiency *= 0.9f;
                mechanicalWear += damage * 0.1f;
                break;
        }
    }
    
    @Override
    protected float getDamageResistance(DamageType damageType) {
        switch (damageType) {
            case CANNON_BALL:
                return engineType == EngineType.HEAVY_STEAM ? 0.7f : 0.3f; // Vulnerable to explosions
            case FIRE:
                return 0.4f; // Engines have some fire resistance
            case WATER:
                return 0.6f; // Some water resistance
            case CORROSION:
                return 0.4f; // Metal components corrode
            default:
                return 0.4f;
        }
    }
    
    @Override
    protected void onDestroyedEffect() {
        // Destroyed engine might explode
        if (isRunning && boilerPressure > 50.0f) {
            Logger.world("Engine {} exploded when destroyed!", name);
            // TODO: Create explosion that damages nearby components
        }
        
        // Emergency shutdown
        emergencyShutdown();
        
        // Lose all fuel
        fuelAmount = 0.0f;
    }
    
    @Override
    protected void applySpecificUpgradeBonus() {
        // Upgrades improve efficiency, reduce wear, and increase reliability
        powerEfficiency *= (1.0f + upgradeLevel * 0.1f);
        reliabilityModifier *= (1.0f + upgradeLevel * 0.15f);
        
        // Reduce accumulated wear
        mechanicalWear *= 0.9f;
        
        Logger.world("Engine {} upgraded to level {} - efficiency improved", name, upgradeLevel);
    }
    
    /**
     * Starts the engine
     */
    public boolean startEngine() {
        if (isRunning || isStarting || isDestroyed || !isActive) {
            return false;
        }
        
        // Check prerequisites
        if (fuelAmount <= 0) {
            Logger.world("Cannot start engine {} - no fuel", name);
            return false;
        }
        
        if (assignedCrew < requiredCrew) {
            Logger.world("Cannot start engine {} - insufficient crew", name);
            return false;
        }
        
        if (needsMaintenance && maintenanceLevel < 0.3f) {
            Logger.world("Cannot start engine {} - requires maintenance", name);
            return false;
        }
        
        isStarting = true;
        Logger.world("Starting engine {}", name);
        return true;
    }
    
    /**
     * Stops the engine
     */
    public void stopEngine() {
        if (!isRunning && !isStarting) return;
        
        targetPower = 0.0f;
        isStarting = false;
        
        Logger.world("Stopping engine {}", name);
    }
    
    /**
     * Emergency shutdown - immediate stop
     */
    public void emergencyShutdown() {
        isRunning = false;
        isStarting = false;
        currentPower = 0.0f;
        targetPower = 0.0f;
        currentRPM = 0.0f;
        boilerPressure = 0.0f;
        
        Logger.world("Emergency shutdown of engine {}", name);
    }
    
    /**
     * Sets the desired power level (0-1)
     */
    public void setPowerLevel(float powerLevel) {
        if (!isRunning && !isStarting) return;
        
        targetPower = Math.max(0.0f, Math.min(1.0f, powerLevel));
    }
    
    /**
     * Adds fuel to the engine
     */
    public float addFuel(FuelType fuelType, float amount, float quality) {
        if (this.fuelType != fuelType && fuelAmount > 0) {
            // Can't mix fuel types
            Logger.world("Cannot add {} to engine {} - different fuel type",
                        fuelType.getName(), name);
            return 0.0f;
        }
        
        float spaceAvailable = maxFuelCapacity - fuelAmount;
        float actualAmount = Math.min(amount, spaceAvailable);
        
        if (actualAmount > 0) {
            // Calculate weighted average quality
            float totalFuel = fuelAmount + actualAmount;
            fuelQuality = (fuelQuality * fuelAmount + quality * actualAmount) / totalFuel;
            
            fuelAmount += actualAmount;
            this.fuelType = fuelType;
            
            Logger.world("Added {} tons of {} to engine {} (quality: {})",
                        actualAmount, fuelType.getName(), name, quality);
        }
        
        return actualAmount;
    }
    
    /**
     * Performs maintenance on the engine
     */
    public void performMaintenance(float maintenanceAmount, float crewSkillLevel) {
        float effectiveAmount = maintenanceAmount * crewSkillLevel;
        
        // Improve maintenance level
        maintenanceLevel = Math.min(1.0f, maintenanceLevel + effectiveAmount);
        
        // Reduce wear
        mechanicalWear = Math.max(0.0f, mechanicalWear - effectiveAmount * 2.0f);
        
        // Reset service timer if major maintenance
        if (effectiveAmount > 0.5f) {
            timeSinceService = 0.0f;
            needsMaintenance = false;
        }
        
        // Improve efficiency
        powerEfficiency = Math.min(1.2f, powerEfficiency + effectiveAmount * 0.1f);
        
        Logger.world("Maintenance performed on engine {} - level now {}",
                    name, maintenanceLevel);
    }
    
    /**
     * Assigns crew to the engine
     */
    public void assignCrew(int crewCount, float averageSkill) {
        assignedCrew = Math.max(0, crewCount);
        crewSkill = Math.max(0.1f, Math.min(1.0f, averageSkill));
        
        Logger.world("Assigned {} crew to engine {} (skill: {})",
                    crewCount, name, crewSkill);
    }
    
    /**
     * Gets the current thrust force generated by the engine
     */
    public float getThrustForce() {
        if (!isRunning) return 0.0f;
        
        float baseThrust = maxPower * currentPower * engineType.getThrustMultiplier();
        
        // Apply efficiency modifiers
        baseThrust *= powerEfficiency;
        baseThrust *= fuelEfficiency;
        baseThrust *= maintenanceLevel;
        baseThrust *= (0.5f + crewSkill * 0.5f); // Crew skill affects performance
        
        // Reduce thrust if overheating or high pressure
        if (isOverheating) baseThrust *= 0.6f;
        if (isPressureHigh) baseThrust *= 0.8f;
        
        // Vibration reduces efficiency
        baseThrust *= (1.0f - vibration * 0.2f);
        
        return Math.max(0.0f, baseThrust);
    }
    
    private void updateEngineOperation(float deltaTime) {
        if (isStarting) {
            // Engine is starting up
            float startupProgress = deltaTime / startupTime;
            currentPower += startupProgress;
            
            if (currentPower >= 0.3f) {
                isRunning = true;
                isStarting = false;
                Logger.world("Engine {} started successfully", name);
            }
        } else if (isRunning) {
            // Engine is running - adjust power to target
            float powerDelta = targetPower - currentPower;
            float adjustmentRate = deltaTime / (powerDelta > 0 ? startupTime : shutdownTime);
            
            currentPower += powerDelta * adjustmentRate;
            currentPower = Math.max(0.0f, Math.min(1.0f, currentPower));
            
            // Stop engine if power reaches zero
            if (currentPower <= 0.01f && targetPower <= 0.01f) {
                isRunning = false;
                currentPower = 0.0f;
                Logger.world("Engine {} stopped", name);
            }
        }
        
        // Update RPM based on power
        if (isRunning) {
            float targetRPM = maxRPM * currentPower;
            currentRPM += (targetRPM - currentRPM) * deltaTime * 2.0f;
        } else {
            currentRPM *= Math.max(0.0f, 1.0f - deltaTime * 3.0f); // Spin down
        }
        
        // Update boiler pressure
        if (isRunning) {
            float targetPressure = 80.0f * currentPower;
            boilerPressure += (targetPressure - boilerPressure) * deltaTime;
        } else {
            boilerPressure *= Math.max(0.0f, 1.0f - deltaTime * 0.5f); // Pressure drops
        }
    }
    
    private void updateFuelConsumption(float deltaTime) {
        if (!isRunning || fuelAmount <= 0) return;
        
        // Calculate fuel consumption rate
        float consumptionRate = fuelConsumption * currentPower * currentPower; // Quadratic consumption
        consumptionRate *= fuelType.getConsumptionMultiplier();
        consumptionRate /= fuelQuality; // Better quality = more efficient
        consumptionRate /= powerEfficiency; // Better efficiency = less consumption
        
        // Consume fuel
        float fuelUsed = consumptionRate * deltaTime / 3600.0f; // Convert per hour to per second
        fuelAmount = Math.max(0.0f, fuelAmount - fuelUsed);
        
        // Update fuel efficiency based on quality and engine condition
        fuelEfficiency = fuelQuality * maintenanceLevel * (0.8f + crewSkill * 0.2f);
        
        // Out of fuel
        if (fuelAmount <= 0.0f) {
            Logger.world("Engine {} out of fuel!", name);
            emergencyShutdown();
        }
    }
    
    private void updateMechanicalSystems(float deltaTime) {
        if (!isRunning) {
            // Engine cooling down
            engineTemperature += (20.0f - engineTemperature) * deltaTime * 0.1f;
            vibration *= Math.max(0.0f, 1.0f - deltaTime * 2.0f);
            return;
        }
        
        // Update temperature
        float targetTemp = 60.0f + currentPower * 40.0f; // 60-100°C operating range
        engineTemperature += (targetTemp - engineTemperature) * deltaTime * 0.2f;
        
        // Update vibration based on RPM and wear
        float baseVibration = currentRPM / maxRPM * 0.3f;
        baseVibration += mechanicalWear / maxHealth * 0.4f;
        baseVibration *= (2.0f - maintenanceLevel); // Poor maintenance increases vibration
        
        vibration += (baseVibration - vibration) * deltaTime;
        
        // Accumulate wear
        float wearRate = currentPower * 0.1f * deltaTime / 3600.0f; // Per hour
        wearRate *= (2.0f - maintenanceLevel); // Poor maintenance increases wear
        wearRate /= reliabilityModifier; // Upgrades reduce wear
        
        mechanicalWear += wearRate;
        
        // Check for overheating
        isOverheating = engineTemperature > 120.0f;
        
        // Check for high pressure
        isPressureHigh = boilerPressure > 90.0f;
    }
    
    private void updateMaintenance(float deltaTime) {
        if (isRunning) {
            timeSinceService += deltaTime / 3600.0f; // Convert to hours
            
            // Maintenance level degrades over time
            float degradeRate = 0.01f * deltaTime / 3600.0f; // 1% per hour
            degradeRate *= (1.0f + mechanicalWear / maxHealth); // Wear accelerates degradation
            
            maintenanceLevel = Math.max(0.0f, maintenanceLevel - degradeRate);
        }
        
        // Check if maintenance is needed
        needsMaintenance = timeSinceService > serviceInterval || maintenanceLevel < 0.4f;
    }
    
    private void checkEngineProblems() {
        // Check for breakdown
        if (isRunning && Math.random() < getBreakdownChance()) {
            Logger.world("Engine {} has broken down!", name);
            emergencyShutdown();
            mechanicalWear += 10.0f;
        }
        
        // Automatic safety shutdowns
        if (isOverheating && engineTemperature > 140.0f) {
            Logger.world("Engine {} emergency shutdown - overheating!", name);
            emergencyShutdown();
        }
        
        if (isPressureHigh && boilerPressure > 95.0f) {
            Logger.world("Engine {} emergency shutdown - pressure too high!", name);
            emergencyShutdown();
        }
    }
    
    private float getBreakdownChance() {
        float baseChance = 0.0001f; // 0.01% per update
        
        // Increase chance based on wear
        baseChance *= (1.0f + mechanicalWear / maxHealth * 5.0f);
        
        // Increase chance based on poor maintenance
        baseChance *= (2.0f - maintenanceLevel);
        
        // Increase chance if overheating or high pressure
        if (isOverheating) baseChance *= 3.0f;
        if (isPressureHigh) baseChance *= 2.0f;
        
        // Reduce chance with upgrades
        baseChance /= reliabilityModifier;
        
        return Math.min(0.01f, baseChance); // Max 1% chance
    }
    
    // Getters
    public EngineType getEngineType() { return engineType; }
    public float getMaxPower() { return maxPower; }
    public float getCurrentPower() { return currentPower; }
    public float getTargetPower() { return targetPower; }
    public float getCurrentRPM() { return currentRPM; }
    public float getMaxRPM() { return maxRPM; }
    public boolean isRunning() { return isRunning; }
    public boolean isStarting() { return isStarting; }
    public FuelType getFuelType() { return fuelType; }
    public float getFuelAmount() { return fuelAmount; }
    public float getMaxFuelCapacity() { return maxFuelCapacity; }
    public float getFuelPercentage() { return fuelAmount / maxFuelCapacity; }
    public float getFuelQuality() { return fuelQuality; }
    public float getBoilerPressure() { return boilerPressure; }
    public float getEngineTemperature() { return engineTemperature; }
    public float getMechanicalWear() { return mechanicalWear; }
    public float getVibration() { return vibration; }
    public boolean isOverheating() { return isOverheating; }
    public boolean isPressureHigh() { return isPressureHigh; }
    public float getMaintenanceLevel() { return maintenanceLevel; }
    public boolean needsMaintenance() { return needsMaintenance; }
    public int getRequiredCrew() { return requiredCrew; }
    public int getAssignedCrew() { return assignedCrew; }
    public float getCrewSkill() { return crewSkill; }
    public float getPowerEfficiency() { return powerEfficiency; }
    
    /**
     * Engine types with different characteristics
     */
    public enum EngineType {
        LIGHT_STEAM("Light Steam Engine", 300.0f, 250.0f, 500.0f, 2.0f, 1200.0f, 15.0f, 8.0f, 5.0f, 100.0f, 2, 0.8f),
        MEDIUM_STEAM("Medium Steam Engine", 500.0f, 400.0f, 800.0f, 3.5f, 1000.0f, 25.0f, 12.0f, 8.0f, 150.0f, 3, 1.0f),
        HEAVY_STEAM("Heavy Steam Engine", 800.0f, 600.0f, 1200.0f, 6.0f, 800.0f, 40.0f, 20.0f, 12.0f, 200.0f, 4, 1.2f),
        COMPOUND_ENGINE("Compound Engine", 600.0f, 450.0f, 1000.0f, 4.0f, 1100.0f, 30.0f, 15.0f, 10.0f, 120.0f, 3, 1.1f),
        TRIPLE_EXPANSION("Triple Expansion Engine", 700.0f, 500.0f, 1500.0f, 5.0f, 900.0f, 35.0f, 18.0f, 15.0f, 80.0f, 4, 1.3f);
        
        private final String name;
        private final float baseHealth;
        private final float baseMass;
        private final float maxPower;
        private final float fuelConsumption;
        private final float maxRPM;
        private final float fuelCapacity;
        private final float startupTime;
        private final float shutdownTime;
        private final float serviceInterval;
        private final int requiredCrew;
        private final float thrustMultiplier;
        
        EngineType(String name, float baseHealth, float baseMass, float maxPower, 
                  float fuelConsumption, float maxRPM, float fuelCapacity,
                  float startupTime, float shutdownTime, float serviceInterval,
                  int requiredCrew, float thrustMultiplier) {
            this.name = name;
            this.baseHealth = baseHealth;
            this.baseMass = baseMass;
            this.maxPower = maxPower;
            this.fuelConsumption = fuelConsumption;
            this.maxRPM = maxRPM;
            this.fuelCapacity = fuelCapacity;
            this.startupTime = startupTime;
            this.shutdownTime = shutdownTime;
            this.serviceInterval = serviceInterval;
            this.requiredCrew = requiredCrew;
            this.thrustMultiplier = thrustMultiplier;
        }
        
        public String getName() { return name; }
        public float getBaseHealth() { return baseHealth; }
        public float getBaseMass() { return baseMass; }
        public float getMaxPower() { return maxPower; }
        public float getFuelConsumption() { return fuelConsumption; }
        public float getMaxRPM() { return maxRPM; }
        public float getFuelCapacity() { return fuelCapacity; }
        public float getStartupTime() { return startupTime; }
        public float getShutdownTime() { return shutdownTime; }
        public float getServiceInterval() { return serviceInterval; }
        public int getRequiredCrew() { return requiredCrew; }
        public float getThrustMultiplier() { return thrustMultiplier; }
    }
    
    /**
     * Fuel types with different characteristics
     */
    public enum FuelType {
        COAL("Coal", 1.0f, 1.0f),
        WOOD("Wood", 0.7f, 1.3f),
        OIL("Oil", 1.5f, 0.8f),
        CHARCOAL("Charcoal", 1.2f, 1.1f);
        
        private final String name;
        private final float energyDensity; // Energy per unit mass
        private final float consumptionMultiplier; // Consumption rate modifier
        
        FuelType(String name, float energyDensity, float consumptionMultiplier) {
            this.name = name;
            this.energyDensity = energyDensity;
            this.consumptionMultiplier = consumptionMultiplier;
        }
        
        public String getName() { return name; }
        public float getEnergyDensity() { return energyDensity; }
        public float getConsumptionMultiplier() { return consumptionMultiplier; }
    }
}