package com.odyssey.ship;

/**
 * Enumeration of all ship component types.
 * Each type has specific properties and behaviors.
 */
public enum ComponentType {
    
    // Hull Components
    HULL("Hull", ComponentCategory.STRUCTURE, true, 1000.0f),
    ARMOR_PLATING("Armor Plating", ComponentCategory.STRUCTURE, false, 200.0f),
    REINFORCEMENT("Reinforcement", ComponentCategory.STRUCTURE, false, 150.0f),
    
    // Propulsion Components
    SAIL("Sail", ComponentCategory.PROPULSION, false, 50.0f),
    MAST("Mast", ComponentCategory.PROPULSION, true, 100.0f),
    RUDDER("Rudder", ComponentCategory.PROPULSION, true, 75.0f),
    OARS("Oars", ComponentCategory.PROPULSION, false, 25.0f),
    ENGINE("Engine", ComponentCategory.PROPULSION, false, 200.0f),
    
    // Weapon Components
    CANNON("Cannon", ComponentCategory.WEAPON, false, 300.0f),
    SWIVEL_GUN("Swivel Gun", ComponentCategory.WEAPON, false, 100.0f),
    BALLISTA("Ballista", ComponentCategory.WEAPON, false, 150.0f),
    CATAPULT("Catapult", ComponentCategory.WEAPON, false, 250.0f),
    RAM("Ram", ComponentCategory.WEAPON, false, 200.0f),
    
    // Utility Components
    ANCHOR("Anchor", ComponentCategory.UTILITY, true, 80.0f),
    CARGO_HOLD("Cargo Hold", ComponentCategory.UTILITY, false, 120.0f),
    CREW_QUARTERS("Crew Quarters", ComponentCategory.UTILITY, false, 100.0f),
    GALLEY("Galley", ComponentCategory.UTILITY, false, 60.0f),
    WORKSHOP("Workshop", ComponentCategory.UTILITY, false, 90.0f),
    
    // Navigation Components
    COMPASS("Compass", ComponentCategory.NAVIGATION, false, 10.0f),
    SPYGLASS("Spyglass", ComponentCategory.NAVIGATION, false, 5.0f),
    CHARTS("Charts", ComponentCategory.NAVIGATION, false, 2.0f),
    SEXTANT("Sextant", ComponentCategory.NAVIGATION, false, 8.0f),
    
    // Special Components
    FIGUREHEAD("Figurehead", ComponentCategory.SPECIAL, false, 30.0f),
    FLAG("Flag", ComponentCategory.SPECIAL, false, 5.0f),
    LANTERN("Lantern", ComponentCategory.SPECIAL, false, 15.0f),
    BELL("Bell", ComponentCategory.SPECIAL, false, 20.0f),
    CROW_NEST("Crow's Nest", ComponentCategory.SPECIAL, false, 40.0f),
    CAPTAIN_CABIN("Captain's Cabin", ComponentCategory.UTILITY, false, 80.0f);
    
    private final String displayName;
    private final ComponentCategory category;
    private final boolean isRequired;
    private final float baseMass;
    
    ComponentType(String displayName, ComponentCategory category, boolean isRequired, float baseMass) {
        this.displayName = displayName;
        this.category = category;
        this.isRequired = isRequired;
        this.baseMass = baseMass;
    }
    
    /**
     * Gets the display name for UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the name for compatibility with ShipBuilder
     */
    public String getName() {
        return displayName;
    }
    
    /**
     * Gets the component category
     */
    public ComponentCategory getCategory() {
        return category;
    }
    
    /**
     * Checks if this component is required for ship functionality
     */
    public boolean isRequired() {
        return isRequired;
    }
    
    /**
     * Gets the base mass of this component type
     */
    public float getBaseMass() {
        return baseMass;
    }
    
    /**
     * Gets the base health for this component type
     */
    public float getBaseHealth() {
        switch (this) {
            case HULL:
                return 1000.0f;
            case MAST:
                return 300.0f;
            case CANNON:
                return 200.0f;
            case SAIL:
                return 100.0f;
            case RUDDER:
                return 150.0f;
            case ANCHOR:
                return 200.0f;
            case ARMOR_PLATING:
                return 400.0f;
            case REINFORCEMENT:
                return 300.0f;
            case SWIVEL_GUN:
                return 80.0f;
            case BALLISTA:
                return 120.0f;
            case CATAPULT:
                return 180.0f;
            case RAM:
                return 250.0f;
            case CARGO_HOLD:
                return 150.0f;
            case CREW_QUARTERS:
                return 120.0f;
            case GALLEY:
                return 80.0f;
            case WORKSHOP:
                return 100.0f;
            case OARS:
                return 60.0f;
            default:
                return 50.0f;
        }
    }
    
    /**
     * Gets the maximum number of this component type allowed on a ship
     */
    public int getMaxCount() {
        switch (this) {
            case HULL:
            case RUDDER:
            case ANCHOR:
                return 1; // Only one allowed
            case MAST:
                return 4; // Up to 4 masts
            case SAIL:
                return 8; // Multiple sails per mast
            case CANNON:
                return 20; // Many cannons
            case SWIVEL_GUN:
                return 10;
            case CARGO_HOLD:
                return 3;
            case CREW_QUARTERS:
                return 2;
            case ARMOR_PLATING:
                return 10;
            case REINFORCEMENT:
                return 6;
            default:
                return 5; // Default limit
        }
    }
    
    /**
     * Gets the component slot requirement
     */
    public ComponentSlot getRequiredSlot() {
        switch (category) {
            case STRUCTURE:
                return ComponentSlot.HULL;
            case PROPULSION:
                return ComponentSlot.DECK;
            case WEAPON:
                return ComponentSlot.WEAPON_MOUNT;
            case UTILITY:
                return ComponentSlot.INTERIOR;
            case NAVIGATION:
                return ComponentSlot.BRIDGE;
            case SPECIAL:
                return ComponentSlot.DECORATION;
            default:
                return ComponentSlot.DECK;
        }
    }
    
    /**
     * Checks if this component type is compatible with a ship type
     */
    public boolean isCompatibleWith(ShipType shipType) {
        // Basic compatibility rules
        switch (this) {
            case CATAPULT:
                // Only large ships can have catapults
                return shipType.getShipClass() == ShipType.ShipClass.HEAVY;
            case OARS:
                // Only small to medium ships use oars
                return shipType.getShipClass() != ShipType.ShipClass.HEAVY;
            case RAM:
                // Only certain ship types can have rams
                return shipType == ShipType.WARSHIP;
            default:
                return true; // Most components are universally compatible
        }
    }
    
    /**
     * Gets the build cost for this component type
     */
    public int getBuildCost() {
        return (int) (baseMass * 5.0f + getBaseHealth() * 0.5f);
    }
    
    /**
     * Gets the base cost for compatibility with ShipBuilder
     */
    public float getBaseCost() {
        return getBuildCost();
    }
    
    /**
     * Component categories for organization
     */
    public enum ComponentCategory {
        STRUCTURE("Structure"),
        PROPULSION("Propulsion"),
        WEAPON("Weapons"),
        UTILITY("Utility"),
        NAVIGATION("Navigation"),
        SPECIAL("Special");
        
        private final String displayName;
        
        ComponentCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Component slot types for placement
     */
    public enum ComponentSlot {
        HULL("Hull"),
        DECK("Deck"),
        WEAPON_MOUNT("Weapon Mount"),
        INTERIOR("Interior"),
        BRIDGE("Bridge"),
        DECORATION("Decoration");
        
        private final String displayName;
        
        ComponentSlot(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}