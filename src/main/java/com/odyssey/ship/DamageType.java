package com.odyssey.ship;

/**
 * Enumeration of damage types that can affect ship components.
 * Each damage type has different effects and resistances.
 */
public enum DamageType {
    
    // Physical damage types
    CANNON_BALL("Cannon Ball", DamageCategory.PHYSICAL, 1.0f),
    RAMMING("Ramming", DamageCategory.PHYSICAL, 1.2f),
    COLLISION("Collision", DamageCategory.PHYSICAL, 0.8f),
    BOARDING("Boarding", DamageCategory.PHYSICAL, 0.6f),
    
    // Elemental damage types
    FIRE("Fire", DamageCategory.ELEMENTAL, 0.9f),
    WATER("Water", DamageCategory.ELEMENTAL, 0.4f),
    LIGHTNING("Lightning", DamageCategory.ELEMENTAL, 1.1f),
    ICE("Ice", DamageCategory.ELEMENTAL, 0.7f),
    
    // Environmental damage types
    STORM("Storm", DamageCategory.ENVIRONMENTAL, 0.8f),
    REEF("Reef", DamageCategory.ENVIRONMENTAL, 1.0f),
    WHIRLPOOL("Whirlpool", DamageCategory.ENVIRONMENTAL, 0.9f),
    KRAKEN("Kraken", DamageCategory.ENVIRONMENTAL, 1.5f),
    
    // Wear and tear
    FATIGUE("Fatigue", DamageCategory.WEAR, 0.1f),
    CORROSION("Corrosion", DamageCategory.WEAR, 0.3f),
    ROT("Rot", DamageCategory.WEAR, 0.2f),
    
    // Special damage types
    EXPLOSION("Explosion", DamageCategory.PHYSICAL, 1.3f),
    MAGIC("Magic", DamageCategory.SPECIAL, 1.0f),
    CURSE("Curse", DamageCategory.SPECIAL, 0.5f),
    GHOST("Ghost", DamageCategory.SPECIAL, 0.8f);
    
    private final String displayName;
    private final DamageCategory category;
    private final float baseDamageMultiplier;
    
    DamageType(String displayName, DamageCategory category, float baseDamageMultiplier) {
        this.displayName = displayName;
        this.category = category;
        this.baseDamageMultiplier = baseDamageMultiplier;
    }
    
    /**
     * Gets the display name for UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the damage category
     */
    public DamageCategory getCategory() {
        return category;
    }
    
    /**
     * Gets the base damage multiplier
     */
    public float getBaseDamageMultiplier() {
        return baseDamageMultiplier;
    }
    
