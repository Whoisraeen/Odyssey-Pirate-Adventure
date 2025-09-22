package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import org.joml.Vector3f;

/**
 * Lava tube feature representing underground channels formed by flowing lava.
 * May contain lava flows, ores, and other volcanic materials.
 */
public class LavaTube extends Feature {
    
    public enum TubeType {
        ACTIVE_FLOW("Active Flow", "Currently flowing with lava", 1.0f),
        RECENT_FLOW("Recent Flow", "Recently active, still hot", 0.7f),
        COOLED_TUBE("Cooled Tube", "No longer active, safe to explore", 0.2f),
        COLLAPSED("Collapsed", "Partially collapsed, dangerous", 0.8f);
        
        private final String displayName;
        private final String description;
        private final float dangerLevel;
        
        TubeType(String displayName, String description, float dangerLevel) {
            this.displayName = displayName;
            this.description = description;
            this.dangerLevel = dangerLevel;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public float getDangerLevel() { return dangerLevel; }
    }
    
    private float length;
    private float width;
    private float height;
    private TubeType tubeType;
    private boolean hasLava;
    private float temperature;
    private float lavaFlowRate;
    private boolean isAccessible;
    private float collapseRisk;
    private Vector3f exitPoint;
    
    public LavaTube(float x, float y, float z, float length, float width, float height) {
        super(FeatureType.LAVA_TUBE, x, y, z);
        this.length = Math.max(10.0f, length);
        this.width = Math.max(2.0f, width);
        this.height = Math.max(1.5f, height);
        this.tubeType = TubeType.COOLED_TUBE;
        this.hasLava = false;
        this.temperature = 20.0f;
        this.lavaFlowRate = 0.0f;
        this.isAccessible = true;
        this.collapseRisk = 0.1f;
        this.exitPoint = calculateExitPoint();
    }
    
    public LavaTube(Vector3f position, float length, float width, float height) {
        this(position.x, position.y, position.z, length, width, height);
    }
    
    /**
     * Updates the lava tube's state
     */
    public void update(float deltaTime) {
        // Update temperature
        updateTemperature(deltaTime);
        
        // Update lava flow
        updateLavaFlow(deltaTime);
        
        // Update collapse risk
        updateCollapseRisk(deltaTime);
        
        // Update accessibility
        updateAccessibility();
    }
    
    /**
     * Updates the tube's temperature
     */
    private void updateTemperature(float deltaTime) {
        float targetTemperature = getTargetTemperature();
        float coolingRate = 0.05f; // Degrees per second
        
        if (temperature > targetTemperature) {
            temperature = Math.max(targetTemperature, temperature - coolingRate * deltaTime);
        } else if (temperature < targetTemperature) {
            temperature = Math.min(targetTemperature, temperature + coolingRate * deltaTime);
        }
    }
    
    /**
     * Gets the target temperature based on tube type
     */
    private float getTargetTemperature() {
        switch (tubeType) {
            case ACTIVE_FLOW: return 1000.0f;
            case RECENT_FLOW: return 200.0f;
            case COOLED_TUBE: return 20.0f;
            case COLLAPSED: return 50.0f;
            default: return 20.0f;
        }
    }
    
    /**
     * Updates lava flow rate
     */
    private void updateLavaFlow(float deltaTime) {
        if (tubeType == TubeType.ACTIVE_FLOW) {
            // Lava flow varies over time
            lavaFlowRate = 0.5f + (float) Math.sin(System.currentTimeMillis() * 0.001f) * 0.3f;
        } else {
            lavaFlowRate = 0.0f;
        }
    }
    
    /**
     * Updates collapse risk
     */
    private void updateCollapseRisk(float deltaTime) {
        // Collapse risk increases with temperature and time
        float tempRisk = Math.min(0.5f, (temperature - 100.0f) / 200.0f);
        float ageRisk = Math.min(0.3f, (System.currentTimeMillis() - getCreationTime()) / 86400000.0f * 0.01f);
        
        collapseRisk = Math.min(1.0f, tempRisk + ageRisk);
        
        // Random collapse events
        if (collapseRisk > 0.7f && Math.random() < 0.001f * deltaTime) {
            triggerCollapse();
        }
    }
    
    /**
     * Triggers a collapse event
     */
    private void triggerCollapse() {
        tubeType = TubeType.COLLAPSED;
        isAccessible = false;
        hasLava = false;
        lavaFlowRate = 0.0f;
        collapseRisk = 1.0f;
    }
    
    /**
     * Updates accessibility based on current conditions
     */
    private void updateAccessibility() {
        isAccessible = tubeType != TubeType.COLLAPSED && 
                      temperature < 100.0f && 
                      collapseRisk < 0.8f;
    }
    
    /**
     * Calculates the exit point of the lava tube
     */
    private Vector3f calculateExitPoint() {
        // Simple calculation - exit point at the end of the tube
        float angle = (float) Math.random() * (float) Math.PI * 2;
        float exitX = position.x + (float) Math.cos(angle) * length;
        float exitZ = position.z + (float) Math.sin(angle) * length;
        float exitY = position.y - 2.0f; // Slightly lower than entrance
        
        return new Vector3f(exitX, exitY, exitZ);
    }
    
