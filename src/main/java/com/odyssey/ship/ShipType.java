package com.odyssey.ship;

/**
 * Defines different types of ships with their base characteristics.
 * Each ship type has different stats, capabilities, and component slots.
 */
public enum ShipType {
    
    // Small, fast ships
    SLOOP("Sloop", 
          500.0f,    // mass
          100.0f,    // health
          0.8f,      // maneuverability
          0.6f,      // stability
          50,        // cargo capacity
          8,         // crew capacity
          2,         // base cannons
          1,         // mast count
          ShipClass.LIGHT),
    
    CUTTER("Cutter",
           400.0f,
           80.0f,
           0.9f,
           0.5f,
           30,
           6,
           1,
           1,
           ShipClass.LIGHT),
    
    // Medium ships
    BRIGANTINE("Brigantine",
               1200.0f,
               200.0f,
               0.7f,
               0.7f,
               100,
               16,
               6,
               2,
               ShipClass.MEDIUM),
    
    FRIGATE("Frigate",
            1800.0f,
            300.0f,
            0.6f,
            0.8f,
            150,
            24,
            12,
            3,
            ShipClass.MEDIUM),
    
    CORVETTE("Corvette",
             1000.0f,
             180.0f,
             0.75f,
             0.65f,
             80,
             14,
             8,
             2,
             ShipClass.MEDIUM),
    
    // Large ships
    GALLEON("Galleon",
            3000.0f,
            500.0f,
            0.4f,
            0.9f,
            400,
            50,
            20,
            4,
            ShipClass.HEAVY),
    
    MAN_OF_WAR("Man of War",
               4000.0f,
               600.0f,
               0.3f,
               0.95f,
               300,
               60,
               32,
               3,
               ShipClass.HEAVY),
    
    SHIP_OF_THE_LINE("Ship of the Line",
                     5000.0f,
                     800.0f,
                     0.25f,
                     1.0f,
                     200,
                     80,
                     50,
                     3,
                     ShipClass.HEAVY),
    
    // Special ships
    MERCHANT_VESSEL("Merchant Vessel",
                    2000.0f,
                    250.0f,
                    0.5f,
                    0.8f,
                    600,
                    30,
                    4,
                    2,
                    ShipClass.MERCHANT),
    
    TREASURE_GALLEON("Treasure Galleon",
                     3500.0f,
                     400.0f,
                     0.35f,
                     0.85f,
                     800,
                     40,
                     16,
                     4,
                     ShipClass.MERCHANT),
    
    PIRATE_SHIP("Pirate Ship",
                1500.0f,
                220.0f,
                0.65f,
                0.7f,
                120,
                20,
                10,
                2,
                ShipClass.PIRATE),
    
    GHOST_SHIP("Ghost Ship",
               2000.0f,
               300.0f,
               0.6f,
               0.8f,
               100,
               25,
               14,
               3,
               ShipClass.SUPERNATURAL),
    
    WARSHIP("Warship",
            2500.0f,
            450.0f,
            0.5f,
            0.85f,
            180,
            35,
            18,
            3,
            ShipClass.HEAVY);
    
    // Ship properties
    private final String displayName;
    private final float baseMass;
    private final float baseHealth;
    private final float baseManeuverability;
    private final float baseStability;
    private final int baseCargoCapacity;
    private final int baseCrewCapacity;
    private final int baseCannons;
    private final int mastCount;
    private final ShipClass shipClass;
    
