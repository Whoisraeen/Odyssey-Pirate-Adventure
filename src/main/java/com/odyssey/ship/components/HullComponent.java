package com.odyssey.ship.components;

import com.odyssey.ship.ShipComponent;
import com.odyssey.ship.ComponentType;
import com.odyssey.ship.DamageType;
import com.odyssey.util.Logger;
import org.joml.Vector3f;

/**
 * Hull component - the main structural component of a ship.
 * Determines buoyancy, structural integrity, and overall ship health.
 */
public class HullComponent extends ShipComponent {
    
    // Hull-specific properties
    private float buoyancy;
    private float maxBuoyancy;
    private float waterIntake; // Water entering through damage
    private float structuralIntegrity;
    private HullMaterial material;
    private float thickness;
    
    // Hull sections for localized damage
    private final HullSection[] sections;
    private static final int HULL_SECTIONS = 8; // Bow, stern, port/starboard sides, etc.
    
    public HullComponent(String name, Vector3f position, HullMaterial material, float thickness) {
        super(ComponentType.HULL, name, position, 
              material.getBaseHealth() * thickness, 
              material.getBaseMass() * thickness);
        
        this.material = material;
        this.thickness = thickness;
        this.maxBuoyancy = calculateMaxBuoyancy();
        this.buoyancy = maxBuoyancy;
        this.waterIntake = 0.0f;
        this.structuralIntegrity = 1.0f;
        
        // Initialize hull sections
        this.sections = new HullSection[HULL_SECTIONS];
        for (int i = 0; i < HULL_SECTIONS; i++) {
            sections[i] = new HullSection(HullSectionType.values()[i], maxHealth / HULL_SECTIONS);
        }
    }
    
    @Override
    protected void updateComponent(float deltaTime) {
        // Update water intake based on damage
        updateWaterIntake(deltaTime);
        
        // Update buoyancy based on water intake
        updateBuoyancy(deltaTime);
        
        // Update structural integrity
        updateStructuralIntegrity();
        
        // Check for critical damage
        checkCriticalDamage();
    }
    
    @Override
    public void takeDamage(float damage, DamageType damageType) {
        // Distribute damage across hull sections
        distributeDamage(damage, damageType);
        
        // Apply damage to main component
        super.takeDamage(damage, damageType);
        
        // Hull damage creates water intake
        if (damageType == DamageType.CANNON_BALL || 
            damageType == DamageType.RAMMING || 
            damageType == DamageType.COLLISION ||
            damageType == DamageType.REEF) {
            
            float intakeIncrease = damage * 0.1f;
            waterIntake += intakeIncrease;
            
            Logger.world("Hull breach! Water intake increased by {}", intakeIncrease);
        }
    }
    
    @Override
    protected float getDamageResistance(DamageType damageType) {
        float baseResistance = material.getDamageResistance(damageType);
        float thicknessBonus = Math.min(thickness * 0.1f, 0.5f); // Max 50% bonus
        
        return Math.min(baseResistance + thicknessBonus, 0.9f); // Max 90% resistance
    }
    
    @Override
    protected void onDestroyedEffect() {
        // Hull destruction means ship sinking
        Logger.world("CRITICAL: Hull destroyed! Ship is sinking!");
        
        // Massive water intake
        waterIntake = maxBuoyancy * 2.0f;
        buoyancy = 0.0f;
        structuralIntegrity = 0.0f;
    }
    
    @Override
    protected void applySpecificUpgradeBonus() {
        // Increase thickness and material quality
        thickness *= 1.05f;
        maxBuoyancy = calculateMaxBuoyancy();
        
        // Reduce water intake
        waterIntake *= 0.9f;
        
        Logger.world("Hull upgraded: thickness={}, buoyancy={}", thickness, maxBuoyancy);
    }
    
    /**
     * Repairs hull breaches and reduces water intake
     */
    public void patchHull(float repairAmount) {
        repair(repairAmount);
        
        // Reduce water intake
        float intakeReduction = repairAmount * 0.05f;
        waterIntake = Math.max(0, waterIntake - intakeReduction);
        
        Logger.world("Hull patched: water intake reduced by {}", intakeReduction);
    }
    
    /**
     * Pumps water out of the hull
     */
    public void pumpWater(float pumpRate) {
        if (waterIntake > 0) {
            float actualPump = Math.min(pumpRate, waterIntake);
            waterIntake -= actualPump;
            
            // Restore some buoyancy
            float buoyancyRestore = actualPump * 0.5f;
            buoyancy = Math.min(maxBuoyancy, buoyancy + buoyancyRestore);
        }
    }
    
