package com.odyssey.world;

/**
 * Defines different shapes for islands
 */
public enum IslandShape {
    VOLCANIC("Volcanic", "Steep cone-shaped with central peak"),
    MOUNTAINOUS("Mountainous", "Multiple peaks and ridges"),
    FLAT("Flat", "Gentle slopes and relatively flat terrain"),
    ATOLL("Atoll", "Ring-shaped with central lagoon");
    
    private final String displayName;
    private final String description;
    
    IslandShape(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    /**
     * Gets the slope factor for this shape (higher = steeper)
     */
    public float getSlopeFactor() {
        switch (this) {
            case VOLCANIC:
                return 1.5f;
            case MOUNTAINOUS:
                return 1.2f;
            case FLAT:
                return 0.3f;
            case ATOLL:
                return 0.8f; // Steep outer edges, flat lagoon
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the erosion resistance for this shape
     */
    public float getErosionResistance() {
        switch (this) {
            case VOLCANIC:
                return 0.9f; // Hard volcanic rock
            case MOUNTAINOUS:
                return 0.8f; // Rocky terrain
            case ATOLL:
                return 0.4f; // Coral is softer
            case FLAT:
                return 0.6f; // Moderate resistance
            default:
                return 0.7f;
        }
    }
    
    /**
     * Determines if this shape has a central depression (like lagoons)
     */
    public boolean hasCentralDepression() {
        return this == ATOLL;
    }
    
    /**
     * Gets the coastline complexity factor
     */
    public float getCoastlineComplexity() {
        switch (this) {
            case ATOLL:
                return 0.9f; // Complex reef structures
            case MOUNTAINOUS:
                return 0.8f; // Jagged coastline
            case VOLCANIC:
                return 0.6f; // Relatively smooth cone
            case FLAT:
                return 0.4f; // Simple coastline
            default:
                return 0.6f;
        }
    }
}