package com.odyssey.world.save.dimensions;

/**
 * Defines the different dimension types in the maritime world.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum DimensionType {
    OVERWORLD("overworld", "The main maritime world with oceans, islands, and ships"),
    NETHER("the_nether", "The fiery underwater volcanic realm"),
    END("the_end", "The mysterious deep ocean abyss realm");
    
    private final String id;
    private final String description;
    
    DimensionType(String id, String description) {
        this.id = id;
        this.description = description;
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    
    /**
     * Gets dimension type by ID.
     */
    public static DimensionType fromId(String id) {
        for (DimensionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return OVERWORLD; // Default fallback
    }
}