    /**
     * Gets the current water level in the hull (0.0 to 1.0)
     */
    public float getWaterLevel() {
        return Math.min(1.0f, waterIntake / maxBuoyancy);
    }
    
    /**
     * Checks if the ship is sinking
     */
    public boolean isSinking() {
        return buoyancy <= 0 || waterIntake >= maxBuoyancy;
    }
    
    /**
     * Gets the time until ship sinks (in seconds)
     */
    public float getTimeUntilSinking() {
        if (!isSinking() && waterIntake <= 0) return Float.POSITIVE_INFINITY;
        
        float remainingBuoyancy = buoyancy;
        float currentIntakeRate = waterIntake * 0.1f; // Water intake rate per second
        
        if (currentIntakeRate <= 0) return Float.POSITIVE_INFINITY;
        
        return remainingBuoyancy / currentIntakeRate;
    }
    
    private void updateWaterIntake(float deltaTime) {
        if (waterIntake > 0) {
            // Water intake increases over time if not repaired
            float intakeIncrease = waterIntake * 0.01f * deltaTime; // 1% increase per second
            waterIntake += intakeIncrease;
            
            // Natural reduction through drainage (small amount)
            float naturalDrainage = 0.1f * deltaTime;
            waterIntake = Math.max(0, waterIntake - naturalDrainage);
        }
    }
    
    private void updateBuoyancy(float deltaTime) {
        // Buoyancy decreases with water intake
        float targetBuoyancy = Math.max(0, maxBuoyancy - waterIntake);
        
        // Gradual change to target buoyancy
        float changeRate = Math.abs(targetBuoyancy - buoyancy) * 2.0f * deltaTime;
        
        if (buoyancy > targetBuoyancy) {
            buoyancy = Math.max(targetBuoyancy, buoyancy - changeRate);
        } else if (buoyancy < targetBuoyancy) {
            buoyancy = Math.min(targetBuoyancy, buoyancy + changeRate);
        }
    }
    
    private void updateStructuralIntegrity() {
        // Structural integrity based on hull section health
        float totalSectionHealth = 0;
        for (HullSection section : sections) {
            totalSectionHealth += section.getHealthPercentage();
        }
        
        structuralIntegrity = totalSectionHealth / HULL_SECTIONS;
        
        // Minimum structural integrity affects ship performance
        if (structuralIntegrity < 0.3f && isActive) {
            Logger.world("Warning: Structural integrity critical ({}%)", 
                        (int)(structuralIntegrity * 100));
        }
    }
    
    private void checkCriticalDamage() {
        // Check for catastrophic hull failure
        int destroyedSections = 0;
        for (HullSection section : sections) {
            if (section.isDestroyed()) {
                destroyedSections++;
            }
        }
        
        // If more than half the sections are destroyed, hull fails
        if (destroyedSections > HULL_SECTIONS / 2 && !isDestroyed) {
            Logger.world("Critical hull failure: {} sections destroyed", destroyedSections);
            health = 0; // Force destruction
        }
    }
    
    private void distributeDamage(float damage, DamageType damageType) {
        // Distribute damage across hull sections
        // Some damage types affect specific sections more
        
        for (int i = 0; i < HULL_SECTIONS; i++) {
            HullSection section = sections[i];
            float sectionDamage = damage / HULL_SECTIONS;
            
            // Modify damage based on damage type and section type
            sectionDamage *= getSectionDamageMultiplier(section.getType(), damageType);
            
            section.takeDamage(sectionDamage, damageType);
        }
    }
    
    private float getSectionDamageMultiplier(HullSectionType sectionType, DamageType damageType) {
        switch (damageType) {
            case RAMMING:
                return sectionType == HullSectionType.BOW ? 3.0f : 0.5f;
            case REEF:
                return sectionType == HullSectionType.KEEL ? 4.0f : 1.0f;
            case CANNON_BALL:
                // Sides take more damage from cannon fire
                return (sectionType == HullSectionType.PORT_SIDE || 
                       sectionType == HullSectionType.STARBOARD_SIDE) ? 2.0f : 1.0f;
            default:
                return 1.0f;
        }
    }
    
    private float calculateMaxBuoyancy() {
        // Calculate buoyancy based on hull volume and material
        return getVolume() * material.getBuoyancyFactor() * 1000.0f; // Water density
    }
    
    /**
     * Gets the hull volume for buoyancy calculations
     */
    public float getVolume() {
        // Calculate volume based on hull dimensions and thickness
        // This is a simplified calculation - in reality would be more complex
        float baseVolume = 100.0f; // Base hull volume in cubic meters
        return baseVolume * thickness * material.getBuoyancyFactor();
    }
    