    /**
     * Checks if this damage type affects a specific component type more
     */
    public float getComponentDamageMultiplier(ComponentType componentType) {
        switch (this) {
            case FIRE:
                // Fire is especially effective against sails and wooden components
                switch (componentType) {
                    case SAIL:
                        return 2.0f;
                    case MAST:
                    case HULL:
                        return 1.5f;
                    case ARMOR_PLATING:
                        return 0.5f; // Metal armor resists fire
                    default:
                        return 1.0f;
                }
                
            case WATER:
                // Water damages certain components over time
                switch (componentType) {
                    case CANNON:
                    case SWIVEL_GUN:
                        return 1.5f; // Rust and corrosion
                    case CHARTS:
                    case COMPASS:
                        return 2.0f; // Water ruins navigation equipment
                    case SAIL:
                        return 0.5f; // Sails can handle water
                    default:
                        return 1.0f;
                }
                
            case LIGHTNING:
                // Lightning affects tall components and metal
                switch (componentType) {
                    case MAST:
                        return 3.0f; // Masts attract lightning
                    case CANNON:
                    case ARMOR_PLATING:
                        return 1.5f; // Metal conducts electricity
                    case SAIL:
                        return 0.3f; // Fabric doesn't conduct well
                    default:
                        return 1.0f;
                }
                
            case CANNON_BALL:
                // Cannon balls are effective against structure
                switch (componentType) {
                    case HULL:
                    case ARMOR_PLATING:
                        return 1.5f;
                    case MAST:
                        return 2.0f; // Masts are vulnerable to direct hits
                    case SAIL:
                        return 0.5f; // Cannon balls often pass through sails
                    default:
                        return 1.0f;
                }
                
            case RAMMING:
                // Ramming affects hull and structure
                switch (componentType) {
                    case HULL:
                        return 2.0f;
                    case ARMOR_PLATING:
                        return 1.5f;
                    case REINFORCEMENT:
                        return 1.8f;
                    default:
                        return 0.5f; // Other components less affected
                }
                
            case CORROSION:
                // Corrosion affects metal components
                switch (componentType) {
                    case CANNON:
                    case SWIVEL_GUN:
                    case ANCHOR:
                    case ARMOR_PLATING:
                        return 2.0f;
                    case HULL:
                    case MAST:
                        return 0.5f; // Wood resists corrosion better
                    default:
                        return 1.0f;
                }
                
            case ROT:
                // Rot affects wooden components
                switch (componentType) {
                    case HULL:
                    case MAST:
                    case RUDDER:
                        return 2.0f;
                    case CANNON:
                    case ARMOR_PLATING:
                        return 0.1f; // Metal doesn't rot
                    default:
                        return 1.0f;
                }
                
            case STORM:
                // Storms affect exposed components
                switch (componentType) {
                    case SAIL:
                        return 2.0f;
                    case MAST:
                        return 1.5f;
                    case RUDDER:
                        return 1.3f;
                    case CARGO_HOLD:
                    case CREW_QUARTERS:
                        return 0.3f; // Interior components protected
                    default:
                        return 1.0f;
                }
                
            case KRAKEN:
                // Kraken attacks are devastating to hull and structure
                switch (componentType) {
                    case HULL:
                        return 3.0f;
                    case MAST:
                        return 2.5f;
                    case RUDDER:
                        return 2.0f;
                    default:
                        return 1.5f;
                }
                
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the damage over time effect (damage per second)
     */
    public float getDamageOverTime() {
        switch (this) {
            case FIRE:
                return 5.0f; // Fire spreads and continues burning
            case CORROSION:
                return 1.0f; // Slow but persistent
            case ROT:
                return 0.5f; // Very slow degradation
            case CURSE:
                return 2.0f; // Magical degradation
            default:
                return 0.0f; // No DoT effect
        }
    }
    
    /**
     * Checks if this damage type can spread to other components
     */
    public boolean canSpread() {
        switch (this) {
            case FIRE:
            case ROT:
            case CORROSION:
            case CURSE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the spread chance per second (0.0 to 1.0)
     */
    public float getSpreadChance() {
        switch (this) {
            case FIRE:
                return 0.1f; // 10% chance per second
            case ROT:
                return 0.01f; // 1% chance per second
            case CORROSION:
                return 0.02f; // 2% chance per second
            case CURSE:
                return 0.05f; // 5% chance per second
            default:
                return 0.0f;
        }
    }
    
    /**
     * Gets the visual effect color for this damage type
     */
    public int getEffectColor() {
        switch (this) {
            case FIRE:
                return 0xFF4500; // Orange-red
            case WATER:
                return 0x0077BE; // Blue
            case LIGHTNING:
                return 0xFFFF00; // Yellow
            case ICE:
                return 0x87CEEB; // Sky blue
            case CORROSION:
                return 0x8B4513; // Brown
            case ROT:
                return 0x556B2F; // Dark olive green
            case MAGIC:
                return 0x9370DB; // Purple
            case CURSE:
                return 0x8B0000; // Dark red
            case GHOST:
                return 0xF0F8FF; // Ghost white
            default:
                return 0xFFFFFF; // White
        }
    }
    
    /**
     * Damage categories for organization and resistance calculations
     */
    public enum DamageCategory {
        PHYSICAL("Physical"),
        ELEMENTAL("Elemental"),
        ENVIRONMENTAL("Environmental"),
        WEAR("Wear & Tear"),
        SPECIAL("Special");
        
        private final String displayName;
        
        DamageCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}