    ShipType(String displayName, float baseMass, float baseHealth, 
             float baseManeuverability, float baseStability,
             int baseCargoCapacity, int baseCrewCapacity, 
             int baseCannons, int mastCount, ShipClass shipClass) {
        this.displayName = displayName;
        this.baseMass = baseMass;
        this.baseHealth = baseHealth;
        this.baseManeuverability = baseManeuverability;
        this.baseStability = baseStability;
        this.baseCargoCapacity = baseCargoCapacity;
        this.baseCrewCapacity = baseCrewCapacity;
        this.baseCannons = baseCannons;
        this.mastCount = mastCount;
        this.shipClass = shipClass;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getName() { return displayName; } // Alias for compatibility
    public float getBaseMass() { return baseMass; }
    public float getBaseHealth() { return baseHealth; }
    public float getBaseManeuverability() { return baseManeuverability; }
    public float getBaseStability() { return baseStability; }
    public int getBaseCargoCapacity() { return baseCargoCapacity; }
    public int getBaseCrewCapacity() { return baseCrewCapacity; }
    public int getBaseCannons() { return baseCannons; }
    public int getMastCount() { return mastCount; }
    public ShipClass getShipClass() { return shipClass; }
    
    /**
     * Gets the length of the ship in meters
     */
    public float getLength() {
        return baseMass / 100.0f + 10.0f; // Rough estimate based on mass
    }
    
    /**
     * Gets the width (beam) of the ship in meters
     */
    public float getWidth() {
        return getLength() * 0.25f; // Typical ship beam-to-length ratio
    }
    
    /**
     * Gets the height of the ship in meters
     */
    public float getHeight() {
        return getLength() * 0.15f; // Typical ship height-to-length ratio
    }
    
    /**
     * Gets the maximum mass capacity of this ship type
     */
    public float getMaxMass() {
        return baseMass * 1.5f; // Ships can carry 50% more than their base mass
    }
    
    /**
     * Gets the base cost to build this ship type
     */
    public float getBaseCost() {
        return baseMass * 2.0f + baseHealth * 5.0f + baseCannons * 100.0f;
    }
    
    /**
     * Gets the maximum speed potential of this ship type
     */
    public float getMaxSpeed() {
        return baseManeuverability * 15.0f; // Knots
    }
    
    /**
     * Gets the turning radius of this ship type
     */
    public float getTurningRadius() {
        return baseMass / (baseManeuverability * 100.0f);
    }
    
    /**
     * Gets the draft (how deep the ship sits in water)
     */
    public float getDraft() {
        return baseMass / 1000.0f + 1.0f; // Meters
    }
    
    /**
     * Gets the sail area multiplier for this ship type
     */
    public float getSailAreaMultiplier() {
        return mastCount * 0.8f + shipClass.getSailBonus();
    }
    
    /**
     * Gets the cannon deck count
     */
    public int getCannonDecks() {
        if (baseCannons <= 8) return 1;
        if (baseCannons <= 20) return 2;
        return 3;
    }
    
    /**
     * Checks if this ship type can mount a specific component
     */
    public boolean canMountComponent(ComponentType componentType) {
        switch (componentType) {
            case HULL:
                return true; // All ships have hulls
            case SAIL:
                return true; // All ships have sails
            case CANNON:
                return baseCannons > 0;
            case RUDDER:
                return true; // All ships have rudders
            case ANCHOR:
                return true; // All ships can have anchors
            case FIGUREHEAD:
                return shipClass != ShipClass.LIGHT; // Only medium+ ships
            case CROW_NEST:
                return mastCount >= 2; // Need multiple masts
            case CARGO_HOLD:
                return baseCargoCapacity > 0;
            case CREW_QUARTERS:
                return baseCrewCapacity >= 10; // Only larger ships
            case CAPTAIN_CABIN:
                return shipClass == ShipClass.HEAVY || shipClass == ShipClass.MERCHANT;
            default:
                return false;
        }
    }
    
    /**
     * Gets the build cost for this ship type
     */
    public int getBuildCost() {
        return (int) (baseMass * 2.0f + baseHealth * 5.0f + baseCannons * 100.0f);
    }
    
    /**
     * Gets the maintenance cost per day
     */
    public int getMaintenanceCost() {
        return getBuildCost() / 100;
    }
    
    /**
     * Ship class categories
     */
    public enum ShipClass {
        LIGHT("Light", 0.2f, 1.2f),
        MEDIUM("Medium", 0.0f, 1.0f),
        HEAVY("Heavy", -0.2f, 0.8f),
        MERCHANT("Merchant", -0.1f, 0.9f),
        PIRATE("Pirate", 0.1f, 1.1f),
        SUPERNATURAL("Supernatural", 0.3f, 1.3f);
        
        private final String displayName;
        private final float sailBonus;
        private final float speedMultiplier;
        
        ShipClass(String displayName, float sailBonus, float speedMultiplier) {
            this.displayName = displayName;
            this.sailBonus = sailBonus;
            this.speedMultiplier = speedMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public float getSailBonus() { return sailBonus; }
        public float getSpeedMultiplier() { return speedMultiplier; }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}