    /**
     * Checks if the tube is safe to explore
     */
    public boolean isSafeToExplore() {
        return isAccessible && temperature < 50.0f && collapseRisk < 0.5f;
    }
    
    /**
     * Gets the exploration difficulty (0.0 = easy, 1.0 = impossible)
     */
    public float getExplorationDifficulty() {
        float baseDifficulty = tubeType.getDangerLevel();
        
        // Add temperature difficulty
        float tempDifficulty = Math.min(0.3f, (temperature - 30.0f) / 100.0f);
        
        // Add collapse difficulty
        float collapseDifficulty = collapseRisk * 0.4f;
        
        // Add size difficulty (smaller tubes are harder to navigate)
        float sizeDifficulty = Math.min(0.2f, (2.0f - Math.min(width, height)) / 2.0f);
        
        return Math.min(1.0f, baseDifficulty + tempDifficulty + collapseDifficulty + sizeDifficulty);
    }
    
    /**
     * Gets the visibility inside the tube
     */
    public float getVisibility() {
        if (hasLava) {
            return 0.8f; // Lava provides some light
        } else if (temperature > 200.0f) {
            return 0.6f; // Hot air glows slightly
        } else {
            return 0.1f; // Very dark
        }
    }
    
    /**
     * Gets the air quality inside the tube
     */
    public float getAirQuality() {
        if (hasLava) {
            return 0.3f; // Toxic gases from lava
        } else if (temperature > 100.0f) {
            return 0.6f; // Hot, but breathable
        } else {
            return 0.9f; // Good air quality
        }
    }
    
    /**
     * Gets the volume of the tube
     */
    public float getVolume() {
        return length * width * height;
    }
    
    /**
     * Gets the surface area of the tube walls
     */
    public float getSurfaceArea() {
        return 2 * (length * width + length * height + width * height);
    }
    
    // Getters and setters
    public float getLength() { return length; }
    public void setLength(float length) { this.length = Math.max(10.0f, length); }
    
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = Math.max(2.0f, width); }
    
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = Math.max(1.5f, height); }
    
    public TubeType getTubeType() { return tubeType; }
    public void setTubeType(TubeType tubeType) { this.tubeType = tubeType; }
    
    public boolean hasLava() { return hasLava; }
    public void setHasLava(boolean hasLava) { this.hasLava = hasLava; }
    
    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }
    
    public float getLavaFlowRate() { return lavaFlowRate; }
    public void setLavaFlowRate(float lavaFlowRate) { this.lavaFlowRate = lavaFlowRate; }
    
    public boolean isAccessible() { return isAccessible; }
    public void setAccessible(boolean accessible) { this.isAccessible = accessible; }
    
    public float getCollapseRisk() { return collapseRisk; }
    public void setCollapseRisk(float collapseRisk) { this.collapseRisk = collapseRisk; }
    
    public Vector3f getExitPoint() { return new Vector3f(exitPoint); }
    public void setExitPoint(Vector3f exitPoint) { this.exitPoint = new Vector3f(exitPoint); }
    
    /**
     * Called when a player or entity interacts with this feature
     */
    @Override
    public void onInteract() {
        if (tubeType == TubeType.ACTIVE_FLOW || tubeType == TubeType.RECENT_FLOW) {
            System.out.println("The lava tube is too hot and dangerous to enter!");
        } else if (tubeType == TubeType.COLLAPSED) {
            System.out.println("The lava tube has collapsed and is unsafe to explore.");
        } else if (isAccessible) {
            System.out.println("You enter the cooled lava tube and explore its depths.");
        } else {
            System.out.println("The lava tube entrance is blocked.");
        }
    }
    
    /**
     * Get the resources that can be extracted from this feature
     */
    @Override
    public String[] getAvailableResources() {
        if (tubeType == TubeType.COOLED_TUBE && isAccessible) {
            return new String[]{"Volcanic Glass", "Basalt", "Rare Minerals"};
        } else if (tubeType == TubeType.COLLAPSED) {
            return new String[]{"Volcanic Rock"};
        } else {
            return new String[]{}; // Too dangerous or inaccessible
        }
    }
    
    /**
     * Extract resources from this feature
     */
    @Override
    public boolean extractResource(String resourceType, int amount) {
        if (!isAccessible || tubeType == TubeType.ACTIVE_FLOW || tubeType == TubeType.RECENT_FLOW) {
            return false; // Too dangerous or inaccessible
        }
        
        String[] availableResources = getAvailableResources();
        for (String resource : availableResources) {
            if (resource.equals(resourceType)) {
                // Simulate resource extraction
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("LavaTube[%s, length=%.1f, temp=%.1f°C, accessible=%s]",
                           tubeType.getDisplayName(), length, temperature, isAccessible);
    }
}