    // Getters
    public float getBuoyancy() { return buoyancy; }
    public float getMaxBuoyancy() { return maxBuoyancy; }
    public float getBuoyancyPercentage() { return buoyancy / maxBuoyancy; }
    public float getWaterIntake() { return waterIntake; }
    public float getStructuralIntegrity() { return structuralIntegrity; }
    public HullMaterial getMaterial() { return material; }
    public float getThickness() { return thickness; }
    public HullSection[] getSections() { return sections.clone(); }
    
    /**
     * Hull materials with different properties
     */
    public enum HullMaterial {
        WOOD("Wood", 800.0f, 50.0f, 1.0f),
        REINFORCED_WOOD("Reinforced Wood", 1200.0f, 75.0f, 1.1f),
        IRON("Iron", 2000.0f, 150.0f, 0.8f),
        STEEL("Steel", 2500.0f, 200.0f, 0.9f),
        MAGICAL_WOOD("Magical Wood", 1500.0f, 100.0f, 1.3f);
        
        private final String name;
        private final float baseHealth;
        private final float baseMass;
        private final float buoyancyFactor;
        
        HullMaterial(String name, float baseHealth, float baseMass, float buoyancyFactor) {
            this.name = name;
            this.baseHealth = baseHealth;
            this.baseMass = baseMass;
            this.buoyancyFactor = buoyancyFactor;
        }
        
        public String getName() { return name; }
        public float getBaseHealth() { return baseHealth; }
        public float getBaseMass() { return baseMass; }
        public float getBuoyancyFactor() { return buoyancyFactor; }
        
        public float getDamageResistance(DamageType damageType) {
            switch (this) {
                case WOOD:
                    switch (damageType) {
                        case FIRE: return 0.1f;
                        case ROT: return 0.2f;
                        case CANNON_BALL: return 0.3f;
                        default: return 0.2f;
                    }
                case REINFORCED_WOOD:
                    switch (damageType) {
                        case FIRE: return 0.3f;
                        case ROT: return 0.4f;
                        case CANNON_BALL: return 0.5f;
                        default: return 0.4f;
                    }
                case IRON:
                    switch (damageType) {
                        case FIRE: return 0.8f;
                        case CORROSION: return 0.2f;
                        case CANNON_BALL: return 0.7f;
                        default: return 0.6f;
                    }
                case STEEL:
                    switch (damageType) {
                        case FIRE: return 0.9f;
                        case CORROSION: return 0.5f;
                        case CANNON_BALL: return 0.8f;
                        default: return 0.7f;
                    }
                case MAGICAL_WOOD:
                    switch (damageType) {
                        case MAGIC: return 0.8f;
                        case CURSE: return 0.9f;
                        case FIRE: return 0.6f;
                        default: return 0.5f;
                    }
                default:
                    return 0.2f;
            }
        }
    }
    
    /**
     * Hull section types
     */
    public enum HullSectionType {
        BOW("Bow"),
        STERN("Stern"),
        PORT_SIDE("Port Side"),
        STARBOARD_SIDE("Starboard Side"),
        KEEL("Keel"),
        DECK("Deck"),
        UPPER_HULL("Upper Hull"),
        LOWER_HULL("Lower Hull");
        
        private final String name;
        
        HullSectionType(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
    }
    
    /**
     * Individual hull section with localized damage
     */
    public static class HullSection {
        private final HullSectionType type;
        private float health;
        private float maxHealth;
        private boolean isDestroyed;
        
        public HullSection(HullSectionType type, float maxHealth) {
            this.type = type;
            this.maxHealth = maxHealth;
            this.health = maxHealth;
            this.isDestroyed = false;
        }
        
        public void takeDamage(float damage, DamageType damageType) {
            if (isDestroyed) return;
            
            health -= damage;
            health = Math.max(0, health);
            
            if (health <= 0) {
                isDestroyed = true;
                Logger.world("Hull section {} destroyed!", type.getName());
            }
        }
        
        public void repair(float repairAmount) {
            if (isDestroyed) return;
            
            health += repairAmount;
            health = Math.min(maxHealth, health);
        }
        
        public void replace() {
            health = maxHealth;
            isDestroyed = false;
        }
        
        public HullSectionType getType() { return type; }
        public float getHealth() { return health; }
        public float getMaxHealth() { return maxHealth; }
        public float getHealthPercentage() { return health / maxHealth; }
        public boolean isDestroyed() { return isDestroyed; }
    